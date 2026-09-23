package com.watchfulai.mywidgets.tasbeeh

import android.content.Context
import org.json.JSONObject

class TasbeehRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("tasbeeh_widgets", Context.MODE_PRIVATE)

    fun get(id: Int): TasbeehConfig? = synchronized(lock) {
        preferences.getString(id.toString(), null)?.let { encoded ->
            runCatching {
                val json = JSONObject(encoded)
                TasbeehConfig(
                    dhikr = json.getString("dhikr"),
                    meaning = json.optString("meaning"),
                    target = json.optInt("target", 33),
                    count = json.optInt("count", 0),
                    theme = TasbeehTheme.entries.firstOrNull { it.name == json.optString("theme") } ?: TasbeehTheme.GREEN,
                    showMeaning = json.optBoolean("showMeaning", true),
                    restartAtTarget = json.optBoolean("restartAtTarget", false),
                ).normalized()
            }.getOrNull()
        }
    }

    // Keep read/modify/write atomic across provider and configuration instances.
    fun update(id: Int, transform: (TasbeehConfig?) -> TasbeehConfig) = synchronized(lock) {
        val config = transform(get(id)).normalized()
        val json = JSONObject().apply {
            put("dhikr", config.dhikr)
            put("meaning", config.meaning)
            put("target", config.target)
            put("count", config.count)
            put("theme", config.theme.name)
            put("showMeaning", config.showMeaning)
            put("restartAtTarget", config.restartAtTarget)
        }
        check(preferences.edit().putString(id.toString(), json.toString()).commit())
    }

    fun remove(id: Int) = synchronized(lock) {
        preferences.edit().remove(id.toString()).commit()
    }

    companion object { private val lock = Any() }
}
