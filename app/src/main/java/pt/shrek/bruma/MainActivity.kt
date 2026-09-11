package pt.shrek.bruma

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import pt.shrek.bruma.core.Definicoes
import pt.shrek.bruma.ui.NavegadorViewModel
import pt.shrek.bruma.ui.ecrans.EcraBloqueio
import pt.shrek.bruma.ui.ecrans.EcraNavegador
import pt.shrek.bruma.ui.ecrans.podeAutenticar
import pt.shrek.bruma.ui.tema.TemaBruma

/**
 * [FragmentActivity] e não `ComponentActivity` por uma razão só: o
 * `BiometricPrompt` da androidx exige uma, para se prender ao ciclo de vida dos
 * fragmentos e sobreviver a rotações do ecrã a meio da autenticação.
 */
class MainActivity : FragmentActivity() {

    private val vm: NavegadorViewModel by viewModels()

    /**
     * As definições vêm do ViewModel, e **não** de uma instância própria.
     *
     * Cada `Definicoes` tem o seu próprio estado do Compose por trás; duas
     * instâncias seriam dois conjuntos de valores que não se falam, e mudar o
     * ecrã seguro nas definições nunca chegaria a esta janela. Uma só instância
     * por app é o que faz a definição valer em todo o lado.
     */
    private val definicoes: Definicoes get() = vm.definicoes

    /** Volta a trancar sempre que a app sai de vista. */
    private var desbloqueado by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaBruma {
                // O ecrã seguro é aplicado aqui e não só no arranque: mudar a
                // definição tem de valer de imediato, senão a app continuava a
                // aparecer nas capturas até ser reiniciada.
                LaunchedEffect(definicoes.ecraSeguro) {
                    if (definicoes.ecraSeguro) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                val trancada = definicoes.pedirBiometria &&
                    podeAutenticar(this@MainActivity) &&
                    !desbloqueado

                if (trancada) {
                    EcraBloqueio(aoDesbloquear = { desbloqueado = true })
                } else {
                    EcraNavegador(vm)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Tranca ao sair de vista, não ao destruir: se só trancasse na
        // destruição, bastava alternar de app e voltar para entrar sem
        // identificação nenhuma.
        if (definicoes.pedirBiometria) desbloqueado = false
    }
}
