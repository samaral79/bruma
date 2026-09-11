package pt.shrek.bruma.tor

import android.util.Log
import io.matthewnelson.kmp.tor.common.api.ExperimentalKmpTorApi
import io.matthewnelson.kmp.tor.resource.exec.tor.ResourceLoaderTorExec
import io.matthewnelson.kmp.tor.runtime.Action
import io.matthewnelson.kmp.tor.runtime.RuntimeEvent
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnEvent
import io.matthewnelson.kmp.tor.runtime.core.OnFailure
import io.matthewnelson.kmp.tor.runtime.core.OnSuccess
import io.matthewnelson.kmp.tor.runtime.core.TorEvent
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
import io.matthewnelson.kmp.tor.runtime.core.ctrl.TorCmd
import io.matthewnelson.kmp.tor.runtime.service.TorServiceConfig
import io.matthewnelson.kmp.tor.runtime.service.TorServiceUI
import io.matthewnelson.kmp.tor.runtime.service.ui.KmpTorServiceUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.shrek.bruma.R

/** O que a interface precisa de saber sobre o tor, e nada mais. */
sealed interface EstadoTor {
    data object Desligado : EstadoTor
    /** [progresso] é a percentagem de arranque do circuito, 0 a 100. */
    data class ALigar(val progresso: Int) : EstadoTor
    data class Pronto(val portoSocks: Int) : EstadoTor
    data class Falhou(val causa: String) : EstadoTor
}

/**
 * O tor corre dentro da app, não num Orbot à parte: o binário vem no APK
 * (kmp-tor + resource-exec-tor) e é executado num serviço em primeiro plano,
 * para o Android não o matar quando o ecrã se apaga.
 *
 * O resultado que interessa ao resto da app é uma coisa só — o porto SOCKS que
 * o tor abriu — porque é isso que se entrega ao WebView. O porto é pedido em
 * `auto`: fixar 9050 dava conflitos com o Orbot e com qualquer outra app que o
 * ocupasse primeiro, e o tor recusava arrancar.
 */
object MotorTor {

    private const val TAG = "Bruma.Tor"

    private val _estado = MutableStateFlow<EstadoTor>(EstadoTor.Desligado)
    val estado: StateFlow<EstadoTor> = _estado

    /**
     * O porto SOCKS atual, ou null se o tor não está pronto. Lido da linha de
     * fundo do WebView, daí ser volátil.
     */
    @Volatile
    var portoSocks: Int? = null
        private set

    private val configuracaoServico: TorServiceConfig by lazy {
        val ui = KmpTorServiceUI.Factory(
            iconReady = R.drawable.ic_tor_pronto,
            iconNotReady = R.drawable.ic_tor_a_ligar,
            info = TorServiceUI.NotificationInfo(
                notificationId = 8118,
                channelId = "bruma_tor",
                channelName = R.string.canal_tor_nome,
                channelDescription = R.string.canal_tor_descricao,
                channelShowBadge = false,
                channelImportanceLow = true,
            ),
            block = {
                defaultConfig {
                    iconData = R.drawable.ic_tor_pronto
                    enableActionRestart = true
                    enableActionStop = true
                }
            },
        )
        TorServiceConfig.Foreground.Builder(ui) {}
    }

    private val ambiente: TorRuntime.Environment by lazy {
        configuracaoServico.newEnvironment(ResourceLoaderTorExec::getOrCreate)
    }

    private val runtime: TorRuntime by lazy {
        TorRuntime.Builder(ambiente) {
            val imediato = OnEvent.Executor.Immediate

            observerStatic(RuntimeEvent.STATE, imediato) { estadoTor ->
                val daemon = estadoTor.daemon
                when {
                    daemon.isOn && daemon.isBootstrapped -> Unit // o porto chega por LISTENERS
                    daemon.isOn || daemon.isStarting -> {
                        if (_estado.value !is EstadoTor.Pronto) {
                            _estado.value = EstadoTor.ALigar(daemon.bootstrap.toInt())
                        }
                    }
                    daemon.isOff -> {
                        portoSocks = null
                        _estado.value = EstadoTor.Desligado
                    }
                }
            }

            // O porto SOCKS só existe depois de o tor abrir o ouvinte. Pedi-lo
            // antes disto devolveria sempre null e o WebView ficaria sem proxy.
            observerStatic(RuntimeEvent.LISTENERS, imediato) { ouvintes ->
                val socks = ouvintes.socks.firstOrNull()
                if (socks == null) {
                    portoSocks = null
                    if (_estado.value is EstadoTor.Pronto) _estado.value = EstadoTor.Desligado
                } else {
                    portoSocks = socks.port.value
                    _estado.value = EstadoTor.Pronto(socks.port.value)
                    Log.i(TAG, "socks em 127.0.0.1:${socks.port.value}")
                }
            }

            observerStatic(RuntimeEvent.ERROR, imediato) { erro ->
                Log.e(TAG, "erro do runtime", erro)
                _estado.value = EstadoTor.Falhou(erro.message ?: erro::class.java.simpleName)
            }

            observerStatic(RuntimeEvent.LOG.WARN, imediato) { Log.w(TAG, it) }

            config {
                @OptIn(ExperimentalKmpTorApi::class)
                TorOption.__SocksPort.configure {
                    auto()
                    flagsIsolation {
                        // Um circuito por destino aproxima o isolamento por
                        // primeira parte do Tor Browser: dois sítios abertos ao
                        // mesmo tempo não partilham nó de saída, e por isso não
                        // se veem um ao outro pelo mesmo IP.
                        IsolateDestAddr = true
                        IsolateDestPort = true
                    }
                    flagsSocks {
                        // O WebView não deve resolver nomes localmente: a
                        // resolução tem de ir pelo tor, senão o `.onion` não
                        // existe para o sistema e o pedido morre — e os nomes
                        // visitados passavam pelo DNS do operador.
                        NoDNSRequest = false
                        CacheDNS = true
                    }
                }
            }

            required(TorEvent.ERR)
            required(TorEvent.WARN)
        }
    }

    fun ligar() {
        if (_estado.value is EstadoTor.Pronto) return
        _estado.value = EstadoTor.ALigar(0)
        runtime.enqueue(
            Action.StartDaemon,
            OnFailure { erro ->
                Log.e(TAG, "falha a arrancar", erro)
                _estado.value = EstadoTor.Falhou(erro.message ?: "falha a arrancar o tor")
            },
            OnSuccess.noOp(),
        )
    }

    fun desligar() {
        portoSocks = null
        _estado.value = EstadoTor.Desligado
        runtime.enqueue(Action.StopDaemon, OnFailure.noOp(), OnSuccess.noOp())
    }

    /** Pede circuitos novos — o equivalente ao "Nova identidade" do Tor Browser. */
    fun novaIdentidade() {
        runtime.enqueue(TorCmd.Signal.NewNym, OnFailure.noOp(), OnSuccess.noOp())
    }
}
