package com.digitalminds.grow

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import java.time.LocalDate

class Row(val ex: Ex, val title: String, val seconds: Int, val tag: Char, val note: String, val mirror: Boolean = false)

class HomeScreen(val act: MainActivity) : Screen {
    override val view = FrameLayout(act)

    init {
        view.setBackgroundColor(C.BG)
        build()
    }

    private fun rowsOf(steps: List<Step>): List<Row> {
        val out = ArrayList<Row>()
        var i = 0
        while (i < steps.size) {
            val s = steps[i]
            if (s.tag == 'R') {
                var j = i; var tot = 0
                while (j < steps.size && steps[j].tag == 'R') { tot += steps[j].sec; j++ }
                val jog = Data.exercises.first { it.id == "jog" }
                val ex = if (steps.subList(i, j).any { it.ex.id == "jog" }) jog else s.ex
                out.add(Row(ex, if (j - i == 1) s.title else "RUN INTERVALS", tot, 'R', if (j - i == 1) "" else "walk, jog and run"))
                i = j
            } else if (s.ex.sided && !s.mirror && i + 1 < steps.size && steps[i + 1].ex === s.ex) {
                out.add(Row(s.ex, s.ex.name, s.sec, s.tag, "each side"))
                i += 2
            } else {
                out.add(Row(s.ex, s.title, s.sec, s.tag, "")); i++
            }
        }
        return out
    }

