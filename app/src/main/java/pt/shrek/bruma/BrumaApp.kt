package pt.shrek.bruma

import android.app.Application
import android.os.Build
import java.io.File
import pt.shrek.bruma.core.Definicoes
import pt.shrek.bruma.navegador.MotorGecko

/**
 * O Gecko arranca aqui e não na atividade: o [org.mozilla.geckoview.GeckoRuntime]
 * é um por processo e tem de existir antes de qualquer sessão.
 *
 * **E arranca só no processo principal.** O `onCreate` de uma Application corre
 * em *todos* os processos da app, e o GeckoView cria vários — `:gpu`, `:tab`,
 * `:crashhelper`, um por separador isolado. Sem esta guarda, cada processo-filho
 * do Gecko voltava a chamar `GeckoRuntime.create()` e tentava ser um motor
 * completo; chegava a `JNI_READY`, abortava, e arrastava o processo principal
 * com um SIGKILL cerca de seis segundos depois do arranque.
 *
 * O sintoma não ajudava nada: sem exceção em Java, sem minidump, sem `am_kill`
 * do ActivityManager, e `ApplicationExitInfo` a dizer apenas `SIGNALED status=9`.
 * O que denunciou foi o `am_proc_died` aparecer sem nenhum `am_kill` antes — ou
 * seja, ninguém no sistema o tinha mandado morrer.
 */
class BrumaApp : Application() {

    /**
     * A única instância de [Definicoes] da app.
     *
     * Cada instância tem o seu próprio estado do Compose por trás. Duas
     * instâncias são dois conjuntos de valores que não se falam: uma definição
     * mudada num sítio fica gravada no disco mas nunca é vista pelo outro até a
     * app reiniciar. Ter uma só, à mão de quem precisa, elimina essa classe
     * inteira de erros em vez de os apanhar um a um.
     */
    val definicoes: Definicoes by lazy { Definicoes(this) }

    override fun onCreate() {
        super.onCreate()
        if (!ehProcessoPrincipal()) return
        MotorGecko.iniciar(this, definicoes)
    }

    private fun ehProcessoPrincipal(): Boolean = nomeDoProcesso() == packageName

    /**
     * O nome deste processo.
     *
     * `Application.getProcessName()` só existe a partir do Android 9, e a app
     * suporta a partir do 8.0 — que é o chão imposto pelo GeckoView. Na primeira
     * versão disto o ramo alternativo devolvia `null`, e o efeito era grave e
     * invisível: no 8.0 e no 8.1 a guarda dizia "não sou o processo principal",
     * o Gecko nunca arrancava, e a app rebentava ao abrir. Não dava para
     * descobrir sem um telemóvel dessas versões.
     *
     * Abaixo do 9 lê-se `/proc/self/cmdline`, que o Linux preenche com o nome do
     * processo. É o mesmo sítio de onde o próprio Android o tira, não precisa de
     * permissões, e funciona em qualquer versão.
     */
    private fun nomeDoProcesso(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return getProcessName()
        return runCatching {
            File("/proc/self/cmdline").readText()
                // O ficheiro vem terminado por bytes nulos; sem os cortar, a
                // comparação com o nome do pacote nunca dava igual.
                .substringBefore('\u0000')
                .trim()
                .ifBlank { null }
        }.getOrNull()
    }
}

