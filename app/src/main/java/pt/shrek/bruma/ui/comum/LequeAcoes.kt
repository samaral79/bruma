package pt.shrek.bruma.ui.comum

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import pt.shrek.bruma.core.Mao
import pt.shrek.bruma.tor.EstadoTor
import pt.shrek.bruma.ui.tema.CoresEstado
import kotlin.math.roundToInt

private class Acao(
    val icone: ImageVector,
    val etiqueta: String,
    val aoCarregar: () -> Unit,
    val destaque: Boolean = false,
)

/**
 * As ações, numa coluna que sobe da cápsula ao longo da mesma aresta.
 *
 * A primeira versão disto era um arco, pela ideia de que o polegar descreve uma
 * circunferência. A ideia é correta, o desenho não era: com seis alvos de 54 dp
 * a 132 dp de raio, os botões sobrepunham-se uns aos outros e as etiquetas
 * chocavam. Num ecrã de telemóvel não há raio que dê para seis alvos legíveis
 * num quadrante e ao mesmo tempo caibam no alcance do polegar.
 *
 * A coluna resolve as duas coisas: fica sobre a aresta onde o polegar já está
 * pousado, o espaçamento é previsível, e lê-se de cima a baixo sem esforço. O
 * que mais se usa fica **em baixo**, junto à cápsula — é onde a mão já está.
 */
@Composable
fun LequeAcoes(
    visivel: Boolean,
    mao: Mao,
    estadoTor: EstadoTor,
    aoFechar: () -> Unit,
    aoNovoSeparador: () -> Unit,
    aoNovoPrivado: () -> Unit,
    aoSeparadores: () -> Unit,
    aoRecarregar: () -> Unit,
    aoAlternarTor: () -> Unit,
    aoInicio: () -> Unit,
    aoMais: () -> Unit,
) {
    val abertura by animateFloatAsState(
        targetValue = if (visivel) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "abertura",
    )
    if (abertura < 0.01f) return

    val naDireita = mao == Mao.DIREITA

    // De cima para baixo na coluna; o último da lista é o que fica mais perto
    // da cápsula, e por isso o mais fácil de alcançar.
    val acoes = listOf(
        Acao(Icons.Default.MoreHoriz, "Mais", aoMais),
        Acao(Icons.Default.VisibilityOff, "Privado", aoNovoPrivado),
        Acao(Icons.Default.Refresh, "Recarregar", aoRecarregar),
        Acao(Icons.Default.Home, "Início", aoInicio),
        Acao(Icons.Default.Layers, "Separadores", aoSeparadores),
        Acao(Icons.Default.Add, "Novo separador", aoNovoSeparador),
        Acao(
            Icons.Default.Shield,
            if (estadoTor is EstadoTor.Pronto) "Desligar tor" else "Ligar tor",
            aoAlternarTor,
            destaque = true,
        ),
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f * abertura))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { aoFechar() }
    ) {
        Column(
            modifier = Modifier
                .align(BiasAlignment(if (naDireita) 1f else -1f, 0.62f))
                .padding(horizontal = 14.dp),
            horizontalAlignment = if (naDireita) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            acoes.forEachIndexed { indice, acao ->
                // Os de baixo entram primeiro: a coluna desdobra-se a partir da
                // cápsula, no sentido em que o polegar a vai percorrer.
                val ordem = acoes.size - 1 - indice
                val atraso = ordem * 0.07f
                val progresso = ((abertura - atraso) / (1f - atraso)).coerceIn(0f, 1f)

                ItemAcao(
                    acao = acao,
                    naDireita = naDireita,
                    progresso = progresso,
                    aoCarregar = { acao.aoCarregar(); aoFechar() },
                )
            }
        }
    }
}

@Composable
private fun ItemAcao(
    acao: Acao,
    naDireita: Boolean,
    progresso: Float,
    aoCarregar: () -> Unit,
) {
    val fundo = if (acao.destaque) CoresEstado.tor else MaterialTheme.colorScheme.surface
    val frente = if (acao.destaque) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val tamanho = if (acao.destaque) 56.dp else 48.dp

    val disco = @Composable {
        Box(
            Modifier
                .size(tamanho)
                .clip(CircleShape)
                .background(fundo),
            contentAlignment = Alignment.Center,
        ) {
            Icon(acao.icone, contentDescription = acao.etiqueta, tint = frente, modifier = Modifier.size(21.dp))
        }
    }

    val etiqueta = @Composable {
        Text(
            acao.etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.9f),
        )
    }

    Row(
        modifier = Modifier
            .alpha(progresso)
            // A linha toda é o alvo, não só o disco. Com a etiqueta ao lado, um
            // alvo que parece tocável e não é gera toques que não fazem nada —
            // e a pessoa conclui que a app está encravada, não que falhou o
            // círculo por vinte pixels.
            .clip(RoundedCornerShape(30.dp))
            .clickable { aoCarregar() }
            .padding(start = 12.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
            // Entram a deslizar do lado da aresta, como se saíssem de dentro da
            // cápsula em vez de aparecerem do nada.
            .offset {
                val desvio = ((1f - progresso) * 40.dp.toPx()).roundToInt()
                IntOffset(if (naDireita) desvio else -desvio, 0)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // A etiqueta fica do lado de dentro do ecrã, nunca debaixo da mão.
        if (naDireita) { etiqueta(); disco() } else { disco(); etiqueta() }
    }
}
