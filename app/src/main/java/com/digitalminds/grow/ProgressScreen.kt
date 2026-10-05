package com.digitalminds.grow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ChartView(ctx: Context) : View(ctx) {
    var am: List<Pair<Long, Float>> = emptyList()
    var pm: List<Pair<Long, Float>> = emptyList()
    var unit = "cm"
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val all = am + pm
        val w = width.toFloat(); val h = height.toFloat()
        p.textSize = 11f * resources.displayMetrics.scaledDensity; p.typeface = BODY
        if (all.isEmpty()) {
            p.color = C.DIM; p.textAlign = Paint.Align.CENTER
            c.drawText("No measurements yet", w / 2, h / 2, p); return
        }
        val padL = 46f * resources.displayMetrics.density; val padB = 22f * resources.displayMetrics.density
        val pad = 10f * resources.displayMetrics.density
        var lo = all.minOf { it.second }; var hi = all.maxOf { it.second }
        if (hi - lo < 1f) { lo -= 0.5f; hi += 0.5f }
        val span = hi - lo; lo -= span * 0.15f; hi += span * 0.15f
        val x0 = all.minOf { it.first }; val x1 = all.maxOf { it.first }
        val gx0 = padL; val gx1 = w - pad; val gy0 = pad; val gy1 = h - padB
        fun X(d: Long) = if (x1 == x0) (gx0 + gx1) / 2 else gx0 + (d - x0).toFloat() / (x1 - x0) * (gx1 - gx0)
        fun Y(v: Float) = gy1 - (v - lo) / (hi - lo) * (gy1 - gy0)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f; p.color = 0xFF2A2B2F.toInt()
        for (i in 0..3) {
            val v = lo + (hi - lo) * i / 3f; val y = Y(v)
            c.drawLine(gx0, y, gx1, y, p)
            p.style = Paint.Style.FILL; p.color = C.DIM; p.textAlign = Paint.Align.RIGHT
            c.drawText("%.1f".format(v), gx0 - 8f, y + 8f, p)
            p.style = Paint.Style.STROKE; p.color = 0xFF2A2B2F.toInt()
        }
        fun series(d: List<Pair<Long, Float>>, color: Int) {
            if (d.isEmpty()) return
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = color; p.strokeCap = Paint.Cap.ROUND
            path.reset()
            for ((i, e) in d.withIndex()) { val x = X(e.first); val y = Y(e.second); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
            if (d.size > 1) c.drawPath(path, p)
            p.style = Paint.Style.FILL
            for (e in d) c.drawCircle(X(e.first), Y(e.second), 6f, p)
        }
        series(pm, C.GRAY)
        series(am, C.RED)
    }
}

class ProgressScreen(val act: MainActivity) : Screen {
    override val view = FrameLayout(act)
    private var showWeight = false
    private lateinit var chart: ChartView
    private lateinit var summary: TextView
    private lateinit var tabH: TextView
    private lateinit var tabW: TextView
    private val measure = MeasureCard(act)

    init {
        view.setBackgroundColor(C.BG)
        val sv = ScrollView(act).apply { isVerticalScrollBarEnabled = false }
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(20), act.dp(10), act.dp(20), act.dp(30)) }
        sv.addView(col); view.addView(sv, FrameLayout.LayoutParams(-1, -1))

        val head = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(IconView(act, Ic.BACK).apply { setOnClickListener { act.showHome() } }, lp(act.dp(48), act.dp(48)))
        head.addView(act.tv("PROGRESS", 20f, C.WHITE, HEAD).also { it.letterSpacing = 0.18f })
        col.addView(head)

        val tot = act.store.totalDone(false)
        val s = act.card(18)
        s.addView(act.statTriple(act.store.streak(act.today, false).toString() to "Streak", act.store.bestStreak(false).toString() to "Best", tot.toString() to "Sessions"))
        s.addView(act.tv("Jaw & neck sessions: ${act.store.totalDone(true)}", 12f, C.DIM, BODY, Gravity.CENTER).also { it.margins(t = act.dp(12)) }, lp(MATCH, WRAP))
        col.addView(s, lp(MATCH, WRAP).also { it.topMargin = act.dp(8) })

        val cc = act.card(16)
        val tabs = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        tabH = chip("HEIGHT") { showWeight = false; refreshChart() }
        tabW = chip("WEIGHT") { showWeight = true; refreshChart() }
        tabs.addView(tabH, lp(0, act.dp(36), 1f).also { it.rightMargin = act.dp(8) }); tabs.addView(tabW, lp(0, act.dp(36), 1f))
        cc.addView(tabs)
        chart = ChartView(act)
        cc.addView(chart, lp(MATCH, act.dp(190)).also { it.topMargin = act.dp(12) })
        val legend = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        legend.addView(act.tv("●  Morning", 12f, C.RED, BODY)); legend.addView(act.tv("●  Afternoon", 12f, C.DIM, BODY).also { it.margins(l = act.dp(18)) })
        cc.addView(legend, lp(MATCH, WRAP).also { it.topMargin = act.dp(8) })
        summary = act.tv("", 13f, C.GRAY, BODY, Gravity.CENTER)
        cc.addView(summary, lp(MATCH, WRAP).also { it.topMargin = act.dp(10) })
        col.addView(cc, lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })
        refreshChart()

        col.addView(measure.view, lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })
        col.addView(act.button("SAVE MEASUREMENT", C.RED, C.WHITE, 15f, 52, 26f).apply {
            setOnClickListener { measure.touched = true; measure.save(); refreshChart() }
        }, lp(MATCH, act.dp(52)).also { it.topMargin = act.dp(12) })
        col.addView(act.reminderCard(), lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })

        val about = act.card(18)
        about.addView(act.tv("About GROW", 16f, C.WHITE, MED))
        about.addView(act.tv("GROW builds the habits that support your best posture and frame: spinal decompression, impact for bone strength, core and posture work, and flexibility.\n\nExercise can recover the small height lost to daily spinal compression and improve posture, but it cannot override genetics or the point where growth plates close. A pediatrician or endocrinologist can check your bone age with a simple X-ray and tell you how much room is left to grow. Sleep 8 to 10 hours and eat enough protein, calcium and vitamin D.\n\nThe jaw and neck routine improves neck posture and jaw mobility. It cannot reshape facial bone, so stop if you feel pain or hear clicking.", 13f, C.GRAY, BODY).also { it.margins(t = act.dp(8)); it.setLineSpacing(act.dp(3).toFloat(), 1f) })
        col.addView(about, lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })
    }

    private fun chip(label: String, click: () -> Unit) = act.tv(label, 12f, C.WHITE, HEAD, Gravity.CENTER).apply {
        letterSpacing = 0.12f; setOnClickListener { click() }
    }

    private fun refreshChart() {
        tabH.background = act.ripple(act.round(if (!showWeight) C.RED else C.CARD2, 18f))
        tabW.background = act.ripple(act.round(if (showWeight) C.RED else C.CARD2, 18f))
        val logs = act.store.logs().takeLast(80)
        fun v(l: Log) = if (showWeight) l.weight else l.height
        chart.am = logs.filter { it.slot == 0 }.map { it.epochDay to v(it) }
        chart.pm = logs.filter { it.slot == 1 }.map { it.epochDay to v(it) }
        chart.unit = if (showWeight) "kg" else "cm"
        chart.invalidate()
        val first = logs.firstOrNull(); val lastL = logs.lastOrNull()
        summary.text = if (first == null || lastL == null) "Add your first measurement below." else {
            val d = v(lastL) - v(first)
            "Latest %.1f %s  ·  change since first entry %s%.1f %s".format(v(lastL), chart.unit, if (d >= 0) "+" else "", d, chart.unit)
        }
    }

    override fun onBack(): Boolean { act.showHome(); return true }
}

