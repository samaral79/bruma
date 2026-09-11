package pt.shrek.bruma.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** De que lado fica a mão que segura o telemóvel. */
enum class Mao { DIREITA, ESQUERDA }

/**
 * Se o tráfego vai pelo tor.
 *
 * É deliberadamente global, e não por separador: o `ProxyController` do WebView
 * aplica-se ao processo inteiro, não a uma instância. Oferecer um "separador
 * tor" ao lado de separadores diretos daria a impressão de isolamento sem o
 * haver — os separadores em segundo plano continuariam a carregar pelo caminho
 * errado. Um interruptor global, visível na barra, não mente sobre o que faz.
 */
enum class ModoTor {
    DESLIGADO,
    LIGADO,
}

/**
 * As definições da app.
 *
 * Ficam em [EncryptedSharedPreferences] porque o conjunto delas é, por si só,
 * revelador: saber que alguém tem o tor sempre ligado e o motor onion escolhido
 * é informação que não interessa deixar em texto simples no armazenamento.
 */
class Definicoes(context: Context) {

    // A androidx.security-crypto foi descontinuada pela Google sem substituto
    // direto. Mantém-se na mesma: continua a funcionar e a alternativa seria
    // guardar isto em texto simples. Se a biblioteca for removida, a migração é
    // trocar este bloco por SharedPreferences normais — nada mais nesta classe
    // depende de ser cifrado.
    @Suppress("DEPRECATION")
    private val prefs: SharedPreferences = run {
        val chaveMestra = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "bruma_definicoes",
            chaveMestra,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun texto(chave: String, omissao: String) = prefs.getString(chave, omissao) ?: omissao
    private fun guardar(chave: String, valor: String) = prefs.edit().putString(chave, valor).apply()
    private fun booleano(chave: String, omissao: Boolean) = prefs.getBoolean(chave, omissao)
    private fun guardar(chave: String, valor: Boolean) = prefs.edit().putBoolean(chave, valor).apply()

    // --- busca ------------------------------------------------------------
    var motorBusca: MotorBusca
        get() = MotorBusca.porId(texto("motor_busca", MotorBusca.DUCKDUCKGO.id))
        set(v) = guardar("motor_busca", v.id)

    /**
     * Com o tor ligado, trocar automaticamente para o DuckDuckGo onion. Poupa a
     * busca de sair pelo nó de saída: o pedido nunca deixa a rede tor, por isso
     * nenhum nó de saída vê os termos procurados.
     */
    var preferirOnionComTor: Boolean
        get() = booleano("preferir_onion", true)
        set(v) = guardar("preferir_onion", v)

    /** O motor a usar tendo em conta o estado do tor. */
    fun motorEfetivo(torPronto: Boolean): MotorBusca {
        val escolhido = motorBusca
        if (escolhido.exigeTor && !torPronto) return MotorBusca.DUCKDUCKGO
        if (torPronto && preferirOnionComTor && escolhido == MotorBusca.DUCKDUCKGO) {
            return MotorBusca.DUCKDUCKGO_ONION
        }
        return escolhido
    }

    // --- tor --------------------------------------------------------------
    var modoTor: ModoTor
        get() = runCatching { ModoTor.valueOf(texto("modo_tor", ModoTor.DESLIGADO.name)) }
            .getOrDefault(ModoTor.DESLIGADO)
        set(v) = guardar("modo_tor", v.name)

    /** Arrancar já com o tor ligado, sem ter de o ligar à mão em cada sessão. */
    var torAoArrancar: Boolean
        get() = booleano("tor_ao_arrancar", false)
        set(v) = guardar("tor_ao_arrancar", v)

    // --- bloqueio ---------------------------------------------------------
    var bloquearPedidos: Boolean
        get() = booleano("bloquear_pedidos", true)
        set(v) = guardar("bloquear_pedidos", v)

    var bloquearCosmetico: Boolean
        get() = booleano("bloquear_cosmetico", true)
        set(v) = guardar("bloquear_cosmetico", v)

    /** Sítios onde o bloqueador fica desligado, um por linha. */
    var excecoesBloqueio: Set<String>
        get() = prefs.getStringSet("excecoes_bloqueio", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("excecoes_bloqueio", v).apply()

    fun alternarExcecao(dominio: String) {
        val atuais = excecoesBloqueio.toMutableSet()
        if (!atuais.remove(dominio)) atuais += dominio
        excecoesBloqueio = atuais
    }

    // --- privacidade ------------------------------------------------------
    var javascript: Boolean
        get() = booleano("javascript", true)
        set(v) = guardar("javascript", v)

    var apenasHttps: Boolean
        get() = booleano("apenas_https", true)
        set(v) = guardar("apenas_https", v)

    var bloquearCookiesTerceiros: Boolean
        get() = booleano("cookies_terceiros", true)
        set(v) = guardar("cookies_terceiros", v)

    var limparAoSair: Boolean
        get() = booleano("limpar_ao_sair", false)
        set(v) = guardar("limpar_ao_sair", v)

    /** FLAG_SECURE: tira a app das capturas de ecrã e do histórico de tarefas. */
    var ecraSeguro: Boolean
        get() = booleano("ecra_seguro", false)
        set(v) = guardar("ecra_seguro", v)

    /**
     * Ver [pt.shrek.bruma.navegador.MotorGecko.aplicarResistenciaAImpressaoDigital]:
     * protege muito e parte sítios, por isso é escolha e não omissão.
     */
    var resistirImpressaoDigital: Boolean
        get() = booleano("resistir_impressao", false)
        set(v) = guardar("resistir_impressao", v)

    var pedirBiometria: Boolean
        get() = booleano("pedir_biometria", false)
        set(v) = guardar("pedir_biometria", v)

    // --- uma mão ----------------------------------------------------------
    var mao: Mao
        get() = runCatching { Mao.valueOf(texto("mao", Mao.DIREITA.name)) }.getOrDefault(Mao.DIREITA)
        set(v) = guardar("mao", v.name)

    var paginaInicial: String
        get() = texto("pagina_inicial", "")
        set(v) = guardar("pagina_inicial", v.trim())
}
