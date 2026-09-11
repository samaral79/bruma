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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
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
import pt.shrek.bruma.ui.ecrans.podeAutenticar
import pt.shrek.bruma.ui.tema.CoresEstado

/**
 * O painel de ações.
 *
 * Os botões são quadrados grandes numa grelha de três, e não uma lista de texto,
 * porque uma lista obriga a apontar com precisão e uma grelha de alvos de 100 dp
 * não obriga. A ordem não é arbitrária: o que mais se usa fica na fila de baixo,
 * que é a mais perto do polegar.
 */
/**
 * A caixa de endereço, ancorada em baixo e por cima do teclado.
 *
 * É o alvo mais usado da app e por isso o maior: 72 dp de altura e texto de
 * corpo grande. Um endereço escreve-se com o telemóvel numa mão, muitas vezes em
 * movimento, e a caixa normal do Material — 56 dp — é difícil de acertar à
 * primeira nessas condições.
 *
 * As definições abrem-se daqui. É onde a mão já está quando se quer mexer na
 * app, e poupa o caminho cápsula → coluna → Mais → Definições.
 */
@Composable
fun FolhaEndereco(
    urlAtual: String,
    motor: MotorBusca,
    aoConfirmar: (String) -> Unit,
    aoAbrirDefinicoes: () -> Unit,
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
            .padding(horizontal = 14.dp)
            .padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = campo,
            onValueChange = { campo = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .focusRequester(foco),
            placeholder = {
                Text(stringResource(R.string.endereco_sugestao), style = MaterialTheme.typography.bodyLarge)
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { aoConfirmar(campo.text) }),
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.endereco_procura_com, motor.etiqueta),
                style = MaterialTheme.typography.labelSmall,
                color = if (motor.exigeTor) CoresEstado.tor
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { aoAbrirDefinicoes() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    stringResource(R.string.mais_definicoes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
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
                            Icons.Default.VisibilityOff, contentDescription = stringResource(R.string.acao_privado),
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
                            Icons.Default.Close, contentDescription = stringResource(R.string.acao_fechar_separador),
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
                stringResource(R.string.acao_novo_separador),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
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
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Switch(
            checked = valor,
            onCheckedChange = aoMudar,
            enabled = ativo,
            modifier = Modifier.padding(start = 12.dp),
        )
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
    // "Limpar tudo" fecha separadores e apaga sessões, e estava a um toque de
    // distância, logo por baixo de "Definições". Ao testar a app acertei nele
    // por engano à primeira tentativa — o que é prova bastante de que precisa de
    // confirmação.
    var aConfirmarLimpeza by remember { mutableStateOf(false) }

    if (aConfirmarLimpeza) {
        AlertDialog(
            onDismissRequest = { aConfirmarLimpeza = false },
            title = { Text(stringResource(R.string.limpar_titulo)) },
            text = { Text(stringResource(R.string.limpar_texto)) },
            confirmButton = {
                TextButton(onClick = { aConfirmarLimpeza = false; aoLimpar() }) {
                    Text(stringResource(R.string.limpar_confirmar), color = CoresEstado.perigo)
                }
            },
            dismissButton = {
                TextButton(onClick = { aConfirmarLimpeza = false }) { Text(stringResource(R.string.cancelar)) }
            },
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Text(
            text = when (estadoUblock) {
                is EstadoUblock.Instalado -> stringResource(R.string.ublock_instalado, estadoUblock.versao)
                is EstadoUblock.Falhou -> stringResource(R.string.ublock_em_falta)
                else -> stringResource(R.string.ublock_a_instalar)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (estadoUblock is EstadoUblock.Falhou) CoresEstado.perigo
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 10.dp),
        )

        if (estadoTor is EstadoTor.Pronto) {
            Linha(
                Icons.Default.Autorenew,
                stringResource(R.string.mais_nova_identidade),
                stringResource(R.string.mais_nova_identidade_detalhe),
                aoNovaIdentidade,
            )
        }
        Linha(Icons.Default.Tune, stringResource(R.string.mais_definicoes), null, aoDefinicoes)
        Linha(
            Icons.Default.DeleteSweep,
            stringResource(R.string.mais_limpar),
            stringResource(R.string.mais_limpar_detalhe),
            { aConfirmarLimpeza = true }, perigo = true,
        )
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
