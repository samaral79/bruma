package pt.shrek.bruma.ui.ecrans

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import pt.shrek.bruma.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import pt.shrek.bruma.ui.tema.CoresEstado

/** Que tipos de autenticação o aparelho aceita. */
private const val AUTENTICADORES =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

/** Se o aparelho tem sequer forma de autenticar o dono. */
fun podeAutenticar(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(AUTENTICADORES) ==
        BiometricManager.BIOMETRIC_SUCCESS

/**
 * O ecrã que tapa a app até o dono se identificar.
 *
 * Não é um diálogo por cima do navegador: é um ecrã que **substitui** o
 * conteúdo. Um diálogo deixaria a página desenhada por trás dele e visível na
 * lista de apps recentes, que é exatamente o que esta definição existe para
 * evitar.
 *
 * O código do aparelho conta como alternativa à impressão digital de propósito —
 * sem isso, um telemóvel cujo leitor deixou de funcionar trancava o dono fora
 * da própria app.
 */
@Composable
fun EcraBloqueio(aoDesbloquear: () -> Unit) {
    val contexto = LocalContext.current
    val atividade = contexto as? FragmentActivity

    fun pedirIdentificacao() {
        atividade ?: return
        val pedido = BiometricPrompt.PromptInfo.Builder()
            .setTitle(contexto.getString(R.string.bloqueio_titulo))
            .setSubtitle(contexto.getString(R.string.bloqueio_subtitulo))
            .setAllowedAuthenticators(AUTENTICADORES)
            .build()

        BiometricPrompt(
            atividade,
            ContextCompat.getMainExecutor(contexto),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    aoDesbloquear()
                }
            },
        ).authenticate(pedido)
    }

    // Pede logo ao aparecer: obrigar a um toque extra antes do sensor não
    // acrescenta segurança nenhuma, só atrito.
    LaunchedEffect(Unit) { pedirIdentificacao() }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0C0E), Color(0xFF12121A), Color(0xFF0B0C0E))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(42.dp))
                    .background(CoresEstado.tor.copy(alpha = 0.14f))
                    .clickable { pedirIdentificacao() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = stringResource(R.string.bloqueio_identificar),
                    tint = CoresEstado.tor,
                    modifier = Modifier.size(34.dp),
                )
            }
            Text(
                "bruma",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 26.dp),
            )
            Text(
                stringResource(R.string.bloqueio_toca),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
