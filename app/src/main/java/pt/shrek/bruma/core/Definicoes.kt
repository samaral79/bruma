package pt.shrek.bruma.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** De que lado fica a mão que segura o telemóvel. */
enum class Mao { DIREITA, ESQUERDA }

/**
 * Se o tráfego vai pelo tor.
 *
 * É deliberadamente global, e não por separador: o proxy do Gecko aplica-se ao
 * processo inteiro. Oferecer um "separador tor" ao lado de separadores diretos
 * daria a impressão de isolamento sem o haver — os separadores em segundo plano
 * continuariam a carregar pelo caminho errado.
 */
enum class ModoTor { DESLIGADO, LIGADO }

/**
 * As definições da app.
 *
 * **Cada valor é estado do Compose**, não apenas uma leitura das preferências.
 * A primeira versão desta classe lia direto do `SharedPreferences`, e o efeito
 * era subtil e mau: mudar a mão dominante ou o ecrã seguro gravava o valor mas
 * não mexia na interface, porque nada tinha sido notificado. A definição parecia
 * partida quando o que estava partido era a ligação entre ela e o ecrã. Com o
 * valor em `mutableStateOf`, quem o lê num composable volta a desenhar sozinho.
 *
 * Ficam em [EncryptedSharedPreferences] porque o conjunto delas é revelador:
 * saber que alguém tem o tor sempre ligado e o motor onion escolhido é
 * informação que não interessa deixar em texto simples no armazenamento.
 */
class Definicoes(context: Context) {

    // A androidx.security-crypto foi descontinuada pela Google sem substituto
    // direto. Mantém-se: continua a funcionar e a alternativa seria texto
    // simples. Se for removida, a migração é trocar este bloco por
    // SharedPreferences normais — nada mais nesta classe depende da cifra.
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

    /**
     * Uma definição booleana: guardada no disco e observável pelo Compose.
     * O estado é a fonte de verdade em memória; o disco é só a persistência.
     */
    private inner class Booleana(private val chave: String, omissao: Boolean) {
        private var estado by mutableStateOf(prefs.getBoolean(chave, omissao))
        operator fun getValue(alvo: Any?, propriedade: Any?): Boolean = estado
        operator fun setValue(alvo: Any?, propriedade: Any?, valor: Boolean) {
            estado = valor
            prefs.edit().putBoolean(chave, valor).apply()
        }
    }

    private inner class Texto(private val chave: String, omissao: String) {
        private var estado by mutableStateOf(prefs.getString(chave, omissao) ?: omissao)
        operator fun getValue(alvo: Any?, propriedade: Any?): String = estado
        operator fun setValue(alvo: Any?, propriedade: Any?, valor: String) {
            estado = valor.trim()
            prefs.edit().putString(chave, estado).apply()
        }
    }

    // --- busca ------------------------------------------------------------
    private var motorBuscaId: String by Texto("motor_busca", MotorBusca.DUCKDUCKGO.id)

    var motorBusca: MotorBusca
        get() = MotorBusca.porId(motorBuscaId)
        set(v) { motorBuscaId = v.id }

    /**
     * Com o tor ligado, trocar automaticamente para o DuckDuckGo onion: o pedido
     * nunca deixa a rede tor, por isso nenhum nó de saída vê os termos
     * procurados.
     */
    var preferirOnionComTor: Boolean by Booleana("preferir_onion", true)

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
    private var modoTorNome: String by Texto("modo_tor", ModoTor.DESLIGADO.name)

    var modoTor: ModoTor
        get() = runCatching { ModoTor.valueOf(modoTorNome) }.getOrDefault(ModoTor.DESLIGADO)
        set(v) { modoTorNome = v.name }

    /** Arrancar já com o tor ligado, sem o ligar à mão em cada sessão. */
    var torAoArrancar: Boolean by Booleana("tor_ao_arrancar", false)

    // --- privacidade ------------------------------------------------------
    var javascript: Boolean by Booleana("javascript", true)

    var apenasHttps: Boolean by Booleana("apenas_https", true)

    /**
     * Cada sítio com o seu frasco de cookies. Desligar isto não deixa os
     * rastreadores à solta — a proteção reforçada do Gecko continua a agir — mas
     * volta a permitir que um cookie de terceiros siga entre sítios.
     */
    var isolarCookies: Boolean by Booleana("isolar_cookies", true)

    /**
     * Ver [pt.shrek.bruma.navegador.MotorGecko.aplicarResistenciaAImpressaoDigital]:
     * protege muito e parte sítios, por isso é escolha e não omissão.
     */
    var resistirImpressaoDigital: Boolean by Booleana("resistir_impressao", false)

    var limparAoSair: Boolean by Booleana("limpar_ao_sair", false)

    /** FLAG_SECURE: tira a app das capturas de ecrã e do histórico de tarefas. */
    var ecraSeguro: Boolean by Booleana("ecra_seguro", false)

    /** Pedir impressão digital ou rosto para abrir a app. */
    var pedirBiometria: Boolean by Booleana("pedir_biometria", false)

    /**
     * Se o guia de boas-vindas já foi visto.
     *
     * Guardado nas definições e não num ficheiro à parte para seguir o mesmo
     * caminho de todo o resto — e para desaparecer junto com tudo quando alguém
     * limpa os dados da app, que é o que essa pessoa está a pedir.
     */
    var guiaVisto: Boolean by Booleana("guia_visto", false)

    // --- uma mão ----------------------------------------------------------
    private var maoNome: String by Texto("mao", Mao.DIREITA.name)

    var mao: Mao
        get() = runCatching { Mao.valueOf(maoNome) }.getOrDefault(Mao.DIREITA)
        set(v) { maoNome = v.name }
}
