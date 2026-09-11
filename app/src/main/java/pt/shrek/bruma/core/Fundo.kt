package pt.shrek.bruma.core

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * O fundo da página inicial.
 *
 * Por omissão não há imagem nenhuma: o fundo é desenhado — um degradê de névoa,
 * que é o que o nome da app quer dizer. Assim a app não carrega uma fotografia
 * genérica no APK nem vai buscar nada à rede para mostrar o primeiro ecrã.
 *
 * A imagem escolhida pelo utilizador é **copiada** para dentro da app em vez de
 * guardada como URI. Um URI do seletor de ficheiros perde a autorização quando o
 * sistema a revoga ou a app que a concedeu é atualizada, e o fundo desaparecia
 * sem explicação; uma cópia é nossa e dura.
 */
object Fundo {

    private const val NOME = "fundo"

    fun ficheiro(context: Context): File = File(context.filesDir, NOME)

    fun existe(context: Context): Boolean = ficheiro(context).let { it.exists() && it.length() > 0 }

    /** Copia a imagem escolhida para dentro da app. Devolve false se não deu. */
    fun guardar(context: Context, origem: Uri): Boolean = runCatching {
        val destino = ficheiro(context)
        val temporario = File(context.filesDir, "$NOME.parcial")
        context.contentResolver.openInputStream(origem)?.use { entrada ->
            temporario.outputStream().use { saida -> entrada.copyTo(saida) }
        } ?: return false
        // Trocar só no fim: uma cópia interrompida deixaria um fundo corrompido.
        temporario.renameTo(destino)
    }.getOrDefault(false)

    fun apagar(context: Context) {
        ficheiro(context).delete()
    }

    /**
     * Lê a imagem já reduzida ao tamanho do ecrã. Uma fotografia de 50 MP
     * descodificada por inteiro são ~200 MB em memória — num processo que já
     * partilha o aparelho com o Gecko, é morte certa.
     */
    fun carregar(context: Context, larguraEcra: Int, alturaEcra: Int): ImageBitmap? {
        val f = ficheiro(context)
        if (!f.exists() || f.length() == 0L) return null
        return runCatching {
            val medida = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, medida)
            var amostra = 1
            while (medida.outWidth / amostra > larguraEcra * 1.5 &&
                medida.outHeight / amostra > alturaEcra * 1.5
            ) {
                amostra *= 2
            }
            val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
            BitmapFactory.decodeFile(f.absolutePath, opcoes)?.asImageBitmap()
        }.getOrNull()
    }
}
