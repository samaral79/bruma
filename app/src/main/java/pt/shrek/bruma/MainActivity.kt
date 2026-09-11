package pt.shrek.bruma

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import pt.shrek.bruma.core.Definicoes
import pt.shrek.bruma.ui.NavegadorViewModel
import pt.shrek.bruma.ui.ecrans.EcraNavegador
import pt.shrek.bruma.ui.tema.TemaBruma

class MainActivity : ComponentActivity() {

    private val vm: NavegadorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val definicoes = Definicoes(this)
        if (definicoes.ecraSeguro) {
            // Tem de ser antes do setContent: aplicado depois, a primeira
            // miniatura da app já foi para a lista de recentes.
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        enableEdgeToEdge()
        setContent {
            TemaBruma { EcraNavegador(vm) }
        }
    }
}
