package pt.shrek.bruma.navegador

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import org.mozilla.geckoview.GeckoRuntime

/**
 * A lista de separadores e qual está à frente.
 *
 * Arrastar o dedo para o lado na barra troca de separador, como no Sleipnir, e
 * a passagem é circular: chegar ao fim volta ao princípio. Parar a meio de um
 * gesto sem explicação é pior do que dar a volta.
 */
class GestorSeparadores(
    private val runtime: GeckoRuntime,
    private val aoAvisar: (String) -> Unit,
) {

    private var proximoId = 1L

    val separadores = mutableStateListOf<Separador>()

    var indiceAtivo by mutableIntStateOf(0)
        private set

    val ativo: Separador?
        get() = separadores.getOrNull(indiceAtivo)

    fun novo(url: String = "", privado: Boolean = false): Separador {
        val separador = Separador(
            id = proximoId++,
            privado = privado,
            runtime = runtime,
            aoPedirNovoSeparador = { destino -> novo(destino, privado) },
            aoAvisar = aoAvisar,
            aoFechar = { fechar(it) },
        )
        separadores.add(separador)
        trocarPara(separadores.lastIndex)
        if (url.isNotBlank()) separador.abrir(url)
        return separador
    }

    /**
     * O Gecko precisa de saber qual sessão está à vista: só a ativa recebe
     * prioridade de processo e continua a compor. Deixar duas ativas gasta
     * bateria e memória sem nada em troca.
     */
    private fun trocarPara(indice: Int) {
        if (indice !in separadores.indices) return
        separadores.getOrNull(indiceAtivo)?.takeIf { it !== separadores[indice] }?.ativar(false)
        indiceAtivo = indice
        separadores[indice].ativar(true)
    }

    fun irPara(indice: Int) = trocarPara(indice)

    fun seguinte() {
        if (separadores.size > 1) trocarPara((indiceAtivo + 1) % separadores.size)
    }

    fun anterior() {
        if (separadores.size > 1) trocarPara((indiceAtivo - 1 + separadores.size) % separadores.size)
    }

    fun fechar(separador: Separador) {
        val indice = separadores.indexOf(separador)
        if (indice < 0) return
        separador.destruir()
        separadores.removeAt(indice)
        if (separadores.isEmpty()) {
            novo()
        } else {
            trocarPara(indice.coerceAtMost(separadores.lastIndex))
        }
    }

    fun fecharTodos() {
        separadores.forEach { it.destruir() }
        separadores.clear()
        indiceAtivo = 0
        novo()
    }

    val temPrivados: Boolean get() = separadores.any { it.privado }
}
