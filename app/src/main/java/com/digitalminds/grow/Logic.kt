package com.digitalminds.grow

/** Pure progress logic (no Android types) so it can be tested on the JVM. */
object Logic {
    /** consecutive training days up to today (Saturdays are rest days and never break the chain) */
    fun streak(done: Set<Int>, today: Int, isRest: (Int) -> Boolean): Int {
        var d = if (done.contains(today)) today else today - 1
        var n = 0
        while (d >= 1) {
            if (isRest(d)) { d--; continue }
            if (done.contains(d)) { n++; d-- } else break
        }
        return n
    }

    fun bestStreak(done: Set<Int>, total: Int, isRest: (Int) -> Boolean): Int {
        var best = 0; var cur = 0
        for (d in 1..total) {
            if (isRest(d)) continue
            if (done.contains(d)) { cur++; if (cur > best) best = cur } else cur = 0
        }
        return best
    }
}
