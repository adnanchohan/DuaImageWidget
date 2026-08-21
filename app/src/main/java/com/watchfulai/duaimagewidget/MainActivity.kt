package com.watchfulai.duaimagewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchfulai.duaimagewidget.data.AppSettings
import com.watchfulai.duaimagewidget.data.AppSettingsRepository
import com.watchfulai.duaimagewidget.ui.components.BrandMark
import com.watchfulai.duaimagewidget.ui.components.DuaIconButton
import com.watchfulai.duaimagewidget.ui.components.DuaPill
import com.watchfulai.duaimagewidget.ui.components.DuaPrimaryButton
import com.watchfulai.duaimagewidget.ui.components.DuaSurfaceCard
import com.watchfulai.duaimagewidget.ui.LocaleAwareActivity
import com.watchfulai.duaimagewidget.ui.settings.SettingsActivity
import com.watchfulai.duaimagewidget.ui.theme.DuaImageWidgetTheme
import com.watchfulai.duaimagewidget.ui.theme.Gold300
import com.watchfulai.duaimagewidget.ui.widgets.YourWidgetsActivity
import com.watchfulai.duaimagewidget.widget.DuaImageWidgetReceiver

class MainActivity : LocaleAwareActivity() {
    private val settingsRepository by lazy { AppSettingsRepository(applicationContext) }
    private var activeWidgetCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
            DuaImageWidgetTheme(appTheme = settings.theme) {
                var status by remember { mutableStateOf<String?>(null) }
                val pinRequestedMessage = stringResource(R.string.status_widget_pin_requested)
                val pinFallbackMessage = stringResource(R.string.status_widget_pin_fallback)
                HomeScreen(
                    status = status,
                    activeWidgetCount = activeWidgetCount,
                    onSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onYourWidgets = {
                        startActivity(Intent(this, YourWidgetsActivity::class.java))
                    },
                    onAddWidget = {
                        status = if (requestWidgetPin()) {
                            pinRequestedMessage
                        } else {
                            pinFallbackMessage
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeWidgetCount = AppWidgetManager.getInstance(this).getAppWidgetIds(
            ComponentName(this, DuaImageWidgetReceiver::class.java),
        ).size
    }

    private fun requestWidgetPin(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val configureIntent = Intent(
            this,
            com.watchfulai.duaimagewidget.ui.configuration.WidgetConfigurationActivity::class.java,
        ).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val successCallback = PendingIntent.getActivity(
            this,
            PIN_WIDGET_REQUEST_CODE,
            configureIntent,
            pendingIntentFlags,
        )
        return manager.requestPinAppWidget(
            ComponentName(this, DuaImageWidgetReceiver::class.java),
            null,
            successCallback,
        )
    }

    private companion object {
        const val PIN_WIDGET_REQUEST_CODE = 1001
    }
}

@Composable
private fun HomeScreen(
    status: String?,
    activeWidgetCount: Int,
    onSettings: () -> Unit,
    onYourWidgets: () -> Unit,
    onAddWidget: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DuaPrimaryButton(
                        text = stringResource(R.string.home_add_widget),
                        onClick = onAddWidget,
                        modifier = Modifier.fillMaxWidth(),
                        /*leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(20.dp),
                            )
                        },*/
                    )
                    Text(
                        text = stringResource(R.string.watchfulai_apps),
                        modifier = Modifier.padding(top = 9.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DuaPill(text = stringResource(R.string.home_reminder_pill))
                    val heroPrefix = stringResource(R.string.home_hero_prefix)
                    val heroDua = stringResource(R.string.home_hero_dua)
                    val heroSuffix = stringResource(R.string.home_hero_suffix)
                    Text(
                        text = buildAnnotatedString {
                            append(heroPrefix)
                            withStyle(
                                SpanStyle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF8C6514),
                                            Color(0xFFFFE082),
                                            Color(0xFFD4AF37),
                                            Color(0xFFFFF1A8),
                                            Color(0xFF9B741A),
                                        ),
                                    ),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black,
                                ),
                            ) {
                                append(heroDua)
                            }
                            append(heroSuffix)
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.home_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DuaIconButton(
                    icon = R.drawable.ic_settings,
                    contentDescription = stringResource(R.string.open_settings),
                    onClick = onSettings,
                )
            }

            YourWidgetsTile(
                activeWidgetCount = activeWidgetCount,
                onClick = onYourWidgets,
            )

            WidgetShowcase()

            status?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_home),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = it,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            DuaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        stringResource(R.string.home_steps_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    HomeStep(
                        "01",
                        stringResource(R.string.home_step_add_title),
                        stringResource(R.string.home_step_add_description),
                    )
                    HomeStep(
                        "02",
                        stringResource(R.string.home_step_pick_title),
                        stringResource(R.string.home_step_pick_description),
                    )
                    HomeStep(
                        "03",
                        stringResource(R.string.home_step_frame_title),
                        stringResource(R.string.home_step_frame_description),
                    )
                }
            }

//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(
//                        MaterialTheme.colorScheme.surfaceVariant,
//                        RoundedCornerShape(22.dp),
//                    )
//                    .padding(18.dp),
//                horizontalArrangement = Arrangement.spacedBy(14.dp),
//                verticalAlignment = Alignment.CenterVertically,
//            ) {
//                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
//                    Icon(
//                        painter = painterResource(R.drawable.ic_shield),
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.padding(11.dp),
//                    )
//                }
//                Column(modifier = Modifier.weight(1f)) {
//                    Text("Your images stay yours", style = MaterialTheme.typography.titleMedium)
//                    Text(
//                        "Everything is stored only on this device.",
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        style = MaterialTheme.typography.bodySmall,
//                    )
//                }
//            }
        }
    }
}

@Composable
private fun YourWidgetsTile(
    activeWidgetCount: Int,
    onClick: () -> Unit,
) {
    DuaSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    painter = painterResource(R.drawable.ic_widgets),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    stringResource(R.string.home_your_widgets_title),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.home_your_widgets_description),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.widgets_active_count,
                            activeWidgetCount,
                            activeWidgetCount,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.open_your_widgets),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun WidgetShowcase() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(244.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(30.dp))
            .padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(7.dp)
                    .background(Gold300, CircleShape),
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_showcase_dua),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.home_showcase_translation),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        DuaPill(
            text = stringResource(R.string.home_showcase_size_pill),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HomeStep(number: String, title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = number,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
