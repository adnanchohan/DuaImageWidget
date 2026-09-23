package com.watchfulai.mywidgets.ui.tasbeeh

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.watchfulai.mywidgets.R
import com.watchfulai.mywidgets.data.AppSettings
import com.watchfulai.mywidgets.data.AppSettingsRepository
import com.watchfulai.mywidgets.tasbeeh.*
import com.watchfulai.mywidgets.ui.LocaleAwareActivity
import com.watchfulai.mywidgets.ui.components.DuaIconButton
import com.watchfulai.mywidgets.ui.theme.DuaImageWidgetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TasbeehConfigurationActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED)
        if (!TasbeehWidgetProvider.isActive(this, id)) { finish(); return }
        val repository = TasbeehRepository(this)
        val existing = repository.get(id) ?: TasbeehConfig()
        enableEdgeToEdge()
        setContent {
            val settings by remember { AppSettingsRepository(applicationContext) }.settings.collectAsState(initial = AppSettings())
            DuaImageWidgetTheme(appTheme = settings.theme) {
                TasbeehConfigurationScreen(existing, onBack = ::finish) { config, reset ->
                    withContext(Dispatchers.IO) {
                        check(TasbeehWidgetProvider.isActive(this@TasbeehConfigurationActivity, id))
                        repository.update(id) { current ->
                            config.copy(count = if (reset) 0 else (current?.count ?: 0).coerceAtMost(config.target))
                        }
                        TasbeehWidgetProvider.update(this@TasbeehConfigurationActivity, id)
                    }
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                    finish()
                }
            }
        }
    }
}

@Composable
private fun TasbeehConfigurationScreen(existing: TasbeehConfig, onBack: () -> Unit, onSave: suspend (TasbeehConfig, Boolean) -> Unit) {
    var dhikr by rememberSaveable { mutableStateOf(existing.dhikr) }
    var meaning by rememberSaveable { mutableStateOf(existing.meaning) }
    var target by rememberSaveable { mutableStateOf(existing.target.toString()) }
    var theme by rememberSaveable { mutableStateOf(existing.theme) }
    var showMeaning by rememberSaveable { mutableStateOf(existing.showMeaning) }
    var restart by rememberSaveable { mutableStateOf(existing.restartAtTarget) }
    var reset by rememberSaveable { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val validTarget = target.toIntOrNull()?.takeIf { it in 1..99999 }
    val config = TasbeehConfig(dhikr, meaning, validTarget ?: 33, if (reset) 0 else existing.count.coerceAtMost(validTarget ?: 33), theme, showMeaning, restart)
    val presets = listOf(
        "سُبْحَانَ اللَّهِ" to "SubhanAllah",
        "الْحَمْدُ لِلَّهِ" to "Alhamdulillah",
        "اللَّهُ أَكْبَرُ" to "Allahu Akbar",
        "أَسْتَغْفِرُ اللَّهَ" to "Astaghfirullah",
    )
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DuaIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onClick = onBack)
                Text(stringResource(R.string.tasbeeh_config_title), style = MaterialTheme.typography.headlineSmall)
            }
            Text(stringResource(R.string.tasbeeh_preview), style = MaterialTheme.typography.labelLarge)
            TasbeehPreview(config)
            Text(stringResource(R.string.tasbeeh_choose_dhikr), style = MaterialTheme.typography.titleMedium)
            presets.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (text, translation) ->
                        FilterChip(selected = dhikr == text, onClick = { dhikr = text; meaning = translation }, label = { Text(translation) }, enabled = !saving)
                    }
                }
            }
            OutlinedTextField(dhikr, { if (it.length <= 120) dhikr = it }, label = { Text(stringResource(R.string.tasbeeh_dhikr)) }, modifier = Modifier.fillMaxWidth(), enabled = !saving, isError = dhikr.isBlank(), maxLines = 3)
            OutlinedTextField(meaning, { if (it.length <= 160) meaning = it }, label = { Text(stringResource(R.string.tasbeeh_meaning)) }, modifier = Modifier.fillMaxWidth(), enabled = !saving, maxLines = 3)
            OutlinedTextField(target, { if (it.length <= 5 && it.all(Char::isDigit)) target = it }, label = { Text(stringResource(R.string.tasbeeh_target)) }, supportingText = { Text(stringResource(R.string.tasbeeh_target_help)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = validTarget == null, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 99, 100).forEach { count -> FilterChip(selected = validTarget == count, onClick = { target = count.toString() }, label = { Text(count.toString()) }, enabled = !saving) }
            }
            Text(stringResource(R.string.tasbeeh_color), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TasbeehTheme.entries.forEach { item ->
                    FilterChip(selected = theme == item, onClick = { theme = item }, enabled = !saving, label = {
                        Text(stringResource(when (item) { TasbeehTheme.GREEN -> R.string.tasbeeh_green; TasbeehTheme.BLUE -> R.string.tasbeeh_blue; TasbeehTheme.PLUM -> R.string.tasbeeh_plum }))
                    })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tasbeeh_show_meaning), Modifier.weight(1f))
                Switch(showMeaning, { showMeaning = it }, enabled = !saving)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tasbeeh_restart))
                    Text(stringResource(R.string.tasbeeh_restart_help), style = MaterialTheme.typography.bodySmall)
                }
                Switch(restart, { restart = it }, enabled = !saving)
            }
            OutlinedButton(onClick = { confirmReset = true }, enabled = !saving && !reset, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (reset) R.string.tasbeeh_reset_pending else R.string.tasbeeh_reset))
            }
            if (error) Text(stringResource(R.string.tasbeeh_save_error), color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                saving = true; error = false
                scope.launch {
                    try { onSave(config.normalized(), reset) }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { error = true }
                    finally { saving = false }
                }
            }, enabled = !saving && validTarget != null && dhikr.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tasbeeh_save))
            }
        }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text(stringResource(R.string.tasbeeh_reset)) }, text = { Text(stringResource(R.string.tasbeeh_reset_confirm)) }, confirmButton = {
        TextButton(onClick = { reset = true; confirmReset = false }) { Text(stringResource(R.string.tasbeeh_reset)) }
    }, dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(android.R.string.cancel)) } })
}
