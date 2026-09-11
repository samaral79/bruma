package pt.shrek.bruma.ui.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import pt.shrek.bruma.core.Mao
import pt.shrek.bruma.tor.EstadoTor
import pt.shrek.bruma.ui.Folha
import pt.shrek.bruma.ui.NavegadorViewModel
import pt.shrek.bruma.ui.comum.FolhaEndereco
import pt.shrek.bruma.ui.comum.FolhaMais
import pt.shrek.bruma.ui.comum.FolhaSeparadores
import pt.shrek.bruma.ui.comum.LequeAcoes
import pt.shrek.bruma.ui.comum.RailLateral
import pt.shrek.bruma.ui.comum.TiraSeparadores
import pt.shrek.bruma.ui.comum.VistaGecko

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcraNavegador(vm: NavegadorViewModel) {

    val estadoTor by vm.estadoTor.collectAsStateWithLifecycle()
    val ativo = vm.separadores.ativo
    val avisos = remember { SnackbarHostState() }
    val mao = vm.definicoes.mao
    val naDireita = mao == Mao.DIREITA

    LaunchedEffect(Unit) { vm.avisos.collect { avisos.showSnackbar(it) } }

    // A tira aparece quando se troca de separador e apaga-se sozinha. Uma lista
    // permanente roubaria espaço à página; uma que não aparecesse de todo
    // deixaria a troca às cegas.
    var tiraVisivel by remember { mutableStateOf(false) }
    LaunchedEffect(vm.separadores.indiceAtivo, vm.separadores.separadores.size) {
        tiraVisivel = true
        delay(1800)
        tiraVisivel = false
    }

    // A seta de voltar do telemóvel desfaz uma camada de cada vez, da mais
    // recente para a mais antiga: primeiro o que está por cima da página, depois
    // o histórico da própria página, e só quando não há para onde recuar é que
    // sai da app. Recuar na página vem **antes** de repor a cápsula escondida:
    // quem escondeu a cápsula fê-lo para ler, e nesse estado esperar voltar à
    // página anterior é o mais natural.
    val podeRecuar = ativo?.podeVoltar == true
    BackHandler(
        enabled = vm.folha != Folha.NENHUMA || vm.lequeAberto || podeRecuar || !vm.capsulaVisivel
    ) {
        when {
            vm.lequeAberto -> vm.lequeAberto = false
            vm.folha != Folha.NENHUMA -> vm.folha = Folha.NENHUMA
            podeRecuar -> vm.voltar()
            !vm.capsulaVisivel -> vm.capsulaVisivel = true
        }
    }

    if (vm.ecraDefinicoes) {
        EcraDefinicoes(
            definicoes = vm.definicoes,
            aoMudar = { vm.aplicarDefinicoesAoMotor() },
            aoSair = { vm.ecraDefinicoes = false },
        )
        return
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // A página ocupa o ecrã todo; a cápsula flutua por cima. Sem barra a
        // roubar altura, o conteúdo ganha o espaço inteiro.
        // Um separador sem página mostra o início, não um Gecko em branco.
        val semPagina = ativo == null || ativo.url.isBlank() || ativo.url == "about:blank"
        if (semPagina) {
            EcraInicio(
                motor = vm.definicoes.motorEfetivo(estadoTor is EstadoTor.Pronto),
                naDireita = naDireita,
                aoTocarNaBusca = { vm.folha = Folha.ENDERECO },
            )
        } else {
            VistaGecko(
                separador = ativo!!,
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
            )
        }

        AnimatedVisibility(
            visible = vm.capsulaVisivel && !vm.lequeAberto,
            enter = slideInHorizontally { if (naDireita) it else -it } + fadeIn(),
            exit = slideOutHorizontally { if (naDireita) it else -it } + fadeOut(),
            // Alinhamento com viés: encostada ao lado da mão e a 62% da altura,
            // que é onde o polegar assenta com o telemóvel seguro pela base.
            modifier = Modifier.align(BiasAlignment(if (naDireita) 1f else -1f, 0.62f)),
        ) {
            Box(Modifier.padding(horizontal = 8.dp).navigationBarsPadding()) {
                RailLateral(
                    separador = ativo,
                    estadoTor = estadoTor,
                    numeroSeparadores = vm.separadores.separadores.size,
                    mao = mao,
                    aoTocar = { vm.folha = Folha.ENDERECO },
                    aoAbrirLeque = { vm.lequeAberto = true },
                    aoEsconder = { vm.capsulaVisivel = false },
                    aoSeguinte = { vm.separadores.seguinte() },
                    aoAnterior = { vm.separadores.anterior() },
                )
            }
        }

        // Com a cápsula escondida fica uma pega discreta para a trazer de volta,
        // senão o gesto de esconder seria irreversível sem o botão do sistema.
        if (!vm.capsulaVisivel) {
            Box(
                Modifier
                    .align(BiasAlignment(if (naDireita) 1f else -1f, 0.62f))
                    .padding(horizontal = 2.dp)
                    .size(width = 5.dp, height = 56.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outline)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { vm.capsulaVisivel = true }
            )
        }

        TiraSeparadores(
            visivel = tiraVisivel && !vm.lequeAberto && vm.capsulaVisivel,
            separadores = vm.separadores.separadores,
            indiceAtivo = vm.separadores.indiceAtivo,
            naDireita = naDireita,
            aoEscolher = { vm.separadores.irPara(it) },
            modifier = Modifier
                .align(BiasAlignment(if (naDireita) 1f else -1f, 0.62f))
                // Ao lado da cápsula, não por cima: a cápsula tem 50 dp de
                // largura mais 8 de margem.
                .padding(start = if (naDireita) 0.dp else 72.dp,
                         end = if (naDireita) 72.dp else 0.dp)
                .navigationBarsPadding(),
        )

        LequeAcoes(
            visivel = vm.lequeAberto,
            mao = mao,
            estadoTor = estadoTor,
            aoFechar = { vm.lequeAberto = false },
            aoNovoSeparador = { vm.novoSeparador() },
            aoNovoPrivado = { vm.novoSeparador(privado = true) },
            aoSeparadores = { vm.folha = Folha.SEPARADORES },
            aoRecarregar = { vm.recarregar() },
            aoAlternarTor = { vm.alternarTor() },
            aoInicio = { vm.irParaInicio() },
            aoMais = { vm.folha = Folha.MAIS },
        )

        SnackbarHost(
            hostState = avisos,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth(),
        )
    }

    if (vm.folha != Folha.NENHUMA) {
        val estadoFolha = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { vm.folha = Folha.NENHUMA },
            sheetState = estadoFolha,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            when (vm.folha) {
                Folha.ENDERECO -> FolhaEndereco(
                    urlAtual = ativo?.url.orEmpty(),
                    motor = vm.definicoes.motorEfetivo(estadoTor is EstadoTor.Pronto),
                    aoConfirmar = { vm.abrir(it) },
                    aoAbrirDefinicoes = { vm.abrirDefinicoes() },
                )

                Folha.SEPARADORES -> FolhaSeparadores(
                    separadores = vm.separadores.separadores,
                    indiceAtivo = vm.separadores.indiceAtivo,
                    aoEscolher = { vm.separadores.irPara(it); vm.fecharTudo() },
                    aoFechar = { vm.separadores.fechar(it) },
                    aoNovo = { vm.novoSeparador() },
                )

                Folha.MAIS -> FolhaMais(
                    estadoTor = estadoTor,
                    estadoUblock = vm.estadoUblock.collectAsStateWithLifecycle().value,
                    aoNovaIdentidade = { vm.novaIdentidade(); vm.fecharTudo() },
                    aoLimpar = { vm.limparTudo() },
                    aoDefinicoes = { vm.abrirDefinicoes() },
                )

                Folha.NENHUMA -> Unit
            }
        }
    }
}
