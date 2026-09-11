package pt.shrek.bruma.ui.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.shrek.bruma.core.Idioma
import pt.shrek.bruma.core.Idiomas
import pt.shrek.bruma.ui.tema.CoresEstado

/**
 * O primeiro ecrã de todos: escolher o idioma.
 *
 * Vem antes do guia porque o guia é texto — explicá-lo numa língua que a pessoa
 * não lê não explica nada. A lista não é traduzida: cada idioma aparece escrito
 * em si mesmo, que é a única forma de ser reconhecível por quem ainda não
 * percebe nada do que está no ecrã.
 *
 * Não há texto de instruções nem título nesta página, de propósito: seja qual
 * for a língua em que estivesse, seria ilegível para três quartos de quem o vê.
 * Quatro nomes e um botão bastam.
 */
@Composable
fun EcraIdioma(aoEscolher: (Idioma) -> Unit) {
    val contexto = LocalContext.current
    // Começa na língua do telemóvel, se a app a falar: para a maioria das
    // pessoas a escolha certa já está feita e basta confirmar.
    var escolhido by remember { mutableStateOf(Idiomas.sugestaoDoSistema(contexto)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0C0E), Color(0xFF12121A), Color(0xFF0B0C0E))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp)
                .padding(top = 120.dp, bottom = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "bruma",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(46.dp))

            Idioma.entries.forEach { idioma ->
                LinhaIdioma(
                    idioma = idioma,
                    selecionado = idioma == escolhido,
                    aoCarregar = { escolhido = idioma },
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 30.dp, vertical = 34.dp)
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(CoresEstado.tor)
                .clickable { aoEscolher(escolhido) },
            contentAlignment = Alignment.Center,
        ) {
            // O botão fala a língua que está selecionada naquele momento, por
            // isso muda enquanto a pessoa percorre a lista — é a confirmação de
            // que escolheu o que queria, antes sequer de confirmar.
            Text(
                when (escolhido) {
                    Idioma.INGLES -> "Continue"
                    Idioma.PORTUGUES -> "Continuar"
                    Idioma.ESPANHOL -> "Continuar"
                    Idioma.MANDARIM -> "继续"
                },
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF0B0C0E),
            )
        }
    }
}

@Composable
private fun LinhaIdioma(idioma: Idioma, selecionado: Boolean, aoCarregar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selecionado) CoresEstado.tor.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable { aoCarregar() }
            .padding(horizontal = 22.dp, vertical = 19.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            idioma.etiqueta,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selecionado) Color.White else Color.White.copy(alpha = 0.65f),
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (selecionado) CoresEstado.tor else Color.White.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selecionado) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B0C0E))
                )
            }
        }
    }
}
