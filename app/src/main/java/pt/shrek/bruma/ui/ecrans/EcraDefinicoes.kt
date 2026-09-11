package pt.shrek.bruma.ui.ecrans

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import pt.shrek.bruma.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pt.shrek.bruma.core.Definicoes
import pt.shrek.bruma.core.Fundo
import pt.shrek.bruma.core.Idioma
import pt.shrek.bruma.core.Idiomas
import pt.shrek.bruma.core.Mao
import pt.shrek.bruma.core.MotorBusca

/**
 * As definições, em ecrã inteiro e não numa folha inferior.
 *
 * Numa folha, a lista ficava presa a metade do ecrã e a rolar dentro de uma
 * caixa que já rolava — dois deslocamentos aninhados, que no telemóvel é um dos
 * gestos mais frustrantes que há. Em ecrã inteiro há um só deslocamento, e a
 * seta de voltar do telemóvel faz o que qualquer pessoa espera: sair.
 */
@Composable
fun EcraDefinicoes(
    definicoes: Definicoes,
    aoMudar: () -> Unit,
    aoSair: () -> Unit,
) {
    BackHandler { aoSair() }

    val contexto = LocalContext.current
    var temFundo by remember { mutableStateOf(Fundo.existe(contexto)) }

    // O seletor de fotografias do sistema não pede permissão de armazenamento:
    // a app só recebe a imagem escolhida, e nunca vê o resto da galeria.
    val seletorDeFundo = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && Fundo.guardar(contexto, uri)) {
            temFundo = true
            aoMudar()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Cabecalho(aoSair)

        LazyColumn(
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item { Seccao(stringResource(R.string.def_seccao_aspeto)) }
            item {
                Escolha(
                    stringResource(R.string.def_fundo),
                    stringResource(if (temFundo) R.string.def_fundo_imagem else R.string.def_fundo_nevoa),
                ) {
                    if (temFundo) {
                        // Segundo toque devolve o fundo desenhado: sem isto não
                        // havia forma de desfazer a escolha de uma imagem.
                        Fundo.apagar(contexto); temFundo = false; aoMudar()
                    } else {
                        seletorDeFundo.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            }
            item {
                Escolha(
                    stringResource(R.string.def_mao),
                    stringResource(if (definicoes.mao == Mao.DIREITA) R.string.def_mao_direita else R.string.def_mao_esquerda),
                ) {
                    definicoes.mao = if (definicoes.mao == Mao.DIREITA) Mao.ESQUERDA else Mao.DIREITA
                    aoMudar()
                }
            }

            item {
                // Mudar de idioma obriga a recriar a atividade: os recursos são
                // presos à configuração com que ela nasceu.
                Escolha(
                    stringResource(R.string.def_idioma),
                    (Idiomas.escolhido(contexto) ?: Idiomas.sugestaoDoSistema(contexto)).etiqueta,
                ) {
                    val todos = Idioma.entries
                    val atual = Idiomas.escolhido(contexto) ?: Idiomas.sugestaoDoSistema(contexto)
                    Idiomas.guardar(contexto, todos[(todos.indexOf(atual) + 1) % todos.size])
                    (contexto as? android.app.Activity)?.recreate()
                }
            }
            item {
                Escolha(stringResource(R.string.def_rever_guia), "") {
                    definicoes.guiaVisto = false
                    aoSair()
                }
            }

            item { Seccao(stringResource(R.string.def_seccao_busca)) }
            item {
                Escolha(stringResource(R.string.def_motor), definicoes.motorBusca.etiqueta) {
                    val todos = MotorBusca.entries
                    definicoes.motorBusca = todos[(todos.indexOf(definicoes.motorBusca) + 1) % todos.size]
                    aoMudar()
                }
            }
            item {
                Interruptor(
                    stringResource(R.string.def_onion),
                    stringResource(R.string.def_onion_detalhe),
                    definicoes.preferirOnionComTor,
                ) { definicoes.preferirOnionComTor = it; aoMudar() }
            }

            item { Seccao(stringResource(R.string.def_seccao_tor)) }
            item {
                Interruptor(
                    stringResource(R.string.def_tor_arranque),
                    stringResource(R.string.def_tor_arranque_detalhe),
                    definicoes.torAoArrancar,
                ) {
                    definicoes.torAoArrancar = it; aoMudar()
                }
            }

            item { Seccao(stringResource(R.string.def_seccao_privacidade)) }
            item {
                Interruptor(
                    stringResource(R.string.def_https),
                    stringResource(R.string.def_https_detalhe),
                    definicoes.apenasHttps,
                ) { definicoes.apenasHttps = it; aoMudar() }
            }
            item {
                Interruptor(stringResource(R.string.def_javascript), null, definicoes.javascript) {
                    definicoes.javascript = it; aoMudar()
                }
            }
            item {
                Interruptor(
                    stringResource(R.string.def_cookies),
                    stringResource(R.string.def_cookies_detalhe),
                    definicoes.isolarCookies,
                ) { definicoes.isolarCookies = it; aoMudar() }
            }
            item {
                Interruptor(
                    stringResource(R.string.def_impressao),
                    stringResource(R.string.def_impressao_detalhe),
                    definicoes.resistirImpressaoDigital,
                ) { definicoes.resistirImpressaoDigital = it; aoMudar() }
            }
            item {
                Interruptor(stringResource(R.string.def_limpar_ao_sair), null, definicoes.limparAoSair) {
                    definicoes.limparAoSair = it; aoMudar()
                }
            }

            item { Seccao(stringResource(R.string.def_seccao_acesso)) }
            item {
                Interruptor(
                    stringResource(R.string.def_ecra_seguro),
                    stringResource(R.string.def_ecra_seguro_detalhe),
                    definicoes.ecraSeguro,
                ) { definicoes.ecraSeguro = it; aoMudar() }
            }
            item {
                val temSensor = podeAutenticar(contexto)
                Interruptor(
                    stringResource(R.string.def_biometria),
                    stringResource(
                        if (temSensor) R.string.def_biometria_detalhe
                        else R.string.def_biometria_indisponivel
                    ),
                    definicoes.pedirBiometria && temSensor,
                    ativo = temSensor,
                ) { definicoes.pedirBiometria = it; aoMudar() }
            }
        }
    }
}

@Composable
private fun Cabecalho(aoSair: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { aoSair() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.def_voltar),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            stringResource(R.string.def_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
}

@Composable
private fun Seccao(titulo: String) {
    Text(
        titulo.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 26.dp, bottom = 6.dp),
    )
}

@Composable
private fun Interruptor(
    titulo: String,
    detalhe: String?,
    valor: Boolean,
    ativo: Boolean = true,
    aoMudar: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = ativo) { aoMudar(!valor) }
            .padding(horizontal = 24.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = 12.dp).weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = if (ativo) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
            )
            if (detalhe != null) {
                Text(
                    detalhe,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Switch(checked = valor, onCheckedChange = aoMudar, enabled = ativo)
    }
}

@Composable
private fun Escolha(titulo: String, valor: String, aoCarregar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { aoCarregar() }
            .padding(horizontal = 24.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge)
        Text(valor, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
