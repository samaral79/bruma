package pt.shrek.bruma.ui.comum

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoView
import pt.shrek.bruma.navegador.Separador

/**
 * A janela do motor.
 *
 * Há uma só [GeckoView] para todos os separadores: trocar de separador troca a
 * sessão ligada à vista, não a vista. Criar uma vista por separador multiplicava
 * superfícies de composição que o Gecko teria de manter vivas ao mesmo tempo.
 */
@Composable
fun VistaGecko(separador: Separador, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { contexto -> GeckoView(contexto) },
        update = { vista ->
            if (vista.session !== separador.sessao) {
                // Soltar a anterior antes de ligar a seguinte: uma sessão presa
                // a duas vistas deixa a página em branco sem dar erro.
                if (vista.session != null) vista.releaseSession()
                vista.setSession(separador.sessao)
            }
        },
        onRelease = { vista -> if (vista.session != null) vista.releaseSession() },
    )
}
