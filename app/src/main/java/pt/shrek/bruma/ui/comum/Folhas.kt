package pt.shrek.bruma.ui.comum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import pt.shrek.bruma.core.Definicoes
import pt.shrek.bruma.core.Fundo
import pt.shrek.bruma.core.MotorBusca
import pt.shrek.bruma.navegador.EstadoUblock
import pt.shrek.bruma.navegador.Separador
import pt.shrek.bruma.tor.EstadoTor
import pt.shrek.bruma.ui.tema.CoresEstado

/**
 * O painel de ações.
 *
 * Os botões são quadrados grandes numa grelha de três, e não uma lista de texto,
 * porque uma lista obriga a apontar com precisão e uma grelha de alvos de 100 dp
 * não obriga. A ordem não é arbitrária: o que mais se usa fica na fila de baixo,
 * que é a mais perto do polegar.
 */
/** A caixa de endereço, ancorada em baixo, por cima do teclado. */
@Composable
fun FolhaEndereco(
    urlAtual: String,
    motor: MotorBusca,
    aoConfirmar: (String) -> Unit,
) {
    // `about:blank` é um detalhe interno do motor, não uma morada que alguém
    // queira ver ou editar: num separador novo a caixa abre vazia.
    val inicial = urlAtual.takeUnless { it.isBlank() || it == "about:blank" }.orEmpty()

    // O texto começa todo selecionado. Sem isto, escrever num separador que já
    // tem página juntava o novo endereço ao antigo em vez de o substituir — foi
    // exatamente assim que nasceu um `example.comabout:blank`.
    var campo by remember {
        mutableStateOf(TextFieldValue(inicial, selection = TextRange(0, inicial.length)))
    }
    val foco = remember { FocusRequester() }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = campo,
            onValueChange = { campo = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(foco),
            placeholder = { Text("Endereço ou pesquisa") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { aoConfirmar(campo.text) }),
        )
        Text(
            "Procura com ${motor.etiqueta}",
            style = MaterialTheme.typography.labelSmall,
            color = if (motor.exigeTor) CoresEstado.tor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { foco.requestFocus() }
}

/** A lista de separadores: lista vertical, não grelha — percorre-se com o polegar. */
@Composable
fun FolhaSeparadores(
    separadores: List<Separador>,
    indiceAtivo: Int,
    aoEscolher: (Int) -> Unit,
    aoFechar: (Separador) -> Unit,
    aoNovo: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
    ) {
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(separadores, key = { it.id }) { separador ->
                val indice = separadores.indexOf(separador)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { aoEscolher(indice) }
                        .background(
                            if (indice == indiceAtivo) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (separador.privado) {
                        Icon(
                            Icons.Default.VisibilityOff, contentDescription = "Privado",
                            tint = CoresEstado.tor,
                            modifier = Modifier.size(16.dp).padding(end = 2.dp),
                        )
                    }
                    Text(
                        separador.etiqueta,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { aoFechar(separador) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Close, contentDescription = "Fechar separador",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { aoNovo() }
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "Novo separador",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** Definições: uma lista de interruptores, tudo à distância do polegar. */
@Composable
fun FolhaDefinicoes(definicoes: Definicoes, aoMudar: () -> Unit) {
    var versao by remember { mutableStateOf(0) }
    fun mudou() { versao++; aoMudar() }

    val contexto = androidx.compose.ui.platform.LocalContext.current
    var temFundo by remember { mutableStateOf(Fundo.existe(contexto)) }

    // O seletor de fotografias do sistema não pede permissão de armazenamento:
    // a app só recebe a imagem escolhida, e nunca vê o resto da galeria.
    val seletorDeFundo = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && Fundo.guardar(contexto, uri)) {
            temFundo = true
            mudou()
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        Text(
            "Definições",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp, top = 4.dp),
        )

        LazyColumn(Modifier.heightIn(max = 460.dp)) {
            item {
                Escolha(
                    "Fundo do início",
                    if (temFundo) "Imagem escolhida" else "Névoa",
                ) {
                    if (temFundo) {
                        // Segundo toque devolve o fundo desenhado: sem isto não
                        // havia forma de desfazer a escolha de uma imagem.
                        Fundo.apagar(contexto)
                        temFundo = false
                        mudou()
                    } else {
                        seletorDeFundo.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            }
            item {
                Escolha("Motor de busca", definicoes.motorBusca.etiqueta) {
                    val todos = MotorBusca.entries
                    val seguinte = todos[(todos.indexOf(definicoes.motorBusca) + 1) % todos.size]
                    definicoes.motorBusca = seguinte
                    mudou()
                }
            }
            item {
                Interruptor(
                    "Preferir .onion com tor",
                    "Com o tor ligado, a busca vai pelo DuckDuckGo onion e nunca sai da rede tor",
                    definicoes.preferirOnionComTor,
                ) { definicoes.preferirOnionComTor = it; mudou() }
            }
            item {
                Interruptor(
                    "Ligar o tor ao arrancar", null, definicoes.torAoArrancar,
                ) { definicoes.torAoArrancar = it; mudou() }
            }
            item {
                Interruptor(
                    "Só HTTPS",
                    "Recusa ligações em claro. Os .onion continuam a funcionar — já são cifrados pelo endereço",
                    definicoes.apenasHttps,
                ) { definicoes.apenasHttps = it; mudou() }
            }
            item {
                Interruptor("JavaScript", null, definicoes.javascript) {
                    definicoes.javascript = it; mudou()
                }
            }
            item {
                Interruptor(
                    "Resistir à impressão digital",
                    "Uniformiza janela, fuso e tipos de letra. Protege muito e parte alguns sítios",
                    definicoes.resistirImpressaoDigital,
                ) { definicoes.resistirImpressaoDigital = it; mudou() }
            }
            item {
                Interruptor(
                    "Limpar tudo ao sair", null, definicoes.limparAoSair,
                ) { definicoes.limparAoSair = it; mudou() }
            }
            item {
                Interruptor(
                    "Ecrã seguro",
                    "Tira a app das capturas de ecrã e da lista de apps recentes",
                    definicoes.ecraSeguro,
                ) { definicoes.ecraSeguro = it; mudou() }
            }
            item {
                Escolha("Mão", if (definicoes.mao == pt.shrek.bruma.core.Mao.DIREITA) "Direita" else "Esquerda") {
                    definicoes.mao =
                        if (definicoes.mao == pt.shrek.bruma.core.Mao.DIREITA) pt.shrek.bruma.core.Mao.ESQUERDA
                        else pt.shrek.bruma.core.Mao.DIREITA
                    mudou()
                }
            }
        }
    }
}

@Composable
private fun Interruptor(titulo: String, detalhe: String?, valor: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { aoMudar(!valor) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (detalhe != null) {
                Text(
                    detalhe,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Switch(checked = valor, onCheckedChange = aoMudar, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun Escolha(titulo: String, valor: String, aoCarregar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { aoCarregar() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * O que não cabe no leque: ações raras ou destrutivas.
 *
 * Estão aqui de propósito, a um toque de distância a mais — "limpar tudo" e
 * "nova identidade" não são coisas para ficarem debaixo do polegar num arco que
 * se abre por engano.
 */
@Composable
fun FolhaMais(
    estadoTor: EstadoTor,
    estadoUblock: EstadoUblock,
    aoNovaIdentidade: () -> Unit,
    aoLimpar: () -> Unit,
    aoDefinicoes: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Text(
            text = when (estadoUblock) {
                is EstadoUblock.Instalado -> "uBlock Origin ${estadoUblock.versao}"
                is EstadoUblock.Falhou -> "uBlock Origin em falta"
                else -> "uBlock Origin a instalar…"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (estadoUblock is EstadoUblock.Falhou) CoresEstado.perigo
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 10.dp),
        )

        if (estadoTor is EstadoTor.Pronto) {
            Linha(Icons.Default.Autorenew, "Nova identidade", "Circuitos novos para todos os sítios", aoNovaIdentidade)
        }
        Linha(Icons.Default.Tune, "Definições", null, aoDefinicoes)
        Linha(Icons.Default.DeleteSweep, "Limpar tudo", "Cookies, cache, sessões e separadores", aoLimpar, perigo = true)
    }
}

@Composable
private fun Linha(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalhe: String?,
    aoCarregar: () -> Unit,
    perigo: Boolean = false,
) {
    val cor = if (perigo) CoresEstado.perigo else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { aoCarregar() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        Column(Modifier.padding(start = 16.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, color = cor)
            if (detalhe != null) {
                Text(
                    detalhe,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
