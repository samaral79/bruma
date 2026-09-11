package pt.shrek.bruma.ui.comum

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import pt.shrek.bruma.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.shrek.bruma.navegador.Separador
import pt.shrek.bruma.ui.tema.CoresEstado

/**
 * A tira de separadores, ao lado da cápsula.
 *
 * O Sleipnir mantém os separadores sempre à vista e ao alcance do polegar, e a
 * troca faz-se com um gesto. Aqui a troca já se fazia — arrastar a cápsula para
 * cima ou para baixo — mas às cegas: não havia forma de saber para onde se tinha
 * ido nem quantos faltavam. Uma lista que aparece durante a troca e se apaga
 * sozinha dá essa noção sem roubar espaço permanente à página.
 *
 * Mostra o **título**, não uma miniatura: gerar miniaturas obriga a manter as
 * sessões a compor em segundo plano, o que num telemóvel com o Gecko custa
 * memória a sério. Um título lê-se melhor a correr, de qualquer forma.
 */
@Composable
fun TiraSeparadores(
    visivel: Boolean,
    separadores: List<Separador>,
    indiceAtivo: Int,
    naDireita: Boolean,
    aoEscolher: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Com um separador só não há troca possível; a tira seria ruído.
    if (separadores.size < 2) return

    AnimatedVisibility(
        visible = visivel,
        enter = slideInHorizontally { if (naDireita) it / 2 else -it / 2 } + fadeIn(),
        exit = slideOutHorizontally { if (naDireita) it / 2 else -it / 2 } + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = if (naDireita) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            separadores.forEachIndexed { indice, separador ->
                Cartao(
                    separador = separador,
                    ativo = indice == indiceAtivo,
                    naDireita = naDireita,
                    aoCarregar = { aoEscolher(indice) },
                )
            }
        }
    }
}

@Composable
private fun Cartao(
    separador: Separador,
    ativo: Boolean,
    naDireita: Boolean,
    aoCarregar: () -> Unit,
) {
    val fundo =
        if (ativo) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    val frente =
        if (ativo) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        Modifier
            .width(186.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(fundo)
            .clickable { aoCarregar() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Uma barra fina no lado de dentro marca o separador atual sem precisar
        // de mudar a cor do cartão inteiro.
        if (ativo && !naDireita) BarraAtiva()

        if (separador.privado) {
            Icon(
                Icons.Default.VisibilityOff,
                contentDescription = stringResource(R.string.acao_privado),
                tint = CoresEstado.tor,
                modifier = Modifier.size(13.dp).padding(end = 1.dp),
            )
        }

        Text(
            separador.etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = frente,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (naDireita) TextAlign.End else TextAlign.Start,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )

        if (ativo && naDireita) BarraAtiva()
    }
}

@Composable
private fun BarraAtiva() {
    Box(
        Modifier
            .size(width = 3.dp, height = 15.dp)
            .clip(CircleShape)
            .background(CoresEstado.tor)
    )
}
