package com.digitalminds.grow

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

class Log(val epochDay: Long, val slot: Int, val height: Float, val weight: Float)

class Store(ctx: Context) {
    private val p: SharedPreferences = ctx.getSharedPreferences("grow", Context.MODE_PRIVATE)

    var voice: Boolean
        get() = p.getBoolean("voice", true)
        set(v) { p.edit().putBoolean("voice", v).apply() }

    /** 0 short, 1 standard, 2 long */
    var restLevel: Int
        get() = p.getInt("rest", 1)
        set(v) { p.edit().putInt("rest", v).apply() }

    val restSeconds: Int get() = intArrayOf(8, 12, 20)[restLevel.coerceIn(0, 2)]

    /** minutes of day, -1 = off */
    var reminder: Int
        get() = p.getInt("reminder", -1)
        set(v) { p.edit().putInt("reminder", v).apply() }

    private fun setOf(key: String): MutableSet<Int> {
        val s = p.getString(key, "") ?: ""
        val out = HashSet<Int>()
        if (s.isNotEmpty()) for (x in s.split(',')) x.toIntOrNull()?.let { out.add(it) }
        return out
    }
    private fun key(jaw: Boolean) = if (jaw) "doneJ" else "doneG"

    fun isDone(day: Int, jaw: Boolean) = setOf(key(jaw)).contains(day)
    fun markDone(day: Int, jaw: Boolean) {
        val s = setOf(key(jaw)); if (s.add(day)) p.edit().putString(key(jaw), s.sorted().joinToString(",")).apply()
    }
    fun totalDone(jaw: Boolean) = setOf(key(jaw)).size

    fun streak(today: Int, jaw: Boolean): Int {
        val s = setOf(key(jaw))
        var d = if (s.contains(today)) today else today - 1
        var n = 0
        while (d >= 1) {
            if (Data.isRest(d)) { d--; continue }
            if (s.contains(d)) { n++; d-- } else break
        }
        return n
    }

    fun bestStreak(jaw: Boolean): Int {
        val s = setOf(key(jaw)); var best = 0; var cur = 0
        for (d in 1..Data.TOTAL) {
            if (Data.isRest(d)) continue
            if (s.contains(d)) { cur++; if (cur > best) best = cur } else cur = 0
        }
        return best
    }

    /** Days (Mon..Sun) of the week containing 'date' that are done, as booleans. */
    fun weekDone(date: LocalDate, jaw: Boolean): BooleanArray {
        val s = setOf(key(jaw))
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        return BooleanArray(7) { i -> val d = Data.dayOf(monday.plusDays(i.toLong())); s.contains(d) }
    }

    // ---- measurements
    fun logs(): List<Log> {
        val s = p.getString("logs", "") ?: ""
        if (s.isEmpty()) return emptyList()
        return s.split(';').mapNotNull {
            val a = it.split('|'); if (a.size < 4) null
            else Log(a[0].toLong(), a[1].toInt(), a[2].toFloat(), a[3].toFloat())
        }
    }
    fun addLog(epochDay: Long, slot: Int, h: Float, w: Float) {
        val all = logs().filterNot { it.epochDay == epochDay && it.slot == slot }.toMutableList()
        all.add(Log(epochDay, slot, h, w))
        all.sortWith(compareBy({ it.epochDay }, { it.slot }))
        p.edit().putString("logs", all.joinToString(";") { "${it.epochDay}|${it.slot}|${it.height}|${it.weight}" }).apply()
    }
    fun lastHeight(): Float = logs().lastOrNull()?.height ?: 164f
    fun lastWeight(): Float = logs().lastOrNull()?.weight ?: 50f
}
