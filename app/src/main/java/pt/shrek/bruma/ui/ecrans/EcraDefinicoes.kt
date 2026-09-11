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
            item { Seccao("Aspeto") }
            item {
                Escolha("Fundo do início", if (temFundo) "Imagem escolhida" else "Névoa") {
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
                Escolha("Mão", if (definicoes.mao == Mao.DIREITA) "Direita" else "Esquerda") {
                    definicoes.mao = if (definicoes.mao == Mao.DIREITA) Mao.ESQUERDA else Mao.DIREITA
                    aoMudar()
                }
            }

            item {
                Escolha("Ver o guia outra vez", "") {
                    definicoes.guiaVisto = false
                    aoSair()
                }
            }

            item { Seccao("Busca") }
            item {
                Escolha("Motor de busca", definicoes.motorBusca.etiqueta) {
                    val todos = MotorBusca.entries
                    definicoes.motorBusca = todos[(todos.indexOf(definicoes.motorBusca) + 1) % todos.size]
                    aoMudar()
                }
            }
            item {
                Interruptor(
                    "Preferir .onion com tor",
                    "Com o tor ligado, a busca vai pelo DuckDuckGo onion e nunca sai da rede tor",
                    definicoes.preferirOnionComTor,
                ) { definicoes.preferirOnionComTor = it; aoMudar() }
            }

            item { Seccao("Tor") }
            item {
                Interruptor(
                    "Ligar o tor ao arrancar",
                    "Sem isto, o tor fica como o deixaste da última vez",
                    definicoes.torAoArrancar,
                ) {
                    definicoes.torAoArrancar = it; aoMudar()
                }
            }

            item { Seccao("Privacidade") }
            item {
                Interruptor(
                    "Só HTTPS",
                    "Recusa ligações em claro. Os .onion continuam a funcionar — já são cifrados pelo endereço",
                    definicoes.apenasHttps,
                ) { definicoes.apenasHttps = it; aoMudar() }
            }
            item {
                Interruptor("JavaScript", null, definicoes.javascript) {
                    definicoes.javascript = it; aoMudar()
                }
            }
            item {
                Interruptor(
                    "Isolar cookies por sítio",
                    "Um cookie posto pelo mesmo rastreador em dois sítios deixa de os poder ligar",
                    definicoes.isolarCookies,
                ) { definicoes.isolarCookies = it; aoMudar() }
            }
            item {
                Interruptor(
                    "Resistir à impressão digital",
                    "Uniformiza janela, fuso e tipos de letra. Protege muito e parte alguns sítios",
                    definicoes.resistirImpressaoDigital,
                ) { definicoes.resistirImpressaoDigital = it; aoMudar() }
            }
            item {
                Interruptor("Limpar tudo ao sair", null, definicoes.limparAoSair) {
                    definicoes.limparAoSair = it; aoMudar()
                }
            }

            item { Seccao("Acesso à app") }
            item {
                Interruptor(
                    "Ecrã seguro",
                    "Tira a app das capturas de ecrã e da lista de apps recentes",
                    definicoes.ecraSeguro,
                ) { definicoes.ecraSeguro = it; aoMudar() }
            }
            item {
                val temSensor = podeAutenticar(contexto)
                Interruptor(
                    "Pedir identificação para abrir",
                    if (temSensor) "Impressão digital, rosto ou código do telemóvel"
                    else "Indisponível: este telemóvel não tem bloqueio de ecrã configurado",
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
                contentDescription = "Voltar",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            "Definições",
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
