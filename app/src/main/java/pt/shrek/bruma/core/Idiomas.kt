package pt.shrek.bruma.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Os idiomas que a app fala, e como se troca entre eles.
 *
 * [etiqueta] está escrita **no próprio idioma** de propósito: quem abre a app
 * pela primeira vez e não percebe a língua que o telemóvel escolheu precisa de
 * reconhecer a sua na lista. "Português" só ajuda quem já lê português.
 */
enum class Idioma(val codigo: String, val etiqueta: String, val nativo: String) {
    INGLES("en", "English", "English"),
    PORTUGUES("pt", "Português", "Portuguese"),
    ESPANHOL("es", "Español", "Spanish"),
    MANDARIM("zh", "中文", "Chinese (Simplified)");

    companion object {
        fun porCodigo(codigo: String?): Idioma? =
            entries.firstOrNull { it.codigo == codigo }
    }
}

/**
 * A escolha de idioma, aplicada ao contexto da atividade.
 *
 * Fica num [android.content.SharedPreferences] simples, e não nas [Definicoes]
 * cifradas, por uma razão prática: é lido em `attachBaseContext`, antes de a
 * atividade existir. Abrir o Keystore nesse ponto atrasaria todos os arranques
 * para guardar um dado que não é segredo nenhum — saber que alguém lê espanhol
 * não revela nada sobre o que faz na app.
 */
object Idiomas {

    private const val FICHEIRO = "bruma_idioma"
    private const val CHAVE = "codigo"

    fun escolhido(context: Context): Idioma? =
        Idioma.porCodigo(
            context.getSharedPreferences(FICHEIRO, Context.MODE_PRIVATE).getString(CHAVE, null)
        )

    fun jaEscolheu(context: Context): Boolean = escolhido(context) != null

    fun guardar(context: Context, idioma: Idioma) {
        context.getSharedPreferences(FICHEIRO, Context.MODE_PRIVATE)
            .edit().putString(CHAVE, idioma.codigo).apply()
    }

    /** O idioma do sistema, se a app o falar — serve de sugestão inicial. */
    fun sugestaoDoSistema(context: Context): Idioma {
        val doSistema = context.resources.configuration.locales[0]?.language
        return Idioma.porCodigo(doSistema) ?: Idioma.INGLES
    }

    /**
     * Devolve um contexto com o idioma escolhido aplicado.
     *
     * Faz-se assim, e não com `AppCompatDelegate.setApplicationLocales`, para não
     * arrastar a appcompat inteira — que esta app não usa em mais nada — só por
     * causa de uma lista de quatro idiomas. E funciona igual em todas as versões
     * do Android, em vez de precisar do mecanismo novo em 13+ e de um recurso
     * alternativo abaixo disso.
     */
    fun aplicar(base: Context): Context {
        val idioma = escolhido(base) ?: return base
        val local = Locale.forLanguageTag(idioma.codigo)
        Locale.setDefault(local)
        val configuracao = Configuration(base.resources.configuration).apply {
            setLocale(local)
            setLayoutDirection(local)
        }
        return base.createConfigurationContext(configuracao)
    }
}
