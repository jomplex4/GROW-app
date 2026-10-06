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
        Sprites.init(ctx)
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
                o.getInt("rp") == 1, poses, o.optString("es", o.getString("n"))
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

    /** rest after a step: -1 = the user's setting, 0..5 = fixed (side switch, continuous intervals), more = base seconds scaled by the setting */
    fun restFor(base: Int, setting: Int): Int = when {
        base == -1 -> setting
        base <= 5 -> base
        else -> Math.round(base * setting / 12f)
    }

    fun totalSeconds(steps: List<Step>, restSec: Int): Int {
        var t = 0
        for ((i, s) in steps.withIndex()) {
            t += s.sec
            if (i < steps.size - 1) t += restFor(s.rest, restSec)
        }
        return t
    }

    /** main stimulus of a session: jumping, strength circuits and the running intervals */
    fun isKey(s: Step): Boolean = s.tag == 'I' || s.tag == 'S' || (s.tag == 'R' && (s.ex.id == "jog" || s.ex.id == "run" || s.ex.id == "sprint"))

    fun keyMinutes(steps: List<Step>): Triple<Int, Int, Int> {
        var imp = 0; var str = 0; var run = 0
        for (s in steps) when {
            s.tag == 'I' -> imp += s.sec
            s.tag == 'S' -> str += s.sec
            s.tag == 'R' && isKey(s) -> run += s.sec
        }
        return Triple(Math.round(imp / 60f), Math.round(str / 60f), Math.round(run / 60f))
    }

    fun kcal(steps: List<Step>, weightKg: Float): Float {
        var k = 0f
        for (s in steps) k += s.ex.met * weightKg * (s.sec / 3600f)
        return k
    }

    fun sectionLabel(tag: Char): String = when (tag) {
        'W' -> "Warm-up"; 'I' -> "Impact"; 'R' -> "Run"; 'S' -> "Strength"; 'D' -> "Decompression"
        'C' -> "Core & posture"; 'F' -> "Flexibility"; 'J' -> "Jaw & neck"; else -> ""
    }

    class Quote(val text: String, val author: String)

    /** One quote per month, starting October 2026. Themes: universe, life, philosophy, growth mindset, business, investing, study, discipline. */
    val quotes = arrayOf(
        Quote("We are a way for the cosmos to know itself.", "Carl Sagan"),
        Quote("The unexamined life is not worth living.", "Socrates"),
        Quote("Becoming is better than being.", "Carol Dweck"),
        Quote("Spend each day trying to be a little wiser than you were when you woke up.", "Charlie Munger"),
        Quote("The journey of a thousand miles begins with a single step.", "Lao Tzu"),
        Quote("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
        Quote("Imagination is more important than knowledge.", "Albert Einstein"),
        Quote("The impediment to action advances action. What stands in the way becomes the way.", "Marcus Aurelius"),
        Quote("Risk comes from not knowing what you're doing.", "Warren Buffett"),
        Quote("You do not rise to the level of your goals. You fall to the level of your systems.", "James Clear"),
        Quote("Look up at the stars and not down at your feet.", "Stephen Hawking"),
        Quote("He who has a why to live can bear almost any how.", "Friedrich Nietzsche"),
        Quote("Stay hungry. Stay foolish.", "Steve Jobs"),
        Quote("The first principle is that you must not fool yourself, and you are the easiest person to fool.", "Richard Feynman"),
        Quote("We suffer more often in imagination than in reality.", "Seneca"),
        Quote("Someone is sitting in the shade today because someone planted a tree a long time ago.", "Warren Buffett"),
        Quote("Knowing is not enough; we must apply. Willing is not enough; we must do.", "Johann Wolfgang von Goethe"),
        Quote("Life is like riding a bicycle. To keep your balance, you must keep moving.", "Albert Einstein"),
        Quote("Men are disturbed not by things, but by the views they take of them.", "Epictetus"),
        Quote("The big money is not in the buying and selling, but in the waiting.", "Charlie Munger"),
        Quote("I've failed over and over and over again in my life. And that is why I succeed.", "Michael Jordan"),
        Quote("Nothing great was ever achieved without enthusiasm.", "Ralph Waldo Emerson"),
        Quote("It is not that we have a short time to live, but that we waste a lot of it.", "Seneca"),
        Quote("Do not let what you cannot do interfere with what you can do.", "John Wooden"),
        Quote("The investor's chief problem, and even his worst enemy, is likely to be himself.", "Benjamin Graham"),
        Quote("Grit is passion and perseverance for very long-term goals.", "Angela Duckworth"),
        Quote("Your time is limited, so don't waste it living someone else's life.", "Steve Jobs"),
        Quote("Earn with your mind, not your time.", "Naval Ravikant"),
        Quote("Waste no more time arguing what a good man should be. Be one.", "Marcus Aurelius"),
        Quote("I learned that courage was not the absence of fear, but the triumph over it.", "Nelson Mandela"),
        Quote("An investment in knowledge pays the best interest.", "Benjamin Franklin"),
        Quote("Be quick, but don't hurry.", "John Wooden"),
        Quote("First say to yourself what you would be; and then do what you have to do.", "Epictetus"),
        Quote("Do what you can, with what you have, where you are.", "Theodore Roosevelt"),
        Quote("If you have a garden and a library, you have everything you need.", "Cicero"),
        Quote("Somewhere, something incredible is waiting to be known.", "Carl Sagan"),
        Quote("The roots of education are bitter, but the fruit is sweet.", "Aristotle"),
        Quote("The beginning is the most important part of the work.", "Plato"),
        Quote("Nothing in life is to be feared, it is only to be understood.", "Marie Curie"),
        Quote("Success is a lousy teacher.", "Bill Gates"),
        Quote("The best investment you can make is in yourself.", "Warren Buffett"),
        Quote("The last of the human freedoms: to choose one's attitude in any given set of circumstances.", "Viktor Frankl"),
        Quote("The only way to do great work is to love what you do.", "Steve Jobs")
    )

    private fun monthIndex(day: Int): Int {
        val dt = dateOf(day.coerceAtLeast(1))
        return ((dt.year - 2026) * 12 + dt.monthValue - 10).coerceIn(0, quotes.size - 1)
    }
    fun quoteFor(day: Int): String = quotes[monthIndex(day)].text
    fun authorFor(day: Int): String = quotes[monthIndex(day)].author

    fun sectionEs(tag: Char): String = when (tag) {
        'W' -> "CALENTAMIENTO"; 'I' -> "IMPACTO"; 'R' -> "CARRERA"; 'S' -> "FUERZA"; 'D' -> "DESCOMPRESIÓN"
        'C' -> "CORE Y POSTURA"; 'F' -> "FLEXIBILIDAD"; 'J' -> "MANDÍBULA Y CUELLO"; else -> ""
    }

    fun focusOf(day: Int, jaw: Boolean): String {
        if (jaw) return "JAW & NECK MOBILITY"
        return when (dateOf(day).dayOfWeek.value) {
            1 -> "IMPACT + CORE"
            2 -> "RUN INTERVALS + MOBILITY"
            3 -> "STRENGTH + POSTURE"
            4 -> "IMPACT + HANG"
            5 -> "RUN + STRENGTH"
            7 -> "RECOVERY + MOBILITY"
            else -> "REST"
        }
    }

    private val coach = arrayOf(
        "Base phase. Jumps, runs and strength start at a moderate dose so your joints and habits adapt. Clean form first, then more.",
        "Progression phase. Jump sets get longer, strength circuits add a third round and the runs add pace changes.",
        "Build phase. Burpees and lunge jumps join the impact days, sprints appear in the runs and strength runs three rounds.",
        "Peak phase. This is your highest dose. Sleep, protein and the rest day decide how much you gain from it.",
        "Consolidation phase. Keep the strength and posture you built while the volume stays sustainable."
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
