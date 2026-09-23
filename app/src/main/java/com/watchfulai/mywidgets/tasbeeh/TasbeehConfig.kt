package com.watchfulai.mywidgets.tasbeeh

data class TasbeehConfig(
    val dhikr: String = "سُبْحَانَ اللَّهِ",
    val meaning: String = "SubhanAllah",
    val target: Int = 33,
    val count: Int = 0,
    val theme: TasbeehTheme = TasbeehTheme.GREEN,
    val showMeaning: Boolean = true,
    val restartAtTarget: Boolean = false,
) {
    fun increment(): TasbeehConfig = copy(count = when {
        count < target -> count + 1
        restartAtTarget -> 1
        else -> target
    })

    fun normalized(): TasbeehConfig {
        val safeTarget = target.coerceIn(1, 99999)
        return copy(
            dhikr = dhikr.trim().take(120).ifBlank { "سُبْحَانَ اللَّهِ" },
            meaning = meaning.trim().take(160),
            target = safeTarget,
            count = count.coerceIn(0, safeTarget),
        )
    }
}

enum class TasbeehTheme(val background: Long) {
    GREEN(0xFF155E48), BLUE(0xFF203D66), PLUM(0xFF58344F),
}
