import com.digitalminds.grow.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Random

var fails = 0
fun check(c: Boolean, msg: String) { if (!c) { fails++; if (fails <= 40) println("FAIL: $msg") } }

fun main() {
    // ---------------- calendar
    check(Data.TOTAL == 1292, "TOTAL ${Data.TOTAL}")
    check(Data.dateOf(1) == LocalDate.of(2026, 10, 5), "day 1 date")
    check(Data.dateOf(1).dayOfWeek == DayOfWeek.MONDAY, "day 1 is Monday")
    check(Data.dateOf(Data.TOTAL) == LocalDate.of(2030, 4, 18), "last day date ${Data.dateOf(Data.TOTAL)}")
    var rest = 0; var leapSeen = false
    for (d in 1..Data.TOTAL) {
        val dt = Data.dateOf(d)
        check(Data.dayOf(dt) == d, "dayOf(dateOf($d))")
        val sat = dt.dayOfWeek == DayOfWeek.SATURDAY
        check(Data.isRest(d) == sat, "isRest($d)")
        if (sat) rest++
        if (dt == LocalDate.of(2028, 2, 29)) leapSeen = true
        check(Data.focusOf(d, false).isNotEmpty() && Data.focusOf(d, true).isNotEmpty(), "focus $d")
        check(Data.coachNote(d).isNotEmpty() && Data.fuelTip(d).isNotEmpty(), "notes $d")
        val p = Data.phaseOf(d); check(p in 0..4, "phase $d")
        val qi = (dt.year - 2026) * 12 + dt.monthValue - 10
        check(qi in 0 until Data.quotes.size, "quote index $d = $qi")
        check(Data.quoteFor(d) == Data.quotes[qi].text && Data.authorFor(d).isNotEmpty(), "quote $d")
    }
    check(rest == 184, "rest days $rest")
    check(leapSeen, "29 Feb 2028 missing")
    check(Data.quotes.size == 43, "quotes ${Data.quotes.size}")
    check(Data.phaseOf(1) == 0 && Data.phaseOf(90) == 0 && Data.phaseOf(91) == 1 && Data.phaseOf(365) == 1 && Data.phaseOf(366) == 2 &&
          Data.phaseOf(730) == 2 && Data.phaseOf(731) == 3 && Data.phaseOf(1100) == 3 && Data.phaseOf(1101) == 4 && Data.phaseOf(Data.TOTAL) == 4, "phase boundaries")
    // before / after the programme, and extreme device dates
    check(Data.dayOf(LocalDate.of(2026, 10, 4)) == 0, "day before start")
    check(Data.dayOf(LocalDate.of(2030, 4, 19)) == 1293, "day after end")
    for (dt in listOf(LocalDate.of(2000, 1, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2099, 12, 31), LocalDate.of(2030, 4, 19))) {
        val d = Data.dayOf(dt)
        check(d < 1 || d > Data.TOTAL, "outside range $dt")
        // helpers used by the home screen when outside the range must not crash
        Data.quoteFor(d); Data.authorFor(d); Data.fuelTip(d)
    }

    // ---------------- streak logic against a brute force implementation
    val rnd = Random(7)
    for (trial in 0 until 400) {
        val done = HashSet<Int>()
        for (d in 1..Data.TOTAL) if (!Data.isRest(d) && rnd.nextInt(100) < (if (trial % 3 == 0) 97 else 80)) done.add(d)
        val today = 1 + rnd.nextInt(Data.TOTAL)
        // brute force
        var d = if (done.contains(today)) today else today - 1; var n = 0
        while (d >= 1) { if (Data.isRest(d)) { d--; continue }; if (done.contains(d)) { n++; d-- } else break }
        check(Logic.streak(done, today) { Data.isRest(it) } == n, "streak trial $trial")
        val best = Logic.bestStreak(done, Data.TOTAL) { Data.isRest(it) }
        check(best >= Logic.streak(done, today) { Data.isRest(it) }, "best >= current trial $trial")
    }
    val all = (1..Data.TOTAL).filter { !Data.isRest(it) }.toSet()
    check(Logic.streak(all, 100) { Data.isRest(it) } == (1..100).count { !Data.isRest(it) }, "perfect streak")
    check(Logic.bestStreak(all, Data.TOTAL) { Data.isRest(it) } == 1108, "perfect best")
    check(Logic.streak(emptySet(), 50) { Data.isRest(it) } == 0, "empty streak")
    check(Logic.streak(setOf(10), 1) { Data.isRest(it) } == 0, "future day done")

    // ---------------- sprite sequencing
    for (n in 1..12) for (alt in listOf(false, true)) {
        val b = Seq.beats(n, alt); val f = Seq.flips(n, alt)
        check(b.isNotEmpty() && b.size == f.size, "beats size n=$n alt=$alt")
        check(b.all { it in 0 until n }, "beat index range n=$n")
        if (alt) check(f.count { it } == b.size / 2, "alt flips half n=$n")
        else check(f.none { it }, "no flips n=$n")
    }
    check(Seq.beats(3, false).toList() == listOf(0, 1, 2, 1), "ping-pong 3")
    check(Seq.beats(9, false).size == 16, "ping-pong 9")
    check(Seq.beats(2, false).toList() == listOf(0, 1), "ping-pong 2")
    var prev = -1f
    for (k in 0..1000) {
        val p = Seq.smoothPos(k / 1000f)
        check(p in 0f..1f, "smoothPos range at $k")
    }
    check(Seq.smoothPos(0f) == 0f && Seq.smoothPos(0.5f) == 1f, "smoothPos ends")
    // travel is continuous (no jumps larger than 3% between 0.1% steps)
    var last = Seq.smoothPos(0f)
    for (k in 1..1000) { val p = Seq.smoothPos(k / 1000f); check(Math.abs(p - last) < 0.03f, "smoothPos jump at $k"); last = p }

    println("QA done, failures: $fails")
    if (fails > 0) System.exit(1)
}