    private fun build() {
        view.removeAllViews()
        val day = act.today
        val jaw = act.jawMode
        val steps = if (day in 1..Data.TOTAL) Data.steps(day, jaw) else emptyList()
        val sv = ScrollView(act).apply { isVerticalScrollBarEnabled = false; clipToPadding = false; setPadding(0, 0, 0, act.dp(110)) }
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(20), act.dp(18), act.dp(20), act.dp(24)) }
        sv.addView(col)
        view.addView(sv, FrameLayout.LayoutParams(-1, -1))

        // header
        val head = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = android.widget.ImageView(act).apply { setImageResource(R.drawable.dm_logo); adjustViewBounds = true; scaleType = android.widget.ImageView.ScaleType.FIT_START }
        head.addView(logo, lp(0, act.dp(26), 1f).also { it.rightMargin = act.dp(60) })
        val chart = IconView(act, Ic.CHART).apply { setOnClickListener { act.showProgress() } }
        val gear = IconView(act, Ic.GEAR).apply { setOnClickListener { act.showSettings() } }
        head.addView(chart, lp(act.dp(44), act.dp(44)))
        head.addView(gear, lp(act.dp(44), act.dp(44)))
        col.addView(head)

        // tabs
        val tabs = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        fun tab(label: String, sel: Boolean, target: Boolean) = act.tv(label, 13f, if (sel) C.WHITE else C.GRAY, HEAD, Gravity.CENTER).apply {
            letterSpacing = 0.12f
            background = act.ripple(act.round(if (sel) C.RED else C.CARD, 20f))
            setOnClickListener { if (act.jawMode != target) { act.jawMode = target; build() } }
        }
        tabs.addView(tab("GROWTH", !jaw, false), lp(0, act.dp(40), 1f).also { it.rightMargin = act.dp(8) })
        tabs.addView(tab("JAW & NECK", jaw, true), lp(0, act.dp(40), 1f))
        col.addView(tabs.also { it.margins(t = act.dp(16)) })

        // hero
        val hero = act.card(20)
        val rest = day in 1..Data.TOTAL && Data.isRest(day)
        val done = day in 1..Data.TOTAL && act.store.isDone(day, jaw)
        when {
            day < 1 -> {
                val left = (1 - day)
                hero.addView(act.tv(if (left == 1) "Starts tomorrow" else "Starts in $left days", 32f, C.WHITE, HEAD))
                hero.addView(act.tv("Your program begins on Monday, October 5, 2026. Rest, eat well and get ready.", 14f, C.GRAY, BODY).also { it.margins(t = act.dp(8)) })
            }
            day > Data.TOTAL -> {
                hero.addView(act.tv("Program complete", 30f, C.WHITE, HEAD))
                hero.addView(act.tv("You finished all ${Data.TOTAL} days. Keep your habits alive.", 14f, C.GRAY, BODY).also { it.margins(t = act.dp(8)) })
            }
            else -> {
                val week = (day - 1) / 7 + 1
                hero.addView(act.tv(if (rest) "Rest day" else "Day $day", 38f, C.WHITE, HEAD))
                hero.addView(act.tv("${Data.phaseNames[Data.phaseOf(day)]} PHASE  ·  WEEK $week" + (if (jaw) "  ·  JAW" else ""), 12f, C.RED, HEAD).also { it.letterSpacing = 0.14f; it.margins(t = act.dp(6)) })
                if (!rest) hero.addView(act.tv(Data.focusOf(day, jaw), 13f, C.WHITE, HEAD).also { it.letterSpacing = 0.1f; it.margins(t = act.dp(10)) })
                val q = if (rest) "Your body grows while you recover. Sleep well and eat well." else Data.quoteFor(day)
                hero.addView(act.tv("\u201C$q\u201D", 14f, C.GRAY, Typeface.create(BODY, Typeface.ITALIC)).also { it.margins(t = act.dp(14)); it.setLineSpacing(act.dp(3).toFloat(), 1f) })
            }
        }
        col.addView(hero.also { it.margins(t = act.dp(14)) })

        if (steps.isNotEmpty()) {
            val rows = rowsOf(steps)
            val mins = Math.round(Data.totalSeconds(steps, act.store.restSeconds) / 60f)
            val kc = Math.round(Data.kcal(steps, act.store.lastWeight()))
            val st = act.card(18)
            st.addView(act.statTriple(rows.size.toString() to "Exercises", mins.toString() to "Minutes", kc.toString() to "kcal"))
            col.addView(st.also { it.margins(t = act.dp(12)) })

            if (!jaw) {
                val cn = act.card(16)
                cn.addView(act.tv("COACH NOTE", 11f, C.RED, HEAD).also { it.letterSpacing = 0.16f })
                cn.addView(act.tv(Data.coachNote(day), 13f, C.GRAY, BODY).also { it.margins(t = act.dp(6)); it.setLineSpacing(act.dp(3).toFloat(), 1f) })
                col.addView(cn.also { it.margins(t = act.dp(12)) })
            }
            col.addView(act.tv("Exercises", 20f, C.WHITE, HEAD).also { it.margins(t = act.dp(22), b = act.dp(6)) })
            var lastTag = ' '
            for (r in rows) {
                if (r.tag != lastTag) {
                    col.addView(act.tv(Data.sectionLabel(r.tag).uppercase(), 11f, C.RED, HEAD).also { it.letterSpacing = 0.16f; it.margins(t = act.dp(14), b = act.dp(6)) })
                    lastTag = r.tag
                }
                col.addView(rowView(r))
            }
        } else if (rest) {
            col.addView(act.tv("Come back tomorrow, fresh and tall.", 15f, C.GRAY, BODY, Gravity.CENTER).also { it.margins(t = act.dp(28)) }, lp(MATCH, WRAP))
        }
        if (day in 1..Data.TOTAL) {
            val fc = act.card(16)
            fc.addView(act.tv("FUEL & RECOVERY", 11f, C.RED, HEAD).also { it.letterSpacing = 0.16f })
            fc.addView(act.tv(Data.fuelTip(day), 13f, C.GRAY, BODY).also { it.margins(t = act.dp(6)); it.setLineSpacing(act.dp(3).toFloat(), 1f) })
            col.addView(fc.also { it.margins(t = act.dp(20)) })
        }

        // start
        if (steps.isNotEmpty()) {
            val label = if (done) "DO IT AGAIN" else "START"
            val b = act.button(label, if (done) C.CARD2 else C.RED, C.WHITE, 18f, 60, 30f).apply {
                setOnClickListener { act.startWorkout(day, steps) }
            }
            view.addView(b, fl(MATCH, act.dp(60), Gravity.BOTTOM).also { it.setMargins(act.dp(20), 0, act.dp(20), act.dp(20)) })
            if (done) col.addView(act.tv("Completed today", 13f, C.RED, HEAD, Gravity.CENTER).also { it.margins(t = act.dp(14)) }, lp(MATCH, WRAP))
        }
    }

    private fun rowView(r: Row): View {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = act.ripple(act.round(C.CARD, 18f)); setPadding(act.dp(10), act.dp(8), act.dp(16), act.dp(8))
        }
        val fig = FigureView(act).apply { ex = r.ex; still = r.ex.repPos(); mirror = r.mirror; radiusDp = 14f }
        row.addView(fig, lp(act.dp(76), act.dp(88)))
        val mid = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(act.tv(r.title, 15f, C.WHITE, HEAD).also { it.letterSpacing = 0.03f })
        if (r.note.isNotEmpty()) mid.addView(act.tv(r.note, 12f, C.DIM, BODY).also { it.margins(t = act.dp(3)) })
        row.addView(mid, lp(0, WRAP, 1f).also { it.leftMargin = act.dp(8) })
        row.addView(act.tv(fmtTime(r.seconds), 15f, C.RED, HEAD))
        row.setOnClickListener { InfoSheet.show(act, r.ex, r.mirror) }
        row.layoutParams = lp(MATCH, WRAP).also { it.bottomMargin = act.dp(8) }
        return row
    }
}
