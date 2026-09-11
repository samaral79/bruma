package pt.shrek.bruma.navegador

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import pt.shrek.bruma.R
import pt.shrek.bruma.core.Endereco

/** O que a barra precisa de saber sobre a segurança da ligação. */
enum class Seguranca { NENHUMA, CIFRADA, ONION }

/**
 * Um separador: uma [GeckoSession] e o estado que a interface mostra.
 *
 * A sessão é criada aqui e vive enquanto o separador existir. Trocar de
 * separador só troca qual das sessões está ligada à vista — a página não
 * recarrega, não perde a posição nem os formulários a meio.
 */
class Separador(
    val id: Long,
    val privado: Boolean,
    // Precisa de contexto para traduzir as mensagens de erro. É o contexto da
    // aplicação, não o da atividade: um separador sobrevive a rotações e a
    // atividade não.
    private val contexto: Context,
    private val runtime: GeckoRuntime,
    private val aoPedirNovoSeparador: (String) -> Unit,
    private val aoAvisar: (String) -> Unit,
    private val aoFechar: (Separador) -> Unit,
) {

    var url by mutableStateOf("")
        private set
    var titulo by mutableStateOf("")
        private set
    var progresso by mutableIntStateOf(0)
        private set
    var aCarregar by mutableStateOf(false)
        private set
    var podeVoltar by mutableStateOf(false)
        private set
    var podeAvancar by mutableStateOf(false)
        private set
    var seguranca by mutableStateOf(Seguranca.NENHUMA)
        private set
    var bloqueados by mutableIntStateOf(0)
        private set

    val sessao: GeckoSession = GeckoSession(
        GeckoSessionSettings.Builder()
            .usePrivateMode(privado)
            .useTrackingProtection(true)
            .build()
    )

    val etiqueta: String
        get() = titulo.ifBlank { Endereco.paraMostrar(url) }
            .ifBlank { contexto.getString(R.string.separador_vazio) }

    init {
        sessao.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                sessao: GeckoSession,
                novoUrl: String?,
                permissoes: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                temUtilizadorGesto: Boolean,
            ) {
                // O motor anuncia `about:blank` ao abrir a sessão, e essa
                // notificação chega **depois** de já lhe termos mandado carregar
                // um endereço. Aceitá-la apagava o URL acabado de pedir, e a app
                // saltava para o ecrã inicial com a página a carregar por trás —
                // que foi o ecrã preto que apareceu ao abrir um link de fora.
                //
                // `about:blank` nunca é uma morada que interesse mostrar. Quando
                // é mesmo para esvaziar o separador, quem o faz é
                // [limparParaInicio], que limpa o estado à mão.
                val morada = novoUrl.orEmpty()
                if (morada == "about:blank") return
                url = morada
                // O contador de bloqueios é por página: mantê-lo entre páginas
                // daria um número grande e sem significado.
                bloqueados = 0
                seguranca = when {
                    Endereco.ehOnion(url) -> Seguranca.ONION
                    url.startsWith("https://") -> Seguranca.CIFRADA
                    else -> Seguranca.NENHUMA
                }
            }

            override fun onCanGoBack(sessao: GeckoSession, valor: Boolean) {
                podeVoltar = valor
                Log.i(TAG, "podeVoltar=$valor em ${Endereco.paraMostrar(url)}")
            }
            override fun onCanGoForward(sessao: GeckoSession, valor: Boolean) { podeAvancar = valor }

            override fun onLoadRequest(
                sessao: GeckoSession,
                pedido: GeckoSession.NavigationDelegate.LoadRequest,
            ): GeckoResult<AllowOrDeny> {
                val esquema = pedido.uri.substringBefore(':').lowercase()
                // Esquemas que saltam para outra app (intent://, market://,
                // tg://) tiram o tráfego de dentro do tor sem aviso e revelam
                // ao sítio que apps estão instaladas. Não se seguem.
                if (esquema !in ESQUEMAS_PERMITIDOS) {
                    aoAvisar(contexto.getString(R.string.aviso_esquema_recusado, esquema))
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(sessao: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                // As janelas novas passam a separadores, em vez de janelas
                // flutuantes que não caberiam numa interface para uma mão.
                aoPedirNovoSeparador(uri)
                return null
            }

            override fun onLoadError(
                sessao: GeckoSession,
                uri: String?,
                erro: org.mozilla.geckoview.WebRequestError,
            ): GeckoResult<String>? {
                Log.w(TAG, "erro a carregar $uri: categoria=${erro.category} código=${erro.code}")
                aoAvisar(descreverErro(erro))
                return null
            }
        }

        sessao.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(sessao: GeckoSession, url: String) {
                aCarregar = true
                progresso = 0
            }

            override fun onPageStop(sessao: GeckoSession, sucesso: Boolean) {
                aCarregar = false
                progresso = 100
            }

            override fun onProgressChange(sessao: GeckoSession, valor: Int) {
                progresso = valor
            }

            override fun onSecurityChange(
                sessao: GeckoSession,
                info: GeckoSession.ProgressDelegate.SecurityInformation,
            ) {
                if (seguranca != Seguranca.ONION) {
                    seguranca = if (info.isSecure) Seguranca.CIFRADA else Seguranca.NENHUMA
                }
            }
        }

        sessao.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(sessao: GeckoSession, novoTitulo: String?) {
                titulo = novoTitulo.orEmpty()
            }

            override fun onCloseRequest(sessao: GeckoSession) {
                aoFechar(this@Separador)
            }

            override fun onCrash(sessao: GeckoSession) {
                aoAvisar("O separador estoirou e foi recarregado")
                sessao.open(runtime)
                if (url.isNotBlank()) sessao.loadUri(url)
            }
        }

        sessao.contentBlockingDelegate = object : ContentBlocking.Delegate {
            override fun onContentBlocked(sessao: GeckoSession, evento: ContentBlocking.BlockEvent) {
                bloqueados++
            }
        }

        sessao.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onContentPermissionRequest(
                sessao: GeckoSession,
                permissao: GeckoSession.PermissionDelegate.ContentPermission,
            ): GeckoResult<Int> {
                // Localização, notificações e armazenamento persistente são
                // recusados sem perguntar: cada pedido aceite é um traço a mais,
                // e um diálogo a meio da página é onde as pessoas carregam em
                // "permitir" só para o tirar da frente.
                return GeckoResult.fromValue(
                    GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                )
            }

            override fun onMediaPermissionRequest(
                sessao: GeckoSession,
                uri: String,
                video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                callback: GeckoSession.PermissionDelegate.MediaCallback,
            ) {
                aoAvisar(contexto.getString(R.string.aviso_media_recusado))
                callback.reject()
            }
        }

        sessao.open(runtime)
    }

    fun abrir(destino: String) {
        if (destino.isBlank()) return
        url = destino
        sessao.loadUri(destino)
    }

    /** Descarrega a página e esquece por onde andou. */
    fun limparParaInicio() {
        sessao.stop()
        sessao.loadUri("about:blank")
        sessao.purgeHistory()
        url = ""
        titulo = ""
        bloqueados = 0
        seguranca = Seguranca.NENHUMA
    }

    fun recarregar() = sessao.reload()
    fun parar() = sessao.stop()
    fun voltar() = sessao.goBack()
    fun avancar() = sessao.goForward()

    fun ativar(ativo: Boolean) = sessao.setActive(ativo)

    fun destruir() {
        runCatching {
            sessao.stop()
            sessao.purgeHistory()
            sessao.close()
        }.onFailure { Log.w(TAG, "falha a fechar sessão", it) }
    }

    private fun descreverErro(erro: org.mozilla.geckoview.WebRequestError): String =
        contexto.getString(
            when (erro.code) {
                org.mozilla.geckoview.WebRequestError.ERROR_UNKNOWN_HOST ->
                    if (Endereco.ehOnion(url)) R.string.erro_onion_sem_tor else R.string.erro_anfitriao
                org.mozilla.geckoview.WebRequestError.ERROR_PROXY_CONNECTION_REFUSED -> R.string.erro_proxy
                org.mozilla.geckoview.WebRequestError.ERROR_NET_TIMEOUT -> R.string.erro_tempo
                org.mozilla.geckoview.WebRequestError.ERROR_SECURITY_SSL -> R.string.erro_certificado
                org.mozilla.geckoview.WebRequestError.ERROR_HTTPS_ONLY -> R.string.erro_https
                else -> R.string.erro_generico
            }
        )

    private companion object {
        const val TAG = "Bruma.Separador"
        val ESQUEMAS_PERMITIDOS = setOf("http", "https", "about", "data", "blob", "resource")
    }
}
