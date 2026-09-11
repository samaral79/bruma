package pt.shrek.bruma.core

import android.net.Uri

/**
 * Os motores de busca disponíveis. O DuckDuckGo é o que vem escolhido, e tem
 * duas moradas: a normal e o serviço onion oficial.
 */
enum class MotorBusca(
    val id: String,
    val etiqueta: String,
    val modelo: String,
    /** true se a morada for um serviço onion e portanto exigir o tor ligado. */
    val exigeTor: Boolean = false,
) {
    DUCKDUCKGO("ddg", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),

    /**
     * O serviço onion oficial do DuckDuckGo. Confirmado na página do próprio
     * DuckDuckGo sobre o Tor — e não numa lista de terceiros, que é onde
     * costumam circular endereços onion trocados de propósito.
     */
    DUCKDUCKGO_ONION(
        "ddg-onion",
        "DuckDuckGo (.onion)",
        "https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion/?q=%s",
        exigeTor = true,
    ),

    STARTPAGE("startpage", "Startpage", "https://www.startpage.com/sp/search?query=%s"),
    BRAVE("brave", "Brave Search", "https://search.brave.com/search?q=%s"),
    MOJEEK("mojeek", "Mojeek", "https://www.mojeek.com/search?q=%s"),
    WIKIPEDIA("wikipedia", "Wikipédia", "https://pt.wikipedia.org/w/index.php?search=%s");

    fun urlPara(termo: String): String = modelo.format(Uri.encode(termo))

    companion object {
        fun porId(id: String): MotorBusca = entries.firstOrNull { it.id == id } ?: DUCKDUCKGO
    }
}

/**
 * Decide se o que foi escrito na barra é uma morada ou um termo de busca.
 *
 * A dúvida importante é `algumacoisa.com` contra `algumacoisa com`: a primeira
 * é uma morada, a segunda uma busca. A regra é exigir um ponto sem espaços e um
 * sufixo com ar de domínio.
 */
object Endereco {

    private val ESQUEMAS_CONHECIDOS = listOf(
        "http://", "https://", "file://", "about:", "data:", "javascript:", "content://",
    )

    private val PADRAO_ANFITRIAO = Regex(
        """^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$""",
        RegexOption.IGNORE_CASE,
    )

    fun normalizar(entrada: String, motor: MotorBusca): String {
        val texto = entrada.trim()
        if (texto.isEmpty()) return ""

        if (ESQUEMAS_CONHECIDOS.any { texto.startsWith(it, ignoreCase = true) }) return texto

        // `localhost` e IPs literais não têm ponto nem sufixo, mas são moradas.
        if (texto == "localhost" || texto.startsWith("localhost:")) return "http://$texto"

        val semCaminho = texto.substringBefore('/').substringBefore('?')
        val semPorto = semCaminho.substringBefore(':')

        if (' ' !in texto && PADRAO_ANFITRIAO.matches(semPorto)) {
            // Os serviços onion quase nunca têm certificado; forçar https faria
            // falhar a ligação em vez de a proteger. O onion já é autenticado e
            // cifrado pelo próprio endereço, que é a chave pública do serviço.
            return if (semPorto.endsWith(".onion")) "http://$texto" else "https://$texto"
        }

        return motor.urlPara(texto)
    }

    fun ehOnion(url: String): Boolean =
        runCatching { Uri.parse(url).host?.endsWith(".onion") == true }.getOrDefault(false)

    /** O que mostrar na barra: o anfitrião, sem `www.` nem ruído. */
    fun paraMostrar(url: String): String {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
        val anfitriao = uri.host ?: return url
        return anfitriao.removePrefix("www.")
    }

    /** Um onion tem 56 caracteres base32 e é ilegível; encurta-se para caber. */
    fun encurtarOnion(anfitriao: String): String {
        if (!anfitriao.endsWith(".onion")) return anfitriao
        val corpo = anfitriao.removeSuffix(".onion")
        if (corpo.length <= 16) return anfitriao
        return "${corpo.take(8)}…${corpo.takeLast(4)}.onion"
    }
}
