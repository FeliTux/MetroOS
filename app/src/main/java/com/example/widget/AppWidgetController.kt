package com.example.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

class AppWidgetController(private val context: Context) {

  private val manager: AppWidgetManager? = runCatching { AppWidgetManager.getInstance(context) }.getOrNull()
  private val host: AppWidgetHost? = runCatching { AppWidgetHost(context, HOST_ID) }.getOrNull()

  private var listening = false

  fun startListening() {
    if (!listening) {
      runCatching { host?.startListening() }
      listening = true
    }
  }

  fun stopListening() {
    if (listening) {
      runCatching { host?.stopListening() }
      listening = false
    }
  }

  fun allocateAppWidgetId(): Int? =
    runCatching { host?.allocateAppWidgetId() }.getOrNull()

  fun deleteAppWidgetId(appWidgetId: Int) {
    runCatching { host?.deleteAppWidgetId(appWidgetId) }
  }

  fun buildPickWidgetIntent(appWidgetId: Int): Intent {
    return Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
  }

  fun getProviderInfo(appWidgetId: Int): AppWidgetProviderInfo? =
    runCatching { manager?.getAppWidgetInfo(appWidgetId) }.getOrNull()

  fun buildConfigureIntent(appWidgetId: Int): Intent? {
    val info = getProviderInfo(appWidgetId) ?: return null
    val configureComponent = info.configure ?: return null
    return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
      component = configureComponent
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
  }

  fun createHostView(appWidgetId: Int): AppWidgetHostView? {
    val info = getProviderInfo(appWidgetId) ?: return null
    return runCatching {
      host?.createView(context, appWidgetId, info)?.apply {
        setAppWidget(appWidgetId, info)
      }
    }.getOrNull()
  }

  companion object {
    private const val HOST_ID = 0x424C4F43 // "BLOC"
  }
}
