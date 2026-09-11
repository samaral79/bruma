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
import pt.shrek.bruma.R
import pt.shrek.bruma.core.Definicoes
import pt.shrek.bruma.core.Endereco
import pt.shrek.bruma.core.ModoTor
import pt.shrek.bruma.navegador.GestorSeparadores
import pt.shrek.bruma.navegador.MotorGecko
import pt.shrek.bruma.navegador.Separador
import pt.shrek.bruma.tor.EstadoTor
import pt.shrek.bruma.tor.MotorTor

/** Qual folha está aberta. Só uma de cada vez. */
enum class Folha { NENHUMA, SEPARADORES, ENDERECO, MAIS }

class NavegadorViewModel(aplicacao: Application) : AndroidViewModel(aplicacao) {

    private companion object {
        const val ACAO_PROCURAR = "pt.shrek.bruma.PROCURAR"
        const val ACAO_PROCURAR_COM_TOR = "pt.shrek.bruma.PROCURAR_COM_TOR"
    }

    // A instância vem da Application: ver a nota em BrumaApp.definicoes sobre
    // porque não pode haver duas.
    val definicoes = (aplicacao as pt.shrek.bruma.BrumaApp).definicoes

    private val _avisos = MutableSharedFlow<String>(
        replay = 0, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val avisos: SharedFlow<String> = _avisos

    val separadores = GestorSeparadores(aplicacao, MotorGecko.runtime) { aviso -> avisar(aviso) }

    val estadoTor = MotorTor.estado
    val estadoUblock = MotorGecko.ublock

    var folha by mutableStateOf(Folha.NENHUMA)

    /** A coluna de ações. Vive à parte das folhas: sobrepõe-se à página. */
    var lequeAberto by mutableStateOf(false)

    /** As definições são um ecrã inteiro, não uma folha. */
    var ecraDefinicoes by mutableStateOf(false)

    var capsulaVisivel by mutableStateOf(true)

    /** Se a rede já está apontada a um porto fechado à espera do tor. */
    private var redeSuspensa = false

    /** Endereço que ficou à espera de haver tor. */
    private var destinoPendente: String? = null

    val ativo: Separador? get() = separadores.ativo

    init {
        separadores.novo()
        observarTor()
        acertarEstadoDoTorNoArranque()
    }

    /**
     * Põe o proxy do Gecko de acordo com a realidade, logo ao arrancar.
     *
     * O Gecko **grava as preferências de proxy no perfil**, e elas sobrevivem ao
     * fecho da app. Se a app for morta com o tor ligado, no arranque seguinte o
     * perfil ainda aponta para o porto SOCKS da sessão anterior — um porto que
     * já não existe. O resultado era um navegador que não abria nada e um ecrã
     * preto, com um `ERROR_PROXY_CONNECTION_REFUSED` que só aparecia no registo.
     *
     * Por isso nunca se herda o que está no perfil: ou se liga o tor outra vez,
     * ou se limpa o proxy de propósito.
     */
    private fun acertarEstadoDoTorNoArranque() {
        if (definicoes.torAoArrancar || definicoes.modoTor == ModoTor.LIGADO) {
            ligarTor()
        } else {
            MotorGecko.aplicarProxy(null)
        }
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
                            avisar(texto(R.string.aviso_tor_pronto))
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
                        avisar(texto(R.string.aviso_tor_falhou, estado.causa))
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

        // Enquanto o tor arranca, a rede está apontada a um porto fechado de
        // propósito, para nada sair em direto. Um pedido feito nesse intervalo
        // falhava para sempre com "o proxy recusou a ligação" — e quem estava a
        // ver só via uma página em branco. Fica em espera e abre-se sozinho.
        //
        // Vale para qualquer endereço, não só para os `.onion`: abrir a app por
        // um link de outra app com o tor a arrancar caía exatamente no mesmo.
        val precisaDeTor = definicoes.modoTor == ModoTor.LIGADO || Endereco.ehOnion(destino)
        if (precisaDeTor && !torPronto) {
            destinoPendente = destino
            // A folha fecha-se já: deixá-la aberta escondia a página a carregar
            // por trás dela e dava a impressão de que nada tinha acontecido.
            folha = Folha.NENHUMA
            lequeAberto = false
            if (Endereco.ehOnion(destino)) {
                avisar(texto(R.string.aviso_tor_para_onion))
                ligarTor()
            } else {
                avisar(texto(R.string.aviso_a_espera_tor))
                if (definicoes.modoTor != ModoTor.LIGADO) ligarTor()
            }
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
        avisar(texto(R.string.aviso_nova_identidade))
        recarregar()
    }

    // --- privacidade --------------------------------------------------------

    fun fecharTudo() {
        folha = Folha.NENHUMA
        lequeAberto = false
    }

    /**
     * Reenvia ao Gecko tudo o que ele precisa de saber, a cada mudança.
     *
     * É barato e evita a classe de erros em que se acrescenta uma definição, se
     * esquece de a ligar ao motor, e ela fica gravada sem nunca fazer nada — que
     * foi exatamente o que aconteceu com metade delas na primeira versão.
     */
    fun aplicarDefinicoesAoMotor() {
        MotorGecko.definirJavascript(definicoes.javascript)
        MotorGecko.definirApenasHttps(definicoes.apenasHttps)
        MotorGecko.definirIsolamentoDeCookies(definicoes.isolarCookies)
        MotorGecko.aplicarResistenciaAImpressaoDigital(definicoes.resistirImpressaoDigital)
    }

    /**
     * Trata o que chega de fora: um link de outra app, ou um toque no widget.
     *
     * O `ACTION_VIEW` estava declarado no manifesto desde o princípio — a app
     * oferecia-se para ser o navegador do sistema — mas ninguém lia o intent.
     * Quem a escolhesse via a app abrir no ecrã inicial em vez da página que
     * tinha tocado.
     */
    fun tratarIntencao(acao: String?, url: String?) {
        when {
            !url.isNullOrBlank() -> {
                // Um link de fora abre em separador novo — substituir a página
                // onde alguém estava é uma forma barata de lhe perder o trabalho
                // — exceto se o separador atual estiver vazio, caso em que abrir
                // outro só deixava um separador em branco para trás.
                if (ativo?.url?.isNotBlank() == true) separadores.novo()
                abrir(url)
            }
            acao == ACAO_PROCURAR_COM_TOR -> {
                if (definicoes.modoTor != ModoTor.LIGADO) alternarTor()
                folha = Folha.ENDERECO
            }
            acao == ACAO_PROCURAR -> folha = Folha.ENDERECO
        }
    }

    fun abrirDefinicoes() {
        folha = Folha.NENHUMA
        lequeAberto = false
        ecraDefinicoes = true
    }

    fun limparTudo() {
        separadores.fecharTodos()
        MotorGecko.runtime.storageController.clearData(
            org.mozilla.geckoview.StorageController.ClearFlags.ALL
        )
        avisar(texto(R.string.aviso_tudo_apagado))
        folha = Folha.NENHUMA
    }

    /** Atalho para ir buscar um texto traduzido sem repetir o contexto. */
    private fun texto(id: Int, vararg argumentos: Any): String =
        getApplication<Application>().getString(id, *argumentos)

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
