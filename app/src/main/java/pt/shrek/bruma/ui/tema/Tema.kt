package pt.shrek.bruma.ui.tema

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import pt.shrek.bruma.R

/**
 * Paleta quase monocromática com um único acento.
 *
 * A escolha é funcional, não decorativa: num navegador, a cor tem de significar
 * alguma coisa. Aqui só três coisas ganham cor — o tor ligado, um aviso e um
 * erro. Se os botões tivessem cor, o estado da ligação deixava de saltar à
 * vista, que é a informação que mais importa num navegador destes.
 */
private val Violeta = Color(0xFF7C6BF0)
private val VioletaClaro = Color(0xFF5A4BD0)
private val Verde = Color(0xFF3FB950)
private val Vermelho = Color(0xFFE5534B)
private val Ambar = Color(0xFFD29922)

private val Escuro = darkColorScheme(
    primary = Violeta,
    onPrimary = Color(0xFF0B0C0E),
    secondary = Color(0xFF8A9099),
    background = Color(0xFF0B0C0E),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF16181C),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF23262B),
    onSurfaceVariant = Color(0xFF9AA1AA),
    outline = Color(0xFF31353B),
    error = Vermelho,
    onError = Color(0xFF0B0C0E),
)

private val Claro = lightColorScheme(
    primary = VioletaClaro,
    onPrimary = Color.White,
    secondary = Color(0xFF5F656D),
    background = Color(0xFFFAFAFB),
    onBackground = Color(0xFF16181C),
    surface = Color.White,
    onSurface = Color(0xFF16181C),
    surfaceVariant = Color(0xFFEFF0F2),
    onSurfaceVariant = Color(0xFF5F656D),
    outline = Color(0xFFD8DADE),
    error = Vermelho,
    onError = Color.White,
)

/** Cores de estado, fora do esquema do Material por serem semânticas. */
object CoresEstado {
    val tor = Violeta
    val seguro = Verde
    val aviso = Ambar
    val perigo = Vermelho
}

/**
 * O tipo de letra vai embutido no APK.
 *
 * Declarar `FontFamily.SansSerif` não chega: em telemóveis Samsung o `sans-serif`
 * é remapeado para a fonte que o dono escolheu nas definições do sistema — no
 * aparelho onde isto foi testado, uma manuscrita, que desfazia por completo o
 * desenho. Com a Inter embutida, a app tem sempre o aspeto que foi desenhado,
 * seja qual for o telemóvel.
 */
private val Neutra = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val TipografiaBruma = Typography(
    // A barra de endereço é o texto que mais se lê nesta app, e lê-se de
    // relance com o telemóvel a meio do braço: corpo maior do que o habitual.
    bodyLarge = TextStyle(fontFamily = Neutra, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Neutra, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Neutra, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = Neutra, fontSize = 11.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontFamily = Neutra, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = Neutra, fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = 6.sp),
)

@Composable
fun TemaBruma(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    val esquema = if (escuro) Escuro else Claro
    val contexto = LocalContext.current

    SideEffect {
        (contexto as? Activity)?.window?.let { janela ->
            WindowCompat.getInsetsController(janela, janela.decorView)
                .isAppearanceLightStatusBars = !escuro
        }
    }

    MaterialTheme(colorScheme = esquema, typography = TipografiaBruma, content = conteudo)
}
