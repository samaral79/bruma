package pt.shrek.bruma.navegador

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoPreferenceController
import org.mozilla.geckoview.GeckoPreferenceController.SetGeckoPreference
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import pt.shrek.bruma.core.Definicoes

/** Estado da extensão de bloqueio. */
sealed interface EstadoUblock {
    data object PorInstalar : EstadoUblock
    data object AInstalar : EstadoUblock
    data class Instalado(val versao: String) : EstadoUblock
    data class Falhou(val causa: String) : EstadoUblock
}

/**
 * O motor de renderização: Gecko, o mesmo do Firefox e do Tor Browser.
 *
 * A razão de não ser o WebView do sistema é concreta, não de gosto. O WebView
 * não corre extensões, não expõe `resistFingerprinting`, não isola por primeira
 * parte, e o proxy dele é do processo inteiro — não dava para separar
 * verdadeiramente o tráfego. O Gecko traz tudo isso como preferência, e traz
 * ainda a única forma de correr o **uBlock Origin a sério**, em vez de uma
 * imitação que lê as mesmas listas.
 *
 * O preço é o tamanho: ~60 MB de biblioteca nativa por arquitetura.
 */
object MotorGecko {

    private const val TAG = "Bruma.Gecko"
    private const val ID_UBLOCK = "uBlock0@raymondhill.net"
    private const val URI_UBLOCK = "resource://android/assets/extensions/ublock/"

    @Volatile
    private var runtimeInterno: GeckoRuntime? = null

    val runtime: GeckoRuntime
        get() = runtimeInterno ?: error("MotorGecko.iniciar() ainda não foi chamado")

    val pronto: Boolean get() = runtimeInterno != null

    private val _ublock = MutableStateFlow<EstadoUblock>(EstadoUblock.PorInstalar)
    val ublock: StateFlow<EstadoUblock> = _ublock

    fun iniciar(context: Context, definicoes: Definicoes) {
        if (runtimeInterno != null) return

        val bloqueio = ContentBlocking.Settings.Builder()
            // Proteção reforçada no nível estrito: é o mesmo que o Firefox usa
            // no modo estrito, e complementa o uBlock em vez de o repetir —
            // apanha o rastreio por redireccionamento e o abuso de cookies, que
            // as listas de filtros não descrevem.
            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
            .antiTracking(ContentBlocking.AntiTracking.STRICT)
            // Cada sítio com o seu frasco de cookies: um cookie posto pelo
            // mesmo rastreador em dois sítios deixa de os poder ligar.
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
            .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
            .cookiePurging(true)
            .bounceTrackingProtectionMode(
                ContentBlocking.BounceTrackingProtectionMode.BOUNCE_TRACKING_PROTECTION_MODE_ENABLED
            )
            // Tira os parâmetros de seguimento (`?fbclid=`, `?gclid=`) do URL.
            .queryParameterStrippingEnabled(true)
            // A Navegação Segura pergunta à Google por cada sítio visitado.
            // Fica desligada: num navegador destes é uma fuga, não uma defesa.
            .safeBrowsing(ContentBlocking.SafeBrowsing.NONE)
            .build()

        val definicoesRuntime = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(definicoes.javascript)
            .contentBlocking(bloqueio)
            .globalPrivacyControlEnabled(true)
            .allowInsecureConnections(
                if (definicoes.apenasHttps) GeckoRuntimeSettings.HTTPS_ONLY
                else GeckoRuntimeSettings.ALLOW_ALL
            )
            .aboutConfigEnabled(true)
            .remoteDebuggingEnabled(false)
            .build()

        runtimeInterno = GeckoRuntime.create(context.applicationContext, definicoesRuntime)
        aplicarPreferenciasDePrivacidade()
        permitirManifestV2ENtaoInstalarUblock()
    }

    // --- preferências -------------------------------------------------------

    private fun aplicar(vararg prefs: SetGeckoPreference<*>) {
        GeckoPreferenceController.setGeckoPrefs(prefs.toList()).accept(
            { resultado -> resultado?.filterValues { !it }?.keys?.takeIf { it.isNotEmpty() }
                ?.let { Log.w(TAG, "preferências recusadas: $it") } },
            { erro -> Log.e(TAG, "falha a aplicar preferências", erro) },
        )
    }

    private const val UTILIZADOR = GeckoPreferenceController.PREF_BRANCH_USER

