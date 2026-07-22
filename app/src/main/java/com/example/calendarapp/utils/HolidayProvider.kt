package com.example.calendarapp.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

/**
 * Proveedor de feriados nacionales de Chile.
 * Fuente: Ley 2.977, Ley 19.973 y sus modificaciones.
 *
 * Incluye:
 *  - Feriados inamovibles (fecha fija)
 *  - Feriados trasladables (Ley 19.973): San Pedro y San Pablo, Día del Encuentro
 *  - Feriados religiosos móviles: Viernes Santo y Sábado Santo (calculados desde Pascua)
 *  - Feriados "puente" automáticos de Fiestas Patrias (18-19 septiembre)
 */
object HolidayProvider {

    data class Holiday(
        val date: LocalDate,
        val name: String,
        val nameEn: String,
        val isOptional: Boolean = false
    )

    // ─── API PÚBLICA ─────────────────────────────────────────────────

    /** Devuelve todos los feriados del año dado como mapa fecha → Holiday. */
    fun getHolidays(year: Int): Map<LocalDate, Holiday> {
        val list = mutableListOf<Holiday>()
        list += fixedHolidays(year)
        list += mobileHolidays(year)
        list += patriasBridges(year)
        return list.associateBy { it.date }
    }

    // ─── FERIADOS INAMOVIBLES ────────────────────────────────────────

    private fun fixedHolidays(year: Int): List<Holiday> = listOf(
        Holiday(LocalDate.of(year, Month.JANUARY,   1), "Año Nuevo",                                     "New Year's Day"),
        Holiday(LocalDate.of(year, Month.MAY,        1), "Día del Trabajo",                               "Labor Day"),
        Holiday(LocalDate.of(year, Month.MAY,       21), "Día de las Glorias Navales",                    "Navy Day"),
        Holiday(LocalDate.of(year, Month.JULY,      16), "Virgen del Carmen",                             "Our Lady of Mount Carmel"),
        Holiday(LocalDate.of(year, Month.AUGUST,    15), "Asunción de la Virgen",                         "Assumption Day"),
        Holiday(LocalDate.of(year, Month.SEPTEMBER, 18), "Independencia Nacional",                        "National Independence Day"),
        Holiday(LocalDate.of(year, Month.SEPTEMBER, 19), "Glorias del Ejército",                          "Army Day"),
        Holiday(LocalDate.of(year, Month.OCTOBER,   31), "Día de las Iglesias Evangélicas",               "Reformation Day"),
        Holiday(LocalDate.of(year, Month.NOVEMBER,   1), "Día de Todos los Santos",                       "All Saints' Day"),
        Holiday(LocalDate.of(year, Month.DECEMBER,   8), "Inmaculada Concepción",                         "Immaculate Conception"),
        Holiday(LocalDate.of(year, Month.DECEMBER,  25), "Navidad",                                       "Christmas Day"),
    )

    // ─── FERIADOS MÓVILES Y TRASLADABLES ────────────────────────────

    private fun mobileHolidays(year: Int): List<Holiday> {
        val list = mutableListOf<Holiday>()
        val easter = easterDate(year)

        // Semana Santa
        list += Holiday(easter.minusDays(2), "Viernes Santo",  "Good Friday")
        list += Holiday(easter.minusDays(1), "Sábado Santo",   "Holy Saturday")

        // San Pedro y San Pablo — 29 de junio (trasladable Ley 19.973)
        list += transferHoliday(year, Month.JUNE, 29,
            "San Pedro y San Pablo", "St. Peter and Paul's Day")

        // Día del Encuentro de Dos Mundos — 12 de octubre (trasladable Ley 19.973)
        list += transferHoliday(year, Month.OCTOBER, 12,
            "Día del Encuentro de Dos Mundos", "Columbus Day")

        return list
    }

    /**
     * Regla de traslado chilena (Ley 19.973, Art. 2):
     *  - Si cae MARTES  → se traslada al LUNES anterior
     *  - Si cae MIÉRCOLES, JUEVES o VIERNES → se traslada al LUNES siguiente
     *  - Cualquier otro día → sin traslado
     */
    private fun transferHoliday(
        year: Int, month: Month, day: Int,
        nameEs: String, nameEn: String
    ): Holiday {
        val base = LocalDate.of(year, month, day)
        val effective = when (base.dayOfWeek) {
            DayOfWeek.TUESDAY                               -> base.minusDays(1)
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY                                -> nextMonday(base)
            else                                            -> base
        }
        return Holiday(effective, nameEs, nameEn)
    }

    // ─── PUENTES DE FIESTAS PATRIAS ──────────────────────────────────

    /**
     * En Chile, cuando el 18 de septiembre cae MARTES → el lunes 17 es feriado.
     * Cuando el 19 de septiembre cae VIERNES → el lunes 20 también es feriado.
     * (feriados "puente" de Fiestas Patrias por decreto o ley habitual)
     */
    private fun patriasBridges(year: Int): List<Holiday> {
        val list = mutableListOf<Holiday>()
        val sep18 = LocalDate.of(year, Month.SEPTEMBER, 18)
        val sep19 = LocalDate.of(year, Month.SEPTEMBER, 19)

        // Puente antes del 18: si cae martes, el lunes 17 es feriado
        if (sep18.dayOfWeek == DayOfWeek.TUESDAY) {
            list += Holiday(sep18.minusDays(1), "Feriado Puente Fiestas Patrias", "Patriotic Bridge Holiday")
        }
        // Puente después del 19: si cae viernes, el lunes 20 es feriado
        if (sep19.dayOfWeek == DayOfWeek.FRIDAY) {
            list += Holiday(sep19.plusDays(1), "Feriado Puente Fiestas Patrias", "Patriotic Bridge Holiday")
        }

        return list
    }

    // ─── CÁLCULO DE PASCUA (Algoritmo Meeus/Jones/Butcher) ──────────

    private fun easterDate(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day   = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    // ─── HELPERS ─────────────────────────────────────────────────────

    private fun nextMonday(date: LocalDate): LocalDate {
        var d = date.plusDays(1)
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.plusDays(1)
        return d
    }
}
