package pt.shrek.bruma.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import pt.shrek.bruma.MainActivity
import pt.shrek.bruma.R

/**
 * Um widget de pesquisa para o ecrã principal do Android.
 *
 * Não tem caixa de texto, e não é por preguiça: um widget é desenhado com
 * `RemoteViews`, que corre no processo do lançador e só suporta um punhado de
 * vistas — nenhuma delas editável. Todos os widgets de pesquisa, incluindo os da
 * Google e do Chrome, são na verdade um botão com ar de caixa que abre a app com
 * o teclado já levantado. Este faz o mesmo.
 *
 * Tem dois alvos: a barra, que abre a busca normal, e o escudo, que liga o tor
 * antes de abrir — útil para quem quer começar já protegido sem ter de se
 * lembrar de o ligar depois.
 */
class WidgetPesquisa : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        gestor: AppWidgetManager,
        identificadores: IntArray,
    ) {
        identificadores.forEach { id -> gestor.updateAppWidget(id, construir(context)) }
    }

    private fun construir(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_pesquisa).apply {
            setOnClickPendingIntent(R.id.widget_barra, intentPara(context, ACAO_PROCURAR))
            setOnClickPendingIntent(R.id.widget_tor, intentPara(context, ACAO_PROCURAR_COM_TOR))
        }

    private fun intentPara(context: Context, acao: String): PendingIntent {
        val intencao = Intent(context, MainActivity::class.java).apply {
            action = acao
            // Sem NEW_TASK o lançador recusa-se a abrir a atividade; com
            // CLEAR_TOP reaproveita a que já estiver aberta em vez de empilhar
            // navegadores uns sobre os outros.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            acao.hashCode(),
            intencao,
            // IMMUTABLE porque nada de fora deve poder alterar para onde este
            // intent aponta; é exigido a partir do Android 12 de qualquer forma.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACAO_PROCURAR = "pt.shrek.bruma.PROCURAR"
        const val ACAO_PROCURAR_COM_TOR = "pt.shrek.bruma.PROCURAR_COM_TOR"

        /** Redesenha os widgets todos — útil quando o aspeto depende do tema. */
        fun atualizarTodos(context: Context) {
            val gestor = AppWidgetManager.getInstance(context)
            val ids = gestor.getAppWidgetIds(ComponentName(context, WidgetPesquisa::class.java))
            if (ids.isNotEmpty()) WidgetPesquisa().onUpdate(context, gestor, ids)
        }
    }
}
