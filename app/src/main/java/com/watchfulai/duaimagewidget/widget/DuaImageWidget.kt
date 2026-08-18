package com.watchfulai.duaimagewidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import com.watchfulai.duaimagewidget.R
import com.watchfulai.duaimagewidget.data.WidgetConfigRepository
import com.watchfulai.duaimagewidget.image.ImageStorage
import com.watchfulai.duaimagewidget.image.WidgetBitmapRenderer
import com.watchfulai.duaimagewidget.ui.configuration.WidgetSizeDp
import com.watchfulai.duaimagewidget.ui.configuration.WidgetConfigurationActivity
import com.watchfulai.duaimagewidget.ui.configuration.putWidgetSize
import kotlin.math.roundToInt

class DuaImageWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val repository = WidgetConfigRepository(context)

        provideContent {
            val localContext = LocalContext.current
            val config by repository.observe(appWidgetId).collectAsState(initial = null)
            val imageFileName = config?.imageFileName
            val source by produceState<android.graphics.Bitmap?>(
                initialValue = null,
                key1 = imageFileName,
            ) {
                value = imageFileName?.let { ImageStorage.load(localContext, it) }
            }
            val size = LocalSize.current
            val density = localContext.resources.displayMetrics.density
            val cornerRadiusPx = remember(localContext, density) {
                widgetCornerRadiusPx(localContext, density)
            }
            val rendered = remember(source, config, size, density, cornerRadiusPx) {
                val currentConfig = config
                val currentSource = source
                if (currentSource == null || currentConfig == null) {
                    null
                } else {
                    WidgetBitmapRenderer.render(
                        source = currentSource,
                        requestedWidth = (size.width.value * density).roundToInt(),
                        requestedHeight = (size.height.value * density).roundToInt(),
                        config = currentConfig,
                        cornerRadiusPx = cornerRadiusPx,
                    )
                }
            }
            WidgetContent(
                context = localContext,
                appWidgetId = appWidgetId,
                widgetSize = WidgetSizeDp(
                    width = size.width.value,
                    height = size.height.value,
                ),
                renderedImage = rendered,
            )
        }
    }

    suspend fun update(context: Context, appWidgetId: Int) {
        update(context, GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId))
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val removed = WidgetConfigRepository(context).remove(appWidgetId)
        removed?.let { ImageStorage.delete(context, it.imageFileName) }
    }
}

@Composable
private fun WidgetContent(
    context: Context,
    appWidgetId: Int,
    widgetSize: WidgetSizeDp,
    renderedImage: android.graphics.Bitmap?,
) {
    val configureIntent = Intent(context, WidgetConfigurationActivity::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putWidgetSize(widgetSize)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    val baseModifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(android.R.dimen.system_app_widget_background_radius)
        .clickable(actionStartActivity(configureIntent))

    if (renderedImage != null) {
        Image(
            provider = ImageProvider(renderedImage),
            contentDescription = context.getString(R.string.widget_image_description),
            modifier = baseModifier,
            contentScale = ContentScale.FillBounds,
        )
    } else {
        Box(
            modifier = baseModifier.background(
                imageProvider = ImageProvider(R.drawable.widget_background),
                colorFilter = null,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = context.getString(R.string.widget_choose_image),
            )
        }
    }
}

private fun widgetCornerRadiusPx(context: Context, density: Float): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.resources.getDimension(android.R.dimen.system_app_widget_background_radius)
    } else {
        FALLBACK_WIDGET_CORNER_RADIUS_DP * density
    }

private const val FALLBACK_WIDGET_CORNER_RADIUS_DP = 20f