    /**
     * As proteções que não dependem do tor e por isso valem sempre.
     */
    private fun aplicarPreferenciasDePrivacidade() {
        aplicar(
            // O WebRTC descobre o IP local e o público mesmo por trás de um
            // proxy — é a fuga clássica de qualquer navegador com tor.
            SetGeckoPreference.setBoolPref("media.peerconnection.enabled", false, UTILIZADOR),

            // Cada sítio no seu compartimento: cookies, cache, armazenamento e
            // ligações deixam de ser partilhados entre primeiras partes.
            SetGeckoPreference.setBoolPref("privacy.firstparty.isolate", true, UTILIZADOR),

            // O referenciador só sai dentro da mesma origem.
            SetGeckoPreference.setIntPref("network.http.referer.XOriginPolicy", 2, UTILIZADOR),
            SetGeckoPreference.setIntPref("network.http.referer.XOriginTrimmingPolicy", 2, UTILIZADOR),

            // Antecipação de DNS e de ligações contacta servidores de sítios que
            // o utilizador ainda não escolheu visitar — e fá-lo fora do proxy.
            SetGeckoPreference.setBoolPref("network.dns.disablePrefetch", true, UTILIZADOR),
            SetGeckoPreference.setBoolPref("network.predictor.enabled", false, UTILIZADOR),
            SetGeckoPreference.setBoolPref("network.prefetch-next", false, UTILIZADOR),
            SetGeckoPreference.setBoolPref("browser.send_pings", false, UTILIZADOR),
            SetGeckoPreference.setBoolPref("beacon.enabled", false, UTILIZADOR),

            SetGeckoPreference.setBoolPref("geo.enabled", false, UTILIZADOR),
            SetGeckoPreference.setBoolPref("dom.battery.enabled", false, UTILIZADOR),
            SetGeckoPreference.setBoolPref("device.sensors.enabled", false, UTILIZADOR),

            // Sem telemetria nem relatórios de falha.
            SetGeckoPreference.setBoolPref("datareporting.healthreport.uploadEnabled", false, UTILIZADOR),
            SetGeckoPreference.setBoolPref("toolkit.telemetry.enabled", false, UTILIZADOR),
        )
        aplicarResistenciaAImpressaoDigital(definicaoResistencia)
    }

    /** Guardado à parte porque a interface pode alterná-lo sem reiniciar. */
    @Volatile
    private var definicaoResistencia: Boolean = false

    /**
     * `resistFingerprinting` é a proteção mais forte que existe contra
     * identificação por características do aparelho — e a que mais parte
     * sítios: uniformiza o tamanho da janela, o fuso horário, os tipos de letra
     * e os relógios. Fica por escolha do utilizador, com o custo dito.
     */
    fun aplicarResistenciaAImpressaoDigital(ligada: Boolean) {
        definicaoResistencia = ligada
        if (!pronto) return
        aplicar(
            SetGeckoPreference.setBoolPref("privacy.resistFingerprinting", ligada, UTILIZADOR),
            SetGeckoPreference.setBoolPref("privacy.fingerprintingProtection", true, UTILIZADOR),
        )
    }

    // --- proxy tor ----------------------------------------------------------

    /**
     * Encaminha o Gecko para o SOCKS do tor, ou volta à ligação direta.
     *
     * Duas preferências valem por si só o resto do trabalho:
     *
     * - `socks_remote_dns`: sem ela o Gecko resolve os nomes no telemóvel antes
     *   de falar com o proxy. Além de revelar ao DNS do operador tudo o que se
     *   visita, torna os `.onion` impossíveis — não existem em DNS nenhum.
     * - `blockDotOnion`: o Firefox recusa `.onion` por omissão, justamente para
     *   não os deixar vazar para o DNS. Com o tor ligado, deixa de fazer
     *   sentido e tem de ser desligada, senão o endereço nem é tentado.
     */
    fun aplicarProxy(portoSocks: Int?) {
        if (!pronto) return
        if (portoSocks == null) {
            aplicar(
                SetGeckoPreference.setIntPref("network.proxy.type", 0, UTILIZADOR),
                // Sem tor não há como chegar a um onion; recusá-lo cedo dá um
                // erro claro em vez de uma espera até esgotar o tempo.
                SetGeckoPreference.setBoolPref("network.dns.blockDotOnion", true, UTILIZADOR),
            )
            Log.i(TAG, "proxy desligado: ligação direta")
            return
        }
        aplicar(
            SetGeckoPreference.setIntPref("network.proxy.type", 1, UTILIZADOR),
            SetGeckoPreference.setStringPref("network.proxy.socks", "127.0.0.1", UTILIZADOR),
            SetGeckoPreference.setIntPref("network.proxy.socks_port", portoSocks, UTILIZADOR),
            SetGeckoPreference.setIntPref("network.proxy.socks_version", 5, UTILIZADOR),
            SetGeckoPreference.setBoolPref("network.proxy.socks_remote_dns", true, UTILIZADOR),
            SetGeckoPreference.setBoolPref("network.dns.blockDotOnion", false, UTILIZADOR),
            // Sem isto, um sítio podia mandar o navegador falar com 127.0.0.1 e
            // contornar o proxy para descobrir que portos estão abertos.
            SetGeckoPreference.setBoolPref("network.proxy.allow_hijacking_localhost", false, UTILIZADOR),
            // Nada escapa ao proxy, nem sequer os anfitriões locais.
            SetGeckoPreference.setStringPref("network.proxy.no_proxies_on", "", UTILIZADOR),
        )
        Log.i(TAG, "proxy em socks5://127.0.0.1:$portoSocks com DNS remoto")
    }

