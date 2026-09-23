package com.watchfulai.mywidgets.ui.tasbeeh

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.watchfulai.mywidgets.R
import com.watchfulai.mywidgets.tasbeeh.TasbeehConfig
import com.watchfulai.mywidgets.ui.theme.Gold300
import java.text.NumberFormat

@Composable
fun TasbeehPreview(config: TasbeehConfig) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color(config.theme.background), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_counter), null, tint = Gold300, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.gallery_tasbeeh_name), color = Gold300, style = MaterialTheme.typography.labelMedium)
                }
                Text(config.dhikr, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2)
                if (config.showMeaning && config.meaning.isNotBlank()) {
                    Text(config.meaning, color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .15f), border = BorderStroke(2.dp, Gold300)) {
                Column(Modifier.size(100.dp).padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    val formatter = NumberFormat.getIntegerInstance(androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
                    Text("${formatter.format(config.count)} / ${formatter.format(config.target)}", color = Color.White, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.showcase_tasbeeh_tap), color = Gold300, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
