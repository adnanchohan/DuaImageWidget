package com.watchfulai.duaimagewidget.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.watchfulai.duaimagewidget.R
import com.watchfulai.duaimagewidget.data.AppSettings
import com.watchfulai.duaimagewidget.data.AppSettingsRepository
import com.watchfulai.duaimagewidget.prayer.PrayerTimesWidgetProvider
import com.watchfulai.duaimagewidget.ui.LocaleAwareActivity
import com.watchfulai.duaimagewidget.ui.components.BrandMark
import com.watchfulai.duaimagewidget.ui.components.DuaIconButton
import com.watchfulai.duaimagewidget.ui.components.DuaSurfaceCard
import com.watchfulai.duaimagewidget.ui.theme.DuaImageWidgetTheme
import com.watchfulai.duaimagewidget.widget.DuaImageWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WidgetGalleryActivity : LocaleAwareActivity() {
    private val settingsRepository by lazy { AppSettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
            val snackbarHostState = remember { SnackbarHostState() }
            val comingSoonMessage = stringResource(R.string.gallery_coming_soon)
            DuaImageWidgetTheme(appTheme = settings.theme) {
                WidgetGalleryScreen(
                    snackbarHostState = snackbarHostState,
                    onBack = ::finish,
                    onAddDuaWidget = ::addDuaWidget,
                    onAddPrayerWidget = ::addPrayerWidget,
                    onComingSoon = { scope ->
                        scope.launch { snackbarHostState.showSnackbar(comingSoonMessage) }
                    },
                )
            }
        }
    }

    private fun addDuaWidget() {
        requestWidgetPin(
            providerClass = DuaImageWidgetReceiver::class.java,
            configurationClass = com.watchfulai.duaimagewidget.ui.configuration
                .WidgetConfigurationActivity::class.java,
            requestCode = PIN_DUA_WIDGET_REQUEST_CODE,
        )
    }

    private fun addPrayerWidget() {
        requestWidgetPin(
            providerClass = PrayerTimesWidgetProvider::class.java,
            configurationClass = com.watchfulai.duaimagewidget.ui.prayer
                .PrayerWidgetConfigurationActivity::class.java,
            requestCode = PIN_PRAYER_WIDGET_REQUEST_CODE,
        )
    }

    private fun requestWidgetPin(
        providerClass: Class<*>,
        configurationClass: Class<*>,
        requestCode: Int,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val configureIntent = Intent(this, configurationClass).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val successCallback = PendingIntent.getActivity(
            this,
            requestCode,
            configureIntent,
            pendingIntentFlags,
        )
        return manager.requestPinAppWidget(
            ComponentName(this, providerClass),
            null,
            successCallback,
        )
    }

    private companion object {
        const val PIN_DUA_WIDGET_REQUEST_CODE = 2001
        const val PIN_PRAYER_WIDGET_REQUEST_CODE = 2002
    }
}

private data class WidgetTypeInfo(
    val nameRes: Int,
    val descriptionRes: Int,
    val iconRes: Int,
    val isAvailable: Boolean,
)

private val widgetTypes = listOf(
    WidgetTypeInfo(
        nameRes = R.string.gallery_dua_image_name,
        descriptionRes = R.string.gallery_dua_image_description,
        iconRes = R.drawable.ic_image,
        isAvailable = true,
    ),
    WidgetTypeInfo(
        nameRes = R.string.gallery_prayer_times_name,
        descriptionRes = R.string.gallery_prayer_times_description,
        iconRes = R.drawable.ic_mosque,
        isAvailable = true,
    ),
    WidgetTypeInfo(
        nameRes = R.string.gallery_tasbeeh_name,
        descriptionRes = R.string.gallery_tasbeeh_description,
        iconRes = R.drawable.ic_counter,
        isAvailable = false,
    ),
    WidgetTypeInfo(
        nameRes = R.string.gallery_hadith_name,
        descriptionRes = R.string.gallery_hadith_description,
        iconRes = R.drawable.ic_book,
        isAvailable = false,
    ),
    WidgetTypeInfo(
        nameRes = R.string.gallery_ayat_name,
        descriptionRes = R.string.gallery_ayat_description,
        iconRes = R.drawable.ic_quran,
        isAvailable = false,
    ),
    WidgetTypeInfo(
        nameRes = R.string.gallery_quotes_name,
        descriptionRes = R.string.gallery_quotes_description,
        iconRes = R.drawable.ic_edit,
        isAvailable = false,
    ),
)

@Composable
private fun WidgetGalleryScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddDuaWidget: () -> Unit,
    onAddPrayerWidget: () -> Unit,
    onComingSoon: (CoroutineScope) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DuaIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.gallery_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.gallery_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                BrandMark(size = 40.dp)
            }

            Spacer(Modifier.height(4.dp))

            widgetTypes.forEachIndexed { index, widgetType ->
                WidgetTypeTile(
                    widgetType = widgetType,
                    onClick = when (index) {
                        0 -> onAddDuaWidget
                        1 -> onAddPrayerWidget
                        else -> null
                    },
                    onComingSoon = onComingSoon,
                )
            }
        }
    }
}

@Composable
private fun WidgetTypeTile(
    widgetType: WidgetTypeInfo,
    onClick: (() -> Unit)?,
    onComingSoon: (CoroutineScope) -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    DuaSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (widgetType.isAvailable && onClick != null) {
                    onClick()
                } else {
                    onComingSoon(scope)
                }
            },
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (widgetType.isAvailable) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Icon(
                    painter = painterResource(widgetType.iconRes),
                    contentDescription = null,
                    tint = if (widgetType.isAvailable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .padding(12.dp)
                        .size(26.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    stringResource(widgetType.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(widgetType.descriptionRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!widgetType.isAvailable) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = stringResource(R.string.gallery_coming_soon),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
