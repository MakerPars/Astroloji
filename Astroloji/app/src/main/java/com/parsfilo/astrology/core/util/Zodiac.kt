package com.parsfilo.astrology.core.util

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.parsfilo.astrology.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class ZodiacElement(
    val nameTr: String,
    val nameEn: String,
    val color: Color,
) {
    FIRE("Ateş", "Fire", Color(0xFFE8593C)),
    EARTH("Toprak", "Earth", Color(0xFF8B7355)),
    AIR("Hava", "Air", Color(0xFF5B9BD5)),
    WATER("Su", "Water", Color(0xFF4A90A4)),
}

enum class ZodiacSign(
    val key: String,
    val nameTr: String,
    val nameEn: String,
    val symbol: String,
    val dateRange: String,
    val element: ZodiacElement,
    val planet: String,
    @DrawableRes val iconRes: Int,
) {
    ARIES("aries", "Koç", "Aries", "♈", "21 Mar - 19 Nis", ZodiacElement.FIRE, "Mars", R.drawable.ic_zodiac_generic),
    TAURUS("taurus", "Boğa", "Taurus", "♉", "20 Nis - 20 May", ZodiacElement.EARTH, "Venüs", R.drawable.ic_zodiac_generic),
    GEMINI("gemini", "İkizler", "Gemini", "♊", "21 May - 20 Haz", ZodiacElement.AIR, "Merkür", R.drawable.ic_zodiac_generic),
    CANCER("cancer", "Yengeç", "Cancer", "♋", "21 Haz - 22 Tem", ZodiacElement.WATER, "Ay", R.drawable.ic_zodiac_generic),
    LEO("leo", "Aslan", "Leo", "♌", "23 Tem - 22 Ağu", ZodiacElement.FIRE, "Güneş", R.drawable.ic_zodiac_generic),
    VIRGO("virgo", "Başak", "Virgo", "♍", "23 Ağu - 22 Eyl", ZodiacElement.EARTH, "Merkür", R.drawable.ic_zodiac_generic),
    LIBRA("libra", "Terazi", "Libra", "♎", "23 Eyl - 22 Eki", ZodiacElement.AIR, "Venüs", R.drawable.ic_zodiac_generic),
    SCORPIO("scorpio", "Akrep", "Scorpio", "♏", "23 Eki - 21 Kas", ZodiacElement.WATER, "Plüton", R.drawable.ic_zodiac_generic),
    SAGITTARIUS("sagittarius", "Yay", "Sagittarius", "♐", "22 Kas - 21 Ara", ZodiacElement.FIRE, "Jüpiter", R.drawable.ic_zodiac_generic),
    CAPRICORN("capricorn", "Oğlak", "Capricorn", "♑", "22 Ara - 19 Oca", ZodiacElement.EARTH, "Satürn", R.drawable.ic_zodiac_generic),
    AQUARIUS("aquarius", "Kova", "Aquarius", "♒", "20 Oca - 18 Şub", ZodiacElement.AIR, "Uranüs", R.drawable.ic_zodiac_generic),
    PISCES("pisces", "Balık", "Pisces", "♓", "19 Şub - 20 Mar", ZodiacElement.WATER, "Neptün", R.drawable.ic_zodiac_generic),
    ;

    fun localizedName(language: String): String = if (TimeUtils.normalizeLanguageTag(language) == "tr") nameTr else nameEn

    companion object {
        fun fromKey(key: String): ZodiacSign = entries.first { it.key == key }

        fun fromKeyOrNull(key: String?): ZodiacSign? = entries.firstOrNull { it.key == key }

        fun fromBirthDate(date: LocalDate): ZodiacSign =
            when {
                matches(date, 3, 21, 4, 19) -> ARIES
                matches(date, 4, 20, 5, 20) -> TAURUS
                matches(date, 5, 21, 6, 20) -> GEMINI
                matches(date, 6, 21, 7, 22) -> CANCER
                matches(date, 7, 23, 8, 22) -> LEO
                matches(date, 8, 23, 9, 22) -> VIRGO
                matches(date, 9, 23, 10, 22) -> LIBRA
                matches(date, 10, 23, 11, 21) -> SCORPIO
                matches(date, 11, 22, 12, 21) -> SAGITTARIUS
                matches(date, 12, 22, 1, 19) -> CAPRICORN
                matches(date, 1, 20, 2, 18) -> AQUARIUS
                else -> PISCES
            }

        fun fromBirthDateMillis(millis: Long): ZodiacSign {
            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            return fromBirthDate(date)
        }

        private fun matches(
            date: LocalDate,
            startMonth: Int,
            startDay: Int,
            endMonth: Int,
            endDay: Int,
        ): Boolean {
            val month = date.monthValue
            val day = date.dayOfMonth
            return if (startMonth <= endMonth) {
                (month > startMonth || (month == startMonth && day >= startDay)) &&
                    (month < endMonth || (month == endMonth && day <= endDay))
            } else {
                (month > startMonth || (month == startMonth && day >= startDay)) ||
                    (month < endMonth || (month == endMonth && day <= endDay))
            }
        }
    }
}
