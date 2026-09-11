package pt.shrek.bruma.ui.ecrans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import pt.shrek.bruma.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import pt.shrek.bruma.core.Fundo
import pt.shrek.bruma.core.MotorBusca

/**
 * A página inicial.
 *
 * Tem exatamente duas coisas: o fundo e uma caixa para escrever. Sem atalhos,
 * sem "sítios mais visitados", sem notícias — um painel de sítios mais visitados
 * é um histórico exposto a quem pegar no telemóvel, o que é o contrário do que
 * esta app existe para fazer.
 */
@Composable
fun EcraInicio(motor: MotorBusca, naDireita: Boolean, aoTocarNaBusca: () -> Unit) {
    val contexto = LocalContext.current
    val configuracao = LocalConfiguration.current
    val densidade = LocalDensity.current

    val larguraPx = with(densidade) { configuracao.screenWidthDp.dp.roundToPx() }
    val alturaPx = with(densidade) { configuracao.screenHeightDp.dp.roundToPx() }

    // A descodificação sai da linha principal: uma fotografia grande demora
    // dezenas de milissegundos e daria um salto visível ao abrir a app.
    val imagem by produceState<ImageBitmap?>(initialValue = null, larguraPx, alturaPx) {
        value = Fundo.carregar(contexto, larguraPx, alturaPx)
    }

    Box(Modifier.fillMaxSize()) {
        if (imagem != null) {
            Image(
                bitmap = imagem!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Véu por cima da fotografia: sem ele, uma imagem clara deixava o
            // texto da caixa de busca ilegível.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
        } else {
            FundoDeNevoa()
        }

        Column(
            modifier = Modifier
                .align(BiasAlignment(0f, 0.35f))
                .fillMaxWidth()
                // A cápsula ocupa uma faixa de ~66 dp de um dos lados. O
                // conteúdo do início afasta-se dela, senão a caixa de busca
                // passava-lhe por baixo e ficavam os dois ilegíveis.
                .padding(
                    start = if (naDireita) 28.dp else 84.dp,
                    end = if (naDireita) 84.dp else 28.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "bruma",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.92f),
            )
            Text(
                motor.etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { aoTocarNaBusca() }
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    stringResource(R.string.inicio_procurar),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
    }
}

/**
 * O fundo por omissão: névoa. Desenhado, não fotografado — um degradê escuro com
 * dois halos difusos, que não pesa um byte no APK e nunca fica datado.
 */
@Composable
private fun FundoDeNevoa() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0C0E), Color(0xFF12121A), Color(0xFF0B0C0E))
                )
            )
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x2E7C6BF0), Color.Transparent),
                    radius = 760f,
                )
            )
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0x1F3FB9A0), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(120f, 2100f),
                    radius = 900f,
                )
            )
    )
}
