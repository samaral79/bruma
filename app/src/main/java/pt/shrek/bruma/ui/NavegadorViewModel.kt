package pt.shrek.bruma.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import pt.shrek.bruma.core.Definicoes
import pt.shrek.bruma.core.Endereco
import pt.shrek.bruma.core.ModoTor
import pt.shrek.bruma.navegador.GestorSeparadores
import pt.shrek.bruma.navegador.MotorGecko
import pt.shrek.bruma.navegador.Separador
import pt.shrek.bruma.tor.EstadoTor
import pt.shrek.bruma.tor.MotorTor

/** Qual folha está aberta. Só uma de cada vez. */
enum class Folha { NENHUMA, SEPARADORES, DEFINICOES, ENDERECO, MAIS }

class NavegadorViewModel(aplicacao: Application) : AndroidViewModel(aplicacao) {

    // A instância vem da Application: ver a nota em BrumaApp.definicoes sobre
    // porque não pode haver duas.
    val definicoes = (aplicacao as pt.shrek.bruma.BrumaApp).definicoes

    private val _avisos = MutableSharedFlow<String>(
        replay = 0, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val avisos: SharedFlow<String> = _avisos

    val separadores = GestorSeparadores(MotorGecko.runtime) { aviso -> avisar(aviso) }

    val estadoTor = MotorTor.estado
    val estadoUblock = MotorGecko.ublock

    var folha by mutableStateOf(Folha.NENHUMA)

    /** O leque de ações em arco. Vive à parte das folhas: sobrepõe-se à página. */
    var lequeAberto by mutableStateOf(false)

    var capsulaVisivel by mutableStateOf(true)

    /** Se a rede já está apontada a um porto fechado à espera do tor. */
    private var redeSuspensa = false

    /** Endereço que ficou à espera de haver tor. */
    private var destinoPendente: String? = null

    val ativo: Separador? get() = separadores.ativo

    init {
        separadores.novo()
        observarTor()
        if (definicoes.torAoArrancar) ligarTor()
    }

    /**
     * O porto SOCKS do tor muda a cada arranque (é pedido em `auto`), por isso
     * o proxy do Gecko tem de ser reescrito sempre que o tor fica pronto — não
     * basta configurá-lo uma vez.
     */
    private fun observarTor() {
        viewModelScope.launch {
            MotorTor.estado.collect { estado ->
                when (estado) {
                    is EstadoTor.Pronto -> {
                        redeSuspensa = false
                        MotorGecko.aplicarProxy(estado.portoSocks)
                        val pendente = destinoPendente
                        if (pendente != null) {
                            destinoPendente = null
                            ativo?.abrir(pendente)
                        } else {
                            avisar("Tor pronto")
                        }
                    }
                    is EstadoTor.ALigar ->
                        // Só na transição: o arranque do tor emite este estado a
                        // cada ponto percentual, e suspender a rede cem vezes
                        // seguidas só enchia o registo.
                        if (!redeSuspensa) {
                            redeSuspensa = true
                            MotorGecko.suspenderRedeAteTor()
                        }
                    is EstadoTor.Falhou -> {
                        MotorGecko.suspenderRedeAteTor()
                        avisar("Tor falhou: ${estado.causa}")
                    }
                    EstadoTor.Desligado ->
                        if (definicoes.modoTor == ModoTor.DESLIGADO) MotorGecko.aplicarProxy(null)
                }
            }
        }
    }

    // --- navegação ----------------------------------------------------------

    fun abrir(entrada: String) {
        val torPronto = MotorTor.estado.value is EstadoTor.Pronto
        val destino = Endereco.normalizar(entrada, definicoes.motorEfetivo(torPronto))
        android.util.Log.i("Bruma.Abrir", "entrada=[$entrada] destino=[$destino]")
        if (destino.isBlank()) return

        if (Endereco.ehOnion(destino) && !torPronto) {
            // Ligar o tor e ficar por aqui obrigava a escrever o endereço outra
            // vez quando o circuito ficasse pronto — e um endereço onion tem 56
            // caracteres que ninguém reescreve de boa vontade. Fica guardado e
            // abre-se sozinho assim que houver circuito.
            destinoPendente = destino
            // A folha fecha-se já: deixá-la aberta escondia a página a carregar
            // por trás dela e dava a impressão de que nada tinha acontecido.
            folha = Folha.NENHUMA
            lequeAberto = false
            avisar("A ligar o tor para abrir este .onion…")
            ligarTor()
            return
        }
        ativo?.abrir(destino)
        folha = Folha.NENHUMA
        lequeAberto = false
    }

    fun novoSeparador(privado: Boolean = false) {
        separadores.novo(privado = privado)
        folha = Folha.ENDERECO
    }

    fun recarregar() = ativo?.recarregar()

    /**
     * Volta ao ecrã inicial sem fechar o separador.
     *
     * Carregar `about:blank` no motor é o que faz o separador deixar de ter
     * página — e é assim que a app decide mostrar o início em vez do Gecko. O
     * histórico do separador é limpo junto, senão o botão de voltar levava de
     * novo à página de que se acabou de sair.
     */
    fun irParaInicio() {
        val separador = ativo ?: return
        separador.limparParaInicio()
        fecharTudo()
    }
    fun voltar(): Boolean {
        val separador = ativo ?: return false
        if (!separador.podeVoltar) return false
        separador.voltar()
        return true
    }

    // --- tor ----------------------------------------------------------------

    fun alternarTor() {
        if (definicoes.modoTor == ModoTor.LIGADO) desligarTor() else ligarTor()
    }

    private fun ligarTor() {
        definicoes.modoTor = ModoTor.LIGADO
        // Suspender primeiro, ligar depois: a ordem importa, senão há pedidos a
        // sair em direto enquanto o circuito não está feito.
        MotorGecko.suspenderRedeAteTor()
        MotorTor.ligar()
    }

    private fun desligarTor() {
        destinoPendente = null
        definicoes.modoTor = ModoTor.DESLIGADO
        MotorTor.desligar()
        MotorGecko.aplicarProxy(null)
    }

    fun novaIdentidade() {
        MotorTor.novaIdentidade()
        avisar("Circuitos novos pedidos")
        recarregar()
    }

    // --- privacidade --------------------------------------------------------

    fun fecharTudo() {
        folha = Folha.NENHUMA
        lequeAberto = false
    }

    fun limparTudo() {
        separadores.fecharTodos()
        MotorGecko.runtime.storageController.clearData(
            org.mozilla.geckoview.StorageController.ClearFlags.ALL
        )
        avisar("Tudo apagado: cookies, cache e sessões")
        folha = Folha.NENHUMA
    }

    fun avisar(texto: String) {
        viewModelScope.launch { _avisos.emit(texto) }
    }

    override fun onCleared() {
        if (definicoes.limparAoSair) {
            MotorGecko.runtime.storageController.clearData(
                org.mozilla.geckoview.StorageController.ClearFlags.ALL
            )
        }
        separadores.separadores.forEach { it.destruir() }
        super.onCleared()
    }
}
