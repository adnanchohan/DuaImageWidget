package com.watchfulai.mywidgets.prayer

import android.content.Context
import com.watchfulai.mywidgets.R

enum class PrayerName {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA;

    fun localizedName(context: Context): String = context.getString(
        when (this) {
            FAJR -> R.string.prayer_fajr
            DHUHR -> R.string.prayer_dhuhr
            ASR -> R.string.prayer_asr
            MAGHRIB -> R.string.prayer_maghrib
            ISHA -> R.string.prayer_isha
        },
    )
}

data class PrayerMoment(
    val name: PrayerName,
    val epochMillis: Long,
)

data class HijriDate(
    val day: Int,
    val month: Int,
    val year: Int,
)

data class PrayerSchedule(
    val dateKey: String,
    val timezoneId: String,
    val hijriDate: HijriDate,
    val tomorrowHijriDate: HijriDate,
    val sunriseEpochMillis: Long,
    val prayers: List<PrayerMoment>,
    val tomorrowFajrEpochMillis: Long,
    val calculationMethod: String,
    val fetchedAtEpochMillis: Long = System.currentTimeMillis(),
) {
    init {
        require(prayers.map(PrayerMoment::name) == PrayerName.entries)
    }

    fun nextPrayerAt(nowEpochMillis: Long): PrayerMoment =
        prayers.firstOrNull { it.epochMillis > nowEpochMillis }
            ?: PrayerMoment(PrayerName.FAJR, tomorrowFajrEpochMillis)

    fun hijriDateAt(nowEpochMillis: Long): HijriDate {
        val maghrib = prayers.first { it.name == PrayerName.MAGHRIB }.epochMillis
        return if (nowEpochMillis >= maghrib) tomorrowHijriDate else hijriDate
    }

    fun currentPrayerAt(nowEpochMillis: Long): PrayerName? {
        val byName = prayers.associateBy(PrayerMoment::name)
        val fajr = byName.getValue(PrayerName.FAJR).epochMillis
        val dhuhr = byName.getValue(PrayerName.DHUHR).epochMillis
        val asr = byName.getValue(PrayerName.ASR).epochMillis
        val maghrib = byName.getValue(PrayerName.MAGHRIB).epochMillis
        val isha = byName.getValue(PrayerName.ISHA).epochMillis

        return when {
            nowEpochMillis < fajr -> null
            nowEpochMillis < sunriseEpochMillis -> PrayerName.FAJR
            nowEpochMillis < dhuhr -> null
            nowEpochMillis < asr -> PrayerName.DHUHR
            nowEpochMillis < maghrib -> PrayerName.ASR
            nowEpochMillis < isha -> PrayerName.MAGHRIB
            else -> PrayerName.ISHA
        }
    }
}

data class PrayerWidgetConfig(
    val appWidgetId: Int,
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String,
    val calculationMethodId: Int = AUTOMATIC_CALCULATION_METHOD,
    val asrSchool: Int = SHAFI_ASR_SCHOOL,
)

data class CalculationMethodOption(
    val id: Int,
    val label: String,
)

const val AUTOMATIC_CALCULATION_METHOD = -1
const val SHAFI_ASR_SCHOOL = 0
const val HANAFI_ASR_SCHOOL = 1

val supportedCalculationMethods: List<CalculationMethodOption> = listOf(
    CalculationMethodOption(AUTOMATIC_CALCULATION_METHOD, "Automatic · nearest authority"),
    CalculationMethodOption(1, "University of Islamic Sciences, Karachi"),
    CalculationMethodOption(2, "Islamic Society of North America (ISNA)"),
    CalculationMethodOption(3, "Muslim World League"),
    CalculationMethodOption(4, "Umm Al-Qura University, Makkah"),
    CalculationMethodOption(5, "Egyptian General Authority of Survey"),
    CalculationMethodOption(7, "Institute of Geophysics, Tehran"),
    CalculationMethodOption(8, "Gulf Region"),
    CalculationMethodOption(9, "Kuwait"),
    CalculationMethodOption(10, "Qatar"),
    CalculationMethodOption(11, "MUIS, Singapore"),
    CalculationMethodOption(12, "UOIF, France"),
    CalculationMethodOption(13, "Diyanet, Turkey"),
    CalculationMethodOption(14, "Spiritual Administration of Muslims of Russia"),
    CalculationMethodOption(15, "Moonsighting Committee Worldwide"),
    CalculationMethodOption(16, "Dubai"),
    CalculationMethodOption(17, "JAKIM, Malaysia"),
    CalculationMethodOption(18, "Tunisia"),
    CalculationMethodOption(19, "Algeria"),
    CalculationMethodOption(20, "KEMENAG, Indonesia"),
    CalculationMethodOption(21, "Morocco"),
    CalculationMethodOption(22, "Comunidade Islâmica de Lisboa"),
    CalculationMethodOption(23, "Ministry of Awqaf, Jordan"),
)