class SettingsScreen(val act: MainActivity) : Screen {
    override val view = FrameLayout(act)

    init {
        view.setBackgroundColor(C.BG)
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; setPadding(act.dp(20), act.dp(10), act.dp(20), act.dp(20)) }
        view.addView(col, FrameLayout.LayoutParams(-1, -1))
        val head = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(IconView(act, Ic.BACK).apply { setOnClickListener { act.showHome() } }, lp(act.dp(48), act.dp(48)))
        head.addView(act.tv("SETTINGS", 20f, C.WHITE, HEAD).also { it.letterSpacing = 0.18f })
        col.addView(head)

        // voice
        val v = act.card(18)
        val vr = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val vl = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        vl.addView(act.tv("Voice guide", 17f, C.WHITE, MED))
        vl.addView(act.tv("Announces exercises, halfway and the final countdown.", 12f, C.DIM, BODY).also { it.margins(t = act.dp(4)) })
        vr.addView(vl, lp(0, WRAP, 1f))
        val sw = android.widget.Switch(act).apply {
            isChecked = act.store.voice
            thumbTintList = android.content.res.ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(C.RED, C.GRAY))
            trackTintList = android.content.res.ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(0x66FF2800, C.CARD2))
            setOnCheckedChangeListener { _, on -> act.store.voice = on; act.speaker.enabled = on; if (on) act.speaker.say("Voice on") else act.speaker.stop() }
        }
        vr.addView(sw)
        v.addView(vr)
        col.addView(v, lp(MATCH, WRAP).also { it.topMargin = act.dp(10) })

        // rest
        val r = act.card(18)
        r.addView(act.tv("Rest duration", 17f, C.WHITE, MED))
        val opts = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        val names = arrayOf("SHORT", "STANDARD", "LONG"); val secs = arrayOf("8s", "12s", "20s")
        val chips = ArrayList<TextView>()
        fun paint() { for ((i, c) in chips.withIndex()) { val sel = act.store.restLevel == i; c.background = act.ripple(act.round(if (sel) C.RED else C.CARD2, 22f)); c.setTextColor(if (sel) C.WHITE else C.GRAY) } }
        for (i in 0..2) {
            val c = act.tv("${names[i]}\n${secs[i]}", 12f, C.GRAY, HEAD, Gravity.CENTER).apply { letterSpacing = 0.08f; setLineSpacing(act.dp(2).toFloat(), 1f) }
            c.setOnClickListener { act.store.restLevel = i; paint() }
            chips.add(c)
            opts.addView(c, lp(0, act.dp(62), 1f).also { it.rightMargin = if (i < 2) act.dp(8) else 0 })
        }
        paint()
        r.addView(opts, lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })
        col.addView(r, lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })
        col.addView(act.tv("GROW  ·  DIGITALMINDS", 11f, C.DIM, HEAD, Gravity.CENTER).also { it.letterSpacing = 0.2f }, lp(MATCH, WRAP).also { it.topMargin = act.dp(30) })
    }

    override fun onBack(): Boolean { act.showHome(); return true }
}
