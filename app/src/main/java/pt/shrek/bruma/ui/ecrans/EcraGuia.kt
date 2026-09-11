package pt.shrek.bruma.ui.ecrans

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.shrek.bruma.core.Mao
import pt.shrek.bruma.ui.tema.CoresEstado

private class Pagina(
    val titulo: String,
    val texto: String,
    val desenho: DesenhoGuia,
)

private enum class DesenhoGuia { MARCA, CAPSULA, GESTOS, TOR }

/**
 * O guia que aparece na primeira abertura.
 *
 * Existe porque esta app não se explica sozinha: não tem barra de endereço no
 * sítio do costume, nem menu no canto, e quase tudo se faz por gestos sobre uma
 * cápsula lateral. Sem uma explicação de trinta segundos, a primeira impressão
 * de quem a instala é a de um navegador avariado.
 *
 * Só aparece uma vez, e pode ser revisto nas Definições — quem o saltou à
 * pressa não fica sem ele para sempre.
 */
@Composable
fun EcraGuia(mao: Mao, aoTerminar: () -> Unit) {
    val paginas = listOf(
        Pagina(
            "bruma",
            "Um navegador para uma mão só.\n\nTor por dentro, sem precisar de mais nenhuma app, e o uBlock Origin a sério a bloquear anúncios e rastreadores.",
            DesenhoGuia.MARCA,
        ),
        Pagina(
            "A cápsula",
            "Não há barra em cima nem em baixo. Há uma cápsula do lado da tua mão, à altura do polegar.\n\nO ponto em cima diz-te tudo: roxo é tor ligado, cinzento é ligação cifrada, vermelho é sem cifra.",
            DesenhoGuia.CAPSULA,
        ),
        Pagina(
            "Os gestos",
            "Tocar abre a caixa de endereço.\n\nArrastar para cima ou para baixo troca de separador.\n\nArrastar para dentro abre as ações. Para fora esconde a cápsula, para leres sem nada à frente.",
            DesenhoGuia.GESTOS,
        ),
        Pagina(
            "O Tor",
            "O escudo nas ações liga o Tor. A partir daí o tráfego sai pela rede Tor, os endereços .onion funcionam, e a busca passa pelo DuckDuckGo onion.\n\nNão é o Tor Browser: esconde de onde estás, mas não te torna igual a toda a gente.",
            DesenhoGuia.TOR,
        ),
    )

    val estado = rememberPagerState { paginas.size }
    val escopo = rememberCoroutineScope()
    val ultima = estado.currentPage == paginas.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0C0E), Color(0xFF12121A), Color(0xFF0B0C0E))
                )
            )
    ) {
        // "Saltar" fica em cima, longe do polegar: é a saída de emergência, não
        // o caminho principal, e não deve ser premida por acidente.
        if (!ultima) {
            Text(
                "Saltar",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(20.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { aoTerminar() },
            )
        }

        HorizontalPager(
            state = estado,
            modifier = Modifier.fillMaxSize(),
        ) { indice ->
            val pagina = paginas[indice]
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 34.dp)
                    .padding(top = 96.dp, bottom = 190.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Ilustracao(pagina.desenho, mao)
                Spacer(Modifier.height(42.dp))
                Text(
                    pagina.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    pagina.texto,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Os comandos vivem em baixo, onde o polegar chega — a app toda segue
        // essa regra, e o guia que a ensina não podia ser a exceção.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 34.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(paginas.size) { i ->
                    val ativo = i == estado.currentPage
                    val largura by animateFloatAsState(if (ativo) 22f else 7f, label = "ponto")
                    Box(
                        Modifier
                            .width(largura.dp)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (ativo) CoresEstado.tor else Color.White.copy(alpha = 0.22f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(29.dp))
                    .background(if (ultima) CoresEstado.tor else Color.White.copy(alpha = 0.1f))
                    .clickable {
                        if (ultima) aoTerminar()
                        else escopo.launch { estado.animateScrollToPage(estado.currentPage + 1) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (ultima) "Começar" else "Seguinte",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (ultima) Color(0xFF0B0C0E) else Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/**
 * Os desenhos são feitos à mão em Canvas, não são imagens.
 *
 * São quatro formas simples; como imagens teriam de vir em várias resoluções,
 * pesariam no APK e ficariam presas a um tema. Desenhadas, acompanham a paleta
 * e o lado da mão que a pessoa escolheu.
 */
@Composable
private fun Ilustracao(desenho: DesenhoGuia, mao: Mao) {
    val naDireita = mao == Mao.DIREITA
    Canvas(Modifier.size(width = 168.dp, height = 232.dp)) {
        val contorno = Color.White.copy(alpha = 0.18f)
        val telemovel = Size(size.width * 0.78f, size.height * 0.92f)
        val canto = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx())
        val origem = Offset((size.width - telemovel.width) / 2f, (size.height - telemovel.height) / 2f)

        drawRoundRect(
            color = contorno,
            topLeft = origem,
            size = telemovel,
            cornerRadius = canto,
            style = Stroke(width = 2.dp.toPx()),
        )

        when (desenho) {
            DesenhoGuia.MARCA -> {
                // Três traços de névoa a atravessar o ecrã.
                repeat(3) { i ->
                    val y = origem.y + telemovel.height * (0.40f + i * 0.10f)
                    drawLine(
                        color = CoresEstado.tor.copy(alpha = 0.75f - i * 0.2f),
                        start = Offset(origem.x + telemovel.width * 0.22f, y),
                        end = Offset(origem.x + telemovel.width * 0.78f, y),
                        strokeWidth = 5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            }
            DesenhoGuia.CAPSULA -> desenharCapsula(origem, telemovel, naDireita, realce = true)
            DesenhoGuia.GESTOS -> {
                desenharCapsula(origem, telemovel, naDireita, realce = false)
                val cx = capsulaX(origem, telemovel, naDireita)
                val cy = origem.y + telemovel.height * 0.62f
                // Setas em cruz: cima/baixo para separadores, para dentro as ações.
                seta(Offset(cx, cy - 34.dp.toPx()), Offset(cx, cy - 66.dp.toPx()))
                seta(Offset(cx, cy + 34.dp.toPx()), Offset(cx, cy + 66.dp.toPx()))
                val dentro = if (naDireita) -1f else 1f
                seta(
                    Offset(cx + dentro * 20.dp.toPx(), cy),
                    Offset(cx + dentro * 58.dp.toPx(), cy),
                )
            }
            DesenhoGuia.TOR -> {
                // Escudo grande ao centro.
                val c = Offset(origem.x + telemovel.width / 2f, origem.y + telemovel.height * 0.5f)
                val r = 30.dp.toPx()
                drawCircle(color = CoresEstado.tor.copy(alpha = 0.16f), radius = r * 1.7f, center = c)
                drawCircle(color = CoresEstado.tor, radius = r, center = c)
                drawCircle(color = Color(0xFF0B0C0E), radius = r * 0.42f, center = c)
            }
        }
    }
}

private fun DrawScope.capsulaX(origem: Offset, telemovel: Size, naDireita: Boolean): Float =
    if (naDireita) origem.x + telemovel.width - 17.dp.toPx()
    else origem.x + 17.dp.toPx()

private fun DrawScope.desenharCapsula(
    origem: Offset,
    telemovel: Size,
    naDireita: Boolean,
    realce: Boolean,
) {
    val largura = 22.dp.toPx()
    val altura = 72.dp.toPx()
    val x = capsulaX(origem, telemovel, naDireita) - largura / 2f
    val y = origem.y + telemovel.height * 0.62f - altura / 2f
    drawRoundRect(
        color = if (realce) CoresEstado.tor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
        topLeft = Offset(x, y),
        size = Size(largura, altura),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(largura / 2f),
    )
    // O ponto de estado, no topo da cápsula.
    drawCircle(
        color = CoresEstado.tor,
        radius = 4.dp.toPx(),
        center = Offset(x + largura / 2f, y + 11.dp.toPx()),
    )
}

private fun DrawScope.seta(de: Offset, para: Offset) {
    val cor = Color.White.copy(alpha = 0.5f)
    drawLine(cor, de, para, strokeWidth = 2.5f.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // Ponta: dois traços curtos a abrir para trás do sentido do movimento.
    val dx = para.x - de.x
    val dy = para.y - de.y
    val comprimento = kotlin.math.hypot(dx, dy)
    if (comprimento == 0f) return
    val ux = dx / comprimento
    val uy = dy / comprimento
    val p = 7.dp.toPx()
    drawLine(cor, para, Offset(para.x - (ux * p) - (uy * p * 0.6f), para.y - (uy * p) + (ux * p * 0.6f)),
        strokeWidth = 2.5f.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(cor, para, Offset(para.x - (ux * p) + (uy * p * 0.6f), para.y - (uy * p) - (ux * p * 0.6f)),
        strokeWidth = 2.5f.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
}
