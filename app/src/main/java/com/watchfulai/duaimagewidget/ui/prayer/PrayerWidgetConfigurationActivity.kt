package com.watchfulai.duaimagewidget.ui.prayer

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.watchfulai.duaimagewidget.R
import com.watchfulai.duaimagewidget.data.AppSettings
import com.watchfulai.duaimagewidget.data.AppSettingsRepository
import com.watchfulai.duaimagewidget.prayer.AlAdhanPrayerApi
import com.watchfulai.duaimagewidget.prayer.AUTOMATIC_CALCULATION_METHOD
import com.watchfulai.duaimagewidget.prayer.CurrentLocationResolver
import com.watchfulai.duaimagewidget.prayer.HANAFI_ASR_SCHOOL
import com.watchfulai.duaimagewidget.prayer.PrayerTimesWidgetUpdater
import com.watchfulai.duaimagewidget.prayer.PrayerWidgetConfig
import com.watchfulai.duaimagewidget.prayer.PrayerWidgetRepository
import com.watchfulai.duaimagewidget.prayer.SHAFI_ASR_SCHOOL
import com.watchfulai.duaimagewidget.prayer.supportedCalculationMethods
import com.watchfulai.duaimagewidget.ui.LocaleAwareActivity
import com.watchfulai.duaimagewidget.ui.components.DuaIconButton
import com.watchfulai.duaimagewidget.ui.components.DuaPrimaryButton
import com.watchfulai.duaimagewidget.ui.components.DuaSurfaceCard
import com.watchfulai.duaimagewidget.ui.theme.DuaImageWidgetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrayerWidgetConfigurationActivity : LocaleAwareActivity() {
    private val appWidgetId: Int by lazy {
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
    }
    private val repository by lazy { PrayerWidgetRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        enableEdgeToEdge()
        setContent {
            var appSettings by remember { mutableStateOf(AppSettings()) }
            val settingsRepository = remember { AppSettingsRepository(applicationContext) }
            androidx.compose.runtime.LaunchedEffect(settingsRepository) {
                settingsRepository.settings.collectLatest { appSettings = it }
            }
            DuaImageWidgetTheme(appTheme = appSettings.theme) {
                PrayerConfigurationScreen(
                    appWidgetId = appWidgetId,
                    existing = repository.getConfig(appWidgetId),
                    onCancel = ::finish,
                    onSaved = ::finishSuccessfully,
                )
            }
        }
    }

    private fun finishSuccessfully() {
        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        finish()
    }
}

@Composable
private fun PrayerConfigurationScreen(
    appWidgetId: Int,
    existing: PrayerWidgetConfig?,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var latitude by remember { mutableStateOf(existing?.latitude) }
    var longitude by remember { mutableStateOf(existing?.longitude) }
    var locationLabel by remember { mutableStateOf(existing?.locationLabel) }
    var calculationMethodId by remember {
        mutableStateOf(existing?.calculationMethodId ?: AUTOMATIC_CALCULATION_METHOD)
    }
    var asrSchool by remember { mutableStateOf(existing?.asrSchool ?: SHAFI_ASR_SCHOOL) }
    var isLocating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var methodMenuExpanded by remember { mutableStateOf(false) }

    suspend fun findLocation() {
        isLocating = true
        errorMessage = null
        val found = CurrentLocationResolver.resolve(context)
        if (found == null) {
            errorMessage = resources.getString(R.string.prayer_location_unavailable)
        } else {
            latitude = found.latitude
            longitude = found.longitude
            locationLabel = withContext(Dispatchers.IO) {
                CurrentLocationResolver.locationLabel(context, found)
            }
        }
        isLocating = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            scope.launch { findLocation() }
        } else {
            errorMessage = resources.getString(R.string.prayer_location_permission_denied)
        }
    }

    fun requestLocation() {
        if (CurrentLocationResolver.hasPermission(context)) {
            scope.launch { findLocation() }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    fun saveWidget() {
        val savedLatitude = latitude
        val savedLongitude = longitude
        val savedLabel = locationLabel
        if (savedLatitude == null || savedLongitude == null || savedLabel.isNullOrBlank()) {
            errorMessage = resources.getString(R.string.prayer_choose_location_first)
            return
        }
        scope.launch {
            isSaving = true
            errorMessage = null
            val config = PrayerWidgetConfig(
                appWidgetId = appWidgetId,
                latitude = savedLatitude,
                longitude = savedLongitude,
                locationLabel = savedLabel,
                calculationMethodId = calculationMethodId,
                asrSchool = asrSchool,
            )
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val schedule = AlAdhanPrayerApi().fetchSchedule(config)
                    PrayerWidgetRepository(context).saveConfig(config)
                    PrayerWidgetRepository(context).saveSchedule(appWidgetId, schedule)
                    PrayerTimesWidgetUpdater.update(context, appWidgetId, allowNetwork = false)
                }
            }
            isSaving = false
            result.onSuccess { onSaved() }
                .onFailure {
                    errorMessage = resources.getString(R.string.prayer_api_error)
                }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DuaIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.back),
                    onClick = onCancel,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.prayer_config_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.prayer_config_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            DuaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        stringResource(R.string.prayer_location_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        locationLabel ?: stringResource(R.string.prayer_no_location),
                        color = if (locationLabel == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedButton(
                        onClick = ::requestLocation,
                        enabled = !isLocating && !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (isLocating) {
                                stringResource(R.string.prayer_finding_location)
                            } else {
                                stringResource(R.string.prayer_use_current_location)
                            },
                        )
                    }
                }
            }

            DuaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        stringResource(R.string.prayer_calculation_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.prayer_calculation_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { methodMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving,
                        ) {
                            Text(
                                supportedCalculationMethods.first {
                                    it.id == calculationMethodId
                                }.label,
                            )
                        }
                        DropdownMenu(
                            expanded = methodMenuExpanded,
                            onDismissRequest = { methodMenuExpanded = false },
                        ) {
                            supportedCalculationMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.label) },
                                    onClick = {
                                        calculationMethodId = method.id
                                        methodMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Text(
                        stringResource(R.string.prayer_asr_school_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilterChip(
                            selected = asrSchool == SHAFI_ASR_SCHOOL,
                            onClick = { asrSchool = SHAFI_ASR_SCHOOL },
                            label = { Text(stringResource(R.string.prayer_school_standard)) },
                            enabled = !isSaving,
                        )
                        FilterChip(
                            selected = asrSchool == HANAFI_ASR_SCHOOL,
                            onClick = { asrSchool = HANAFI_ASR_SCHOOL },
                            label = { Text(stringResource(R.string.prayer_school_hanafi)) },
                            enabled = !isSaving,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = stringResource(R.string.prayer_accuracy_notice),
                    modifier = Modifier.padding(15.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            DuaPrimaryButton(
                text = if (isSaving) {
                    stringResource(R.string.prayer_loading_times)
                } else {
                    stringResource(R.string.prayer_save_widget)
                },
                onClick = ::saveWidget,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && !isLocating,
            )
        }
    }
}
