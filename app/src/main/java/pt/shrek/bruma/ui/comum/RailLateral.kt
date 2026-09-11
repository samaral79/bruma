package pt.shrek.bruma.ui.comum

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pt.shrek.bruma.core.Mao
import pt.shrek.bruma.navegador.Seguranca
import pt.shrek.bruma.navegador.Separador
import pt.shrek.bruma.tor.EstadoTor
import pt.shrek.bruma.ui.tema.CoresEstado
import kotlin.math.abs

/**
 * A cápsula lateral: o único comando permanente da app.
 *
 * Está do lado da mão que segura o telemóvel e no terço inferior do ecrã, que é
 * onde o polegar chega sem mudar a pega. A escolha de a pôr na lateral, e não em
 * baixo, vem da forma do gesto: o polegar não se move em linha reta — descreve
 * um arco com o pivô na base da mão. Uma barra na lateral acompanha esse arco;
 * uma barra em baixo obriga o polegar a esticar-se para os extremos.
 *
 * Os gestos seguem o eixo da cápsula, que agora é vertical:
 *
 *  - tocar → escrever endereço
 *  - arrastar ↑ / ↓ → separador seguinte / anterior
 *  - arrastar para dentro → abre o leque de ações
 *  - arrastar para fora → esconde a cápsula
 *  - premir e segurar → leque de ações
 */
@Composable
fun RailLateral(
    separador: Separador?,
    estadoTor: EstadoTor,
    numeroSeparadores: Int,
    mao: Mao,
    aoTocar: () -> Unit,
    aoAbrirLeque: () -> Unit,
    aoEsconder: () -> Unit,
    aoSeguinte: () -> Unit,
    aoAnterior: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val naDireita = mao == Mao.DIREITA
    val aCarregar = separador?.aCarregar == true
    val progresso by animateFloatAsState(
        targetValue = if (aCarregar) separador.progresso / 100f else 0f,
        label = "progresso",
    )

    val corAcento by animateColorAsState(
        targetValue = when {
            estadoTor is EstadoTor.Pronto -> CoresEstado.tor
            estadoTor is EstadoTor.ALigar -> CoresEstado.aviso
            estadoTor is EstadoTor.Falhou -> CoresEstado.perigo
            separador?.seguranca == Seguranca.CIFRADA -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> CoresEstado.perigo
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "acento",
    )

    val superficie = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .width(50.dp)
            .height(156.dp)
            .clip(RoundedCornerShape(25.dp))
            // Translúcida sobre a página, com um brilho do lado de dentro: dá
            // profundidade sem esconder o conteúdo que fica por baixo.
            .background(
                Brush.horizontalGradient(
                    if (naDireita) listOf(superficie.copy(alpha = 0.93f), superficie)
                    else listOf(superficie, superficie.copy(alpha = 0.93f))
                )
            )
            .border(
                // Um aro de um pixel separa a cápsula de páginas escuras, onde
                // sem ele ela desaparecia por completo.
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(25.dp),
            )
            .drawBehind {
                // Aro de progresso à volta da cápsula. Substitui a barra de
                // progresso: informa sem ocupar uma linha própria no ecrã.
                if (progresso > 0f && progresso < 1f) {
                    val espessura = 3.dp.toPx()
                    drawRoundRect(
                        color = corAcento.copy(alpha = 0.9f),
                        topLeft = Offset(espessura / 2, espessura / 2),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - espessura,
                            (size.height - espessura) * progresso,
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = espessura),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { aoTocar() }, onLongPress = { aoAbrirLeque() })
            }
            .pointerInput(naDireita) {
                var dx = 0f
                var dy = 0f
                // 44 dp separa um gesto de um toque com a mão a tremer. Abaixo
                // disso a cápsula disparava ações sozinha ao ser tocada.
                val limiar = 44.dp.toPx()
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDrag = { _, delta -> dx += delta.x; dy += delta.y },
                    onDragEnd = {
                        val paraDentro = if (naDireita) dx < -limiar else dx > limiar
                        val paraFora = if (naDireita) dx > limiar else dx < -limiar
                        when {
                            abs(dy) > abs(dx) && abs(dy) > limiar ->
                                if (dy < 0) aoSeguinte() else aoAnterior()
                            paraDentro -> aoAbrirLeque()
                            paraFora -> aoEsconder()
                        }
                    },
                )
            }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Um ponto. Toda a informação de segurança — tor, cifra, onion — cabe
        // na cor dele, e um ponto lê-se de relance sem ocupar espaço nem exigir
        // interpretação de um ícone.
        PontoDeEstado(corAcento, estadoTor)

        // O alvo maior fica em baixo, na parte da cápsula que o polegar alcança
        // primeiro e com menos esforço.
        AlvoDeEndereco(corAcento, estadoTor is EstadoTor.Pronto, numeroSeparadores)
    }
}

@Composable
private fun PontoDeEstado(cor: Color, estadoTor: EstadoTor) {
    val descricao = when (estadoTor) {
        is EstadoTor.Pronto -> "Tor ligado"
        is EstadoTor.ALigar -> "Tor a ligar, ${estadoTor.progresso} por cento"
        is EstadoTor.Falhou -> "Tor falhou"
        EstadoTor.Desligado -> "Sem tor"
    }
    Box(
        Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(cor)
            .semantics { contentDescription = descricao }
    )
}

/**
 * O disco de baixo: toca-se para escrever um endereço. O número de separadores
 * vai escrito lá dentro, para não precisar de um segundo alvo só para isso.
 */
@Composable
private fun AlvoDeEndereco(cor: Color, torLigado: Boolean, separadores: Int) {
    Box(contentAlignment = Alignment.Center) {
        if (torLigado) {
            // Halo suave: com o tor ligado a cápsula ganha um brilho, para o
            // estado se ver de relance sem ler nada.
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(cor.copy(alpha = 0.16f))
            )
        }
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (separadores > 99) "∞" else "$separadores",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
