package com.digitalminds.grow

import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import java.time.LocalDate

class DotView(ctx: android.content.Context, val label: String, val done: Boolean, val isToday: Boolean, val restDay: Boolean) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val r = minOf(width, height) * 0.5f - 2f
        val cx = width / 2f; val cy = height / 2f
        p.style = Paint.Style.FILL
        p.color = if (done) C.RED else if (restDay) C.CARD else C.CARD2
        c.drawCircle(cx, cy, r, p)
        if (isToday && !done) { p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = C.RED; c.drawCircle(cx, cy, r, p) }
        p.style = Paint.Style.FILL; p.color = C.WHITE; p.textAlign = Paint.Align.CENTER; p.textSize = r * 0.9f; p.typeface = HEAD
        if (done) {
            p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.16f; p.strokeCap = Paint.Cap.ROUND
            val path = android.graphics.Path()
            path.moveTo(cx - r * .38f, cy + r * .02f); path.lineTo(cx - r * .1f, cy + r * .30f); path.lineTo(cx + r * .42f, cy - r * .28f)
            c.drawPath(path, p)
        } else {
            p.color = if (restDay) C.DIM else C.GRAY
            c.drawText(label, cx, cy + r * 0.32f, p)
        }
    }
}

class CompleteScreen(val act: MainActivity, val day: Int, val steps: List<Step>, val elapsedSec: Int, val jaw: Boolean) : Screen {
    override val view = FrameLayout(act)
    private var measure: MeasureCard? = null

    init {
        view.setBackgroundColor(C.BG)
        act.store.markDone(day, jaw)
        val streak = act.store.streak(day, jaw)
        val sv = ScrollView(act).apply { isVerticalScrollBarEnabled = false }
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(act.dp(20), act.dp(28), act.dp(20), act.dp(28)) }
        sv.addView(col)
        view.addView(sv, FrameLayout.LayoutParams(-1, -1))

        col.addView(FlameView(act), lp(act.dp(140), act.dp(150)))
        col.addView(act.tv(streak.toString(), 72f, C.WHITE, HEAD, Gravity.CENTER).also { it.margins(t = act.dp(6)) })
        col.addView(act.tv(if (streak == 1) "DAY STREAK" else "DAY STREAK", 14f, C.RED, HEAD, Gravity.CENTER).also { it.letterSpacing = 0.3f })
        col.addView(act.tv(if (jaw) "Jaw session complete" else "Day $day complete", 22f, C.WHITE, HEAD, Gravity.CENTER).also { it.margins(t = act.dp(18)) })
        val msgs = arrayOf("Another brick in the wall of your growth.", "Quiet work, loud results.", "You showed up. That is the whole secret.", "Stronger, taller, steadier.", "Consistency looks good on you.")
        col.addView(act.tv(msgs[day % msgs.size], 14f, C.GRAY, BODY, Gravity.CENTER).also { it.margins(t = act.dp(6)) })

        // week dots
        val wk = act.store.weekDone(LocalDate.now(), jaw)
        val todayIdx = LocalDate.now().dayOfWeek.value - 1
        val labels = arrayOf("M", "T", "W", "T", "F", "S", "S")
        val dots = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        for (i in 0..6) dots.addView(DotView(act, labels[i], wk[i], i == todayIdx, i == 5), lp(0, act.dp(38), 1f).also { it.setMargins(act.dp(4), 0, act.dp(4), 0) })
        col.addView(dots, lp(MATCH, WRAP).also { it.topMargin = act.dp(22) })

        val kc = Math.round(Data.kcal(steps, act.store.lastWeight()))
        val st = act.card(18)
        st.addView(act.statTriple(steps.size.toString() to "Exercises", kc.toString() to "kcal", fmtTime(elapsedSec) to "Time"))
        col.addView(st, lp(MATCH, WRAP).also { it.topMargin = act.dp(22) })

        if (!jaw) {
            col.addView(act.reminderCard(), lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })
            val m = MeasureCard(act); measure = m
            col.addView(m.view, lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })
        }
        val next = act.button("NEXT", C.RED, C.WHITE, 18f, 60, 30f).apply {
            setOnClickListener { measure?.save(); act.showHome() }
        }
        col.addView(next, lp(MATCH, act.dp(60)).also { it.topMargin = act.dp(22) })
    }

    override fun onBack(): Boolean { measure?.save(); act.showHome(); return true }
}