    /**
     * Aponta o proxy para um porto fechado enquanto o tor arranca.
     *
     * Sem isto haveria uma janela — os segundos entre carregar no interruptor e
     * o tor abrir o circuito — em que o modo tor já está ligado na interface mas
     * o Gecko ainda sai em direto. Uma fuga curta é uma fuga: basta um pedido
     * para o IP de casa ficar registado do outro lado. Apontar para um porto
     * onde não há nada faz os pedidos falharem, que é o comportamento correto.
     */
    fun suspenderRedeAteTor() {
        if (!pronto) return
        aplicar(
            SetGeckoPreference.setIntPref("network.proxy.type", 1, UTILIZADOR),
            SetGeckoPreference.setStringPref("network.proxy.socks", "127.0.0.1", UTILIZADOR),
            SetGeckoPreference.setIntPref("network.proxy.socks_port", 1, UTILIZADOR),
            SetGeckoPreference.setIntPref("network.proxy.socks_version", 5, UTILIZADOR),
            SetGeckoPreference.setBoolPref("network.proxy.socks_remote_dns", true, UTILIZADOR),
            SetGeckoPreference.setStringPref("network.proxy.no_proxies_on", "", UTILIZADOR),
        )
        Log.i(TAG, "rede suspensa à espera do tor")
    }

    // --- uBlock Origin ------------------------------------------------------

    /**
     * Instala o uBlock Origin embutido no APK.
     *
     * `ensureBuiltIn` é idempotente: instala na primeira vez e, nas seguintes,
     * confirma que continua lá e atualiza se a versão do APK trouxer outra. Vir
     * embutido em vez de ser descarregado do addons.mozilla.org significa que
     * bloqueia desde o primeiro pedido e não depende de a Mozilla estar de pé.
     */
    /**
     * O uBlock Origin é Manifest V2, e o Firefox 155 já traz o V2 desligado por
     * omissão — daí a instalação falhar com um lacónico "Extension is invalid".
     * A preferência tem de estar posta **antes** de instalar, e a instalação tem
     * de esperar pela confirmação: postas as duas em paralelo, a instalação
     * ganhava a corrida e falhava na mesma.
     *
     * O uBlock Origin Lite é a versão V3, mas é bastante mais fraca — perde a
     * filtragem cosmética dinâmica e o bloqueio por pedido. Por isso a escolha
     * é manter o uBO verdadeiro e ligar o V2.
     */
    private fun permitirManifestV2ENtaoInstalarUblock() {
        GeckoPreferenceController.setGeckoPrefs(
            listOf(SetGeckoPreference.setBoolPref("extensions.manifestV2.enabled", true, UTILIZADOR))
        ).accept(
            { instalarUblock() },
            { erro ->
                Log.e(TAG, "não foi possível ligar o Manifest V2", erro)
                _ublock.value = EstadoUblock.Falhou("Manifest V2 recusado")
            },
        )
    }

    fun instalarUblock() {
        if (!pronto) return
        _ublock.value = EstadoUblock.AInstalar
        runtime.webExtensionController.ensureBuiltIn(URI_UBLOCK, ID_UBLOCK).accept(
            { extensao ->
                val versao = extensao?.metaData?.version ?: "?"
                _ublock.value = EstadoUblock.Instalado(versao)
                Log.i(TAG, "uBlock Origin $versao instalado")
                // Sem isto a extensão não corre em separadores privados, que é
                // precisamente onde mais faz falta.
                extensao?.let { ext ->
                    runtime.webExtensionController.setAllowedInPrivateBrowsing(ext, true)
                }
            },
            { erro ->
                Log.e(TAG, "falha a instalar o uBlock Origin", erro)
                _ublock.value = EstadoUblock.Falhou(erro?.message ?: "erro desconhecido")
            },
        )
    }

    // --- definições em tempo de execução ------------------------------------

    fun definirJavascript(ligado: Boolean) {
        if (pronto) runtime.settings.javaScriptEnabled = ligado
    }

    /**
     * O isolamento de cookies por sítio (dFPI) já é escolhido ao criar o
     * runtime, mas o `ContentBlocking.Settings` não muda depois disso. A
     * preferência equivalente muda, e é por ela que a definição passa a valer
     * sem reiniciar a app.
     *
     * Desligado não quer dizer "aceitar tudo": fica em 4, que continua a recusar
     * cookies de rastreadores conhecidos. Uma definição de privacidade que ao
     * ser desligada abrisse as portas todas seria uma armadilha.
     */
    fun definirIsolamentoDeCookies(ligado: Boolean) {
        if (!pronto) return
        aplicar(
            SetGeckoPreference.setIntPref(
                "network.cookie.cookieBehavior", if (ligado) 5 else 4, UTILIZADOR,
            )
        )
    }

    fun definirApenasHttps(ligado: Boolean) {
        if (pronto) {
            runtime.settings.allowInsecureConnections =
                if (ligado) GeckoRuntimeSettings.HTTPS_ONLY else GeckoRuntimeSettings.ALLOW_ALL
        }
    }
}
