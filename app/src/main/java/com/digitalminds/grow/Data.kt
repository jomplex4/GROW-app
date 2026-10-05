package com.digitalminds.grow

import android.content.Context
import org.json.JSONArray
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object Data {
    val START: LocalDate = LocalDate.of(2026, 10, 5)
    val END: LocalDate = LocalDate.of(2030, 4, 18)
    val TOTAL: Int = (ChronoUnit.DAYS.between(START, END) + 1).toInt()

    lateinit var exercises: Array<Ex>
    private lateinit var plan: List<String>
    private lateinit var jaw: List<String>
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        val arr = JSONArray(ctx.assets.open("exercises.json").bufferedReader().use { it.readText() })
        exercises = Array(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val tips = o.getJSONArray("t").let { t -> List(t.length()) { t.getString(it) } }
            val ps = o.getJSONArray("ps")
            val poses = Array(ps.length()) { k ->
                val a = ps.getJSONArray(k)
                FloatArray(a.length()) { a.getDouble(it).toFloat() }
            }
            val mat = if (o.isNull("mat")) null else o.getJSONArray("mat")
            Ex(
                o.getString("id"), o.getString("n"), o.getString("d"), tips, o.getString("p"),
                o.getDouble("m").toFloat(), o.getInt("sd") == 1, o.getDouble("lp").toFloat(), o.getInt("ez"),
                o.getInt("fr") == 1, o.getInt("bu") == 1, o.getDouble("sc").toFloat(),
                o.getDouble("bar").toFloat(), o.getDouble("wall").toFloat(),
                mat?.getDouble(0)?.toFloat() ?: 0f, mat?.getDouble(1)?.toFloat() ?: 0f, mat != null,
                o.getInt("rp") == 1, poses
            )
        }
        plan = ctx.assets.open("plan.txt").bufferedReader().use { it.readLines() }
        jaw = ctx.assets.open("jaw.txt").bufferedReader().use { it.readLines() }
        loaded = true
    }

    /** Day number (1..TOTAL) for a date; <1 means before start, >TOTAL after end. */
    fun dayOf(date: LocalDate): Int = (ChronoUnit.DAYS.between(START, date) + 1).toInt()
    fun dateOf(day: Int): LocalDate = START.plusDays((day - 1).toLong())

    fun phaseOf(day: Int): Int = when {
        day <= 90 -> 0; day <= 365 -> 1; day <= 730 -> 2; day <= 1100 -> 3; else -> 4
    }
    val phaseNames = arrayOf("BASE", "PROGRESSION", "BUILD", "PEAK", "CONSOLIDATION")

    fun isRest(day: Int): Boolean = dateOf(day).dayOfWeek.value == 6

    fun steps(day: Int, jawMode: Boolean): List<Step> {
        if (day < 1 || day > TOTAL) return emptyList()
        val line = (if (jawMode) jaw else plan)[day - 1]
        if (line == "R" || line.isEmpty()) return emptyList()
        return line.split(',').map {
            val p = it.split(':')
            Step(exercises[p[0].toInt()], p[1].toInt(), p[2].toInt(), p[3] == "1", p[4][0])
        }
    }

    fun totalSeconds(steps: List<Step>, restSec: Int): Int {
        var t = 0
        for ((i, s) in steps.withIndex()) {
            t += s.sec
            if (i < steps.size - 1) t += if (s.rest == -1) restSec else s.rest
        }
        return t
    }

    fun kcal(steps: List<Step>, weightKg: Float): Float {
        var k = 0f
        for (s in steps) k += s.ex.met * weightKg * (s.sec / 3600f)
        return k
    }

    fun sectionLabel(tag: Char): String = when (tag) {
        'W' -> "Warm-up"; 'I' -> "Impact"; 'R' -> "Run"; 'D' -> "Decompression"
        'C' -> "Core & posture"; 'F' -> "Flexibility"; 'J' -> "Jaw & neck"; else -> ""
    }

    val quotes = arrayOf(
        "Stand tall in small moments and the big ones will follow.",
        "Patience is a quiet kind of strength.",
        "Today's effort is tomorrow's posture.",
        "Grow like a tree: slowly, deeply, without apology.",
        "Discipline is a promise you keep when no one is watching.",
        "Small steps still leave footprints.",
        "Rest is not the opposite of effort, it is its partner.",
        "Be the person your future self thanks.",
        "Strength grows where comfort ends.",
        "A calm mind carries a tall spine.",
        "Every repetition is a vote for who you are becoming.",
        "Consistency turns ordinary days into extraordinary years.",
        "Pause to breathe, never to surrender.",
        "Reach higher, root deeper.",
        "The body keeps the score of every honest effort.",
        "You do not need motivation, only the next minute.",
        "Quiet work builds loud results.",
        "Be gentle with the process, firm with the habit.",
        "Growth is the sum of days nobody applauds.",
        "Your only rival is yesterday's version of you.",
        "Stillness is also a way of moving forward.",
        "Show up, even when it is small.",
        "Height of body, height of character.",
        "What you repeat, you become.",
        "Let your effort be steady and your spirit light.",
        "A long road is walked one stride at a time.",
        "Discipline is freedom wearing work clothes.",
        "Breathe in patience, breathe out doubt.",
        "Strong foundations are built unseen.",
        "Do it for the person you will be in ten years.",
        "The best session is the one you finish.",
        "Rise early in spirit, even if the clock disagrees.",
        "Progress whispers; keep listening.",
        "Make today's version of effort count.",
        "A tall life starts with a straight back.",
        "Sleep well, eat well, train well, repeat.",
        "Courage is just consistency with a heartbeat.",
        "The mountain is climbed by those who keep walking.",
        "Respect the rest day, it is where you grow.",
        "Be proud of the work, not only the result.",
        "Even slow growth is growth.",
        "Your habits are quietly writing your story.",
        "Move with purpose, recover with intention.",
        "Be steady like roots and open like branches.",
        "Today is a good day to be a little better.",
        "Effort is a language everyone understands.",
        "Hold your head high, but keep your heart humble.",
        "The strongest lift is the first step out the door.",
        "Nothing great is built in a hurry.",
        "Keep going, the view improves with every climb.",
        "You are not behind, you are becoming."
    )
    fun quoteFor(day: Int): String = quotes[(((day - 1) / 7) % quotes.size)]

    fun focusOf(day: Int, jaw: Boolean): String {
        if (jaw) return "JAW & NECK MOBILITY"
        return when (dateOf(day).dayOfWeek.value) {
            1 -> "IMPACT + CORE"
            2 -> "RUN INTERVALS + MOBILITY"
            3 -> "POSTURE + DECOMPRESSION"
            4 -> "IMPACT + HANG"
            5 -> "RUN + CORE + STRETCH"
            7 -> "RECOVERY + MOBILITY"
            else -> "REST"
        }
    }

    private val coach = arrayOf(
        "Base phase. Learn each movement with clean form. Quality beats speed, and the habit matters most right now.",
        "Progression phase. Holds get longer and impact becomes regular. Keep your landings soft and your spine tall.",
        "Build phase. Intervals get longer and the core work gets harder. Recovery weeks keep your joints happy.",
        "Peak phase. This is your highest load. Sleep, protein and rest days decide how much you gain from it.",
        "Consolidation phase. Hold your strength and posture while keeping the volume sustainable."
    )
    fun coachNote(day: Int): String = coach[phaseOf(day)]

    private val fuel = arrayOf(
        "Sleep 8 to 10 hours. Most growth hormone is released in deep sleep, so a steady bedtime counts as training.",
        "Include protein in every meal: eggs, fish, chicken, lentils, quinoa or milk. Your bones and muscles are built from it.",
        "Calcium needs are highest in your teens (about 1300 mg a day). Milk, yogurt, cheese and quinoa all help.",
        "Vitamin D helps your bones use calcium. Get some morning sun and ask a doctor about your level.",
        "Drink water through the day. Spinal discs are mostly water, and hydration helps them recover overnight.",
        "Eat enough. Growing and training need fuel, and skipping meals works against both.",
        "Avoid long stretches of sitting. Stand and reset your posture every 30 to 40 minutes.",
        "Eat something with carbs and protein within an hour after training to recover faster.",
        "Keep screens away from your last hour before bed. Better sleep means better recovery.",
        "Measure your height at the same time each morning, right after waking, for the most reliable number.",
        "Carry your bag on both shoulders and keep it light. Your spine spends all day under that load.",
        "A bone age X-ray with a pediatrician shows how much growing room is left. Knowledge beats guessing."
    )
    fun fuelTip(day: Int): String = fuel[(day.coerceAtLeast(1) - 1) % fuel.size]
}
