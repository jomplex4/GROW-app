package com.digitalminds.grow

import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class BarView(ctx: Context, val trackColor: Int, val fillColor: Int) : View(ctx) {
    var fraction = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val h = height.toFloat(); val w = width.toFloat()
        p.color = trackColor; c.drawRoundRect(RectF(0f, 0f, w, h), h / 2, h / 2, p)
        if (fraction > 0f) { p.color = fillColor; c.drawRoundRect(RectF(0f, 0f, maxOf(h, w * fraction), h), h / 2, h / 2, p) }
    }
}

class WorkoutScreen(val act: MainActivity, val day: Int, val steps: List<Step>, val jaw: Boolean) : Screen {
    override val view = FrameLayout(act)

    private enum class St { READY, WORK, REST }
    private var st = St.READY
    private var idx = 0
    private var paused = false
    private var endAt = 0L
    private var remMs = 0L
    private var totalMs = 1L
    private var lastTick = 0L
    private var activeMs = 0L
    private var lastSec = -1
    private var halfSaid = false
    private val handler = Handler(Looper.getMainLooper())
    private var dead = false
    private val tickR = Runnable { tick() }

    // views
    private val work = LinearLayout(act)
    private val rest = LinearLayout(act)
    private val segs = ArrayList<BarView>()
    private val counter: TextView
    private val fig = FigureView(act)
    private val nameTv: TextView
    private val labelTv: TextView
    private val timeTv: TextView
    private val bar = BarView(act, C.CARD2, C.REDB)
    private val pauseBtn: TextView
    private val restTime: TextView
    private val restNext: TextView
    private val restNextTime: TextView
    private val restCount: TextView
    private val restFig = FigureView(act)
    private val restQuote: TextView

    init {
        view.setBackgroundColor(C.BG)
        // ---------------- WORK layout
        work.orientation = LinearLayout.VERTICAL
        work.setPadding(0, act.dp(8), 0, act.dp(12))
        val top = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(IconView(act, Ic.BACK).apply { setOnClickListener { askQuit() } }, lp(act.dp(48), act.dp(48)))
        val segRow = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        for (i in steps.indices) {
            val b = BarView(act, C.CARD2, C.RED); segs.add(b)
            segRow.addView(b, lp(0, act.dp(4), 1f).also { it.rightMargin = if (i < steps.size - 1) act.dp(2) else 0 })
        }
        top.addView(segRow, lp(0, act.dp(4), 1f))
        counter = act.tv("", 13f, C.GRAY, HEAD, Gravity.CENTER).apply { minWidth = act.dp(56) }
        top.addView(counter)
        work.addView(top, lp(MATCH, act.dp(48)))

        val stage = FrameLayout(act)
        fig.radiusDp = 26f
        stage.addView(fig, fl(MATCH, MATCH))
        val help = act.tv("?", 20f, C.WHITE, HEAD, Gravity.CENTER).apply {
            background = act.ripple(act.round(0x66000000, 22f)); setOnClickListener { showInfo() }
        }
        stage.addView(help, fl(act.dp(44), act.dp(44), Gravity.TOP or Gravity.END).also { it.setMargins(0, act.dp(12), act.dp(12), 0) })
        work.addView(stage, lp(MATCH, 0, 1f).also { it.setMargins(act.dp(16), act.dp(8), act.dp(16), 0) })

        nameTv = act.tv("", 24f, C.WHITE, HEAD, Gravity.CENTER).apply { letterSpacing = 0.04f }
        work.addView(nameTv, lp(MATCH, WRAP).also { it.topMargin = act.dp(16) })
        labelTv = act.tv("", 12f, C.RED, HEAD, Gravity.CENTER).apply { letterSpacing = 0.2f }
        work.addView(labelTv, lp(MATCH, WRAP).also { it.topMargin = act.dp(6) })

        val panel = FrameLayout(act).apply { background = act.round(C.CARD, 24f) }
        timeTv = act.tv("00:00", 68f, C.WHITE, HEAD, Gravity.CENTER)
        panel.addView(timeTv, fl(MATCH, WRAP, Gravity.CENTER).also { it.bottomMargin = act.dp(8) })
        panel.addView(bar, fl(MATCH, act.dp(6), Gravity.BOTTOM).also { it.setMargins(act.dp(20), 0, act.dp(20), act.dp(16)) })
        work.addView(panel, lp(MATCH, act.dp(124)).also { it.setMargins(act.dp(16), act.dp(10), act.dp(16), 0) })

        pauseBtn = act.button("PAUSE", C.RED, C.WHITE, 17f, 58, 29f).apply { setOnClickListener { togglePause() } }
        work.addView(pauseBtn, lp(MATCH, act.dp(58)).also { it.setMargins(act.dp(16), act.dp(14), act.dp(16), 0) })

        val nav = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        fun navBtn(label: String, ic: Ic, onClick: () -> Unit) = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            background = act.ripple(act.round(0x00000000, 20f))
            addView(IconView(act, ic, C.GRAY), lp(act.dp(28), act.dp(28)))
            addView(act.tv(label, 14f, C.GRAY, HEAD).also { it.letterSpacing = 0.06f; it.margins(l = act.dp(6)) })
            setOnClickListener { onClick() }
        }
        nav.addView(navBtn("Previous", Ic.PREV) { prev() }, lp(0, act.dp(44), 1f))
        nav.addView(navBtn("Skip", Ic.NEXT) { skip() }, lp(0, act.dp(44), 1f))
        work.addView(nav, lp(MATCH, act.dp(44)).also { it.topMargin = act.dp(4) })

        // ---------------- REST layout
        rest.orientation = LinearLayout.VERTICAL
        rest.setBackgroundColor(C.REDD)
        rest.setPadding(0, act.dp(8), 0, act.dp(16))
        val rtop = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        rtop.addView(IconView(act, Ic.BACK).apply { setOnClickListener { askQuit() } }, lp(act.dp(48), act.dp(48)))
        rest.addView(rtop, lp(MATCH, act.dp(48)))
        restQuote = act.tv("", 15f, 0xFFFFFFFF.toInt(), Typeface.create(BODY, Typeface.ITALIC), Gravity.CENTER).apply { setLineSpacing(act.dp(3).toFloat(), 1f) }
        rest.addView(restQuote, lp(MATCH, WRAP).also { it.setMargins(act.dp(28), act.dp(4), act.dp(28), 0) })
        rest.addView(act.tv("REST", 18f, 0xFFFFFFFF.toInt(), HEAD, Gravity.CENTER).also { it.letterSpacing = 0.35f }, lp(MATCH, WRAP).also { it.topMargin = act.dp(22) })
        restTime = act.tv("00:00", 96f, 0xFFFFFFFF.toInt(), HEAD, Gravity.CENTER)
        rest.addView(restTime, lp(MATCH, WRAP).also { it.topMargin = act.dp(6) })
        val rb = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val plus = act.button("+20s", 0x40FFFFFF, C.WHITE, 16f, 48, 24f).apply { setOnClickListener { addTime(20000) } }
        val skip = act.button("SKIP", C.WHITE, C.REDD, 16f, 48, 24f).apply { setOnClickListener { skip() } }
        rb.addView(plus, lp(act.dp(120), act.dp(48)).also { it.rightMargin = act.dp(14) })
        rb.addView(skip, lp(act.dp(120), act.dp(48)))
        rest.addView(rb, lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })
        val nx = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val nl = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        restCount = act.tv("", 12f, 0xCCFFFFFF.toInt(), HEAD).apply { letterSpacing = 0.16f }
        restNext = act.tv("", 20f, C.WHITE, HEAD).apply { letterSpacing = 0.03f }
        nl.addView(restCount); nl.addView(restNext.also { it.margins(t = act.dp(4)) })
        nx.addView(nl, lp(0, WRAP, 1f))
        restNextTime = act.tv("", 18f, C.WHITE, HEAD)
        nx.addView(restNextTime)
        rest.addView(nx, lp(MATCH, WRAP).also { it.setMargins(act.dp(24), act.dp(22), act.dp(24), 0) })
        val rstage = FrameLayout(act)
        restFig.radiusDp = 26f
        rstage.addView(restFig, fl(MATCH, MATCH))
        rest.addView(rstage, lp(MATCH, 0, 1f).also { it.setMargins(act.dp(16), act.dp(14), act.dp(16), 0) })

        view.addView(work, FrameLayout.LayoutParams(-1, -1))
        view.addView(rest, FrameLayout.LayoutParams(-1, -1))
        rest.visibility = View.GONE
        startReady()
    }

    // ------------------------------------------------------------ flow
    private fun s() = steps[idx]

    private fun refreshSegments() {
        for ((i, b) in segs.withIndex()) b.fraction = if (i < idx) 1f else 0f
        counter.text = "${idx + 1}/${steps.size}"
    }

    private fun showWork() {
        if (work.visibility != View.VISIBLE) { work.visibility = View.VISIBLE; work.alpha = 0f; work.animate().alpha(1f).setDuration(160).start() }
        rest.visibility = View.GONE
    }

    private fun startReady() {
        st = St.READY; idx = 0
        showWork(); refreshSegments(); bindExercise()
        labelTv.text = "READY TO GO"
        begin(10_000)
        act.speaker.say("Get ready. First, ${s().title.lowercase()}.")
    }

    private fun startWork() {
        st = St.WORK
        showWork(); refreshSegments(); bindExercise()
        labelTv.text = ""
        begin(s().sec * 1000L)
        buzz(50)
        act.speaker.say(s().title.lowercase())
    }

    private fun startRest(sec: Int) {
        st = St.REST
        rest.visibility = View.VISIBLE; work.visibility = View.GONE
        val n = steps[idx + 1]
        restFig.ex = n.ex; restFig.mirror = n.mirror
        restNext.text = n.title
        restNextTime.text = fmtTime(n.sec)
        restCount.text = "NEXT  ${idx + 2}/${steps.size}"
        restQuote.text = "\u201C" + Data.quoteFor(day) + "\u201D"
        begin(sec * 1000L)
        buzz(30)
        act.speaker.say("Rest. Next, ${n.title.lowercase()}.")
    }

    private fun bindExercise() {
        val e = s()
        fig.ex = e.ex; fig.mirror = e.mirror; fig.playing = true
        nameTv.text = e.title
        paused = false; pauseBtn.text = "PAUSE"
    }

    private fun begin(ms: Long) {
        totalMs = ms; remMs = ms; endAt = SystemClock.elapsedRealtime() + ms
        lastSec = -1; halfSaid = false; lastTick = SystemClock.elapsedRealtime()
        paused = false
        tick()
    }

    private fun tick() {
        handler.removeCallbacks(tickR)
        if (dead) return
        val now = SystemClock.elapsedRealtime()
        if (!paused) {
            activeMs += now - lastTick
            remMs = endAt - now
        }
        lastTick = now
        if (remMs <= 0) { onDone(); return }
        val sec = ((remMs + 999) / 1000).toInt()
        val frac = 1f - remMs.toFloat() / totalMs
        when (st) {
            St.REST -> restTime.text = fmtTime(sec)
            else -> { timeTv.text = fmtTime(sec); bar.fraction = frac }
        }
        if (sec != lastSec) {
            lastSec = sec
            if (!paused) {
                if (st == St.WORK) {
                    if (sec in 1..3) act.speaker.say(sec.toString())
                    if (!halfSaid && totalMs >= 40_000 && remMs <= totalMs / 2) { halfSaid = true; act.speaker.say("Halfway") }
                } else if (st == St.READY && sec in 1..3) act.speaker.say(sec.toString())
                else if (st == St.REST && sec in 1..3) act.speaker.say(sec.toString())
            }
        }
        handler.postDelayed(tickR, 50)
    }

    private fun onDone() {
        when (st) {
            St.READY -> startWork()
            St.REST -> { idx++; startWork() }
            St.WORK -> finishWork()
        }
    }

    private fun finishWork() {
        if (idx >= steps.size - 1) { complete(); return }
        val r = if (s().rest == -1) act.store.restSeconds else s().rest
        if (r <= 0) { idx++; startWork() } else startRest(r)
    }

    private fun complete() {
        dead = true
        handler.removeCallbacks(tickR)
        act.speaker.say("Workout complete. Great work.")
        buzz(120)
        act.showComplete(day, steps, (activeMs / 1000).toInt())
    }

    private fun skip() {
        when (st) {
            St.READY -> startWork()
            St.WORK -> finishWork()
            St.REST -> { idx++; startWork() }
        }
    }

    private fun prev() {
        if (st == St.REST) { startWork(); return }
        if (idx > 0) { idx--; startWork() } else startWork()
    }

    private fun addTime(ms: Long) {
        endAt += ms; remMs += ms; totalMs += ms; tick()
    }

    private fun togglePause() { setPaused(!paused) }

    private fun setPaused(p: Boolean) {
        if (p == paused) return
        paused = p
        if (!p) { endAt = SystemClock.elapsedRealtime() + remMs; lastTick = SystemClock.elapsedRealtime() }
        fig.playing = !p; restFig.playing = !p
        pauseBtn.text = if (p) "RESUME" else "PAUSE"
        if (p) act.speaker.stop()
        tick()
    }

    private fun showInfo() {
        val was = paused
        setPaused(true)
        InfoSheet.show(act, s().ex, s().mirror) { if (!was && !dead) setPaused(false) }
    }

    private fun askQuit() {
        val was = paused
        setPaused(true)
        val d = Dialog(act)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL; background = act.round(C.CARD, 24f); setPadding(act.dp(24), act.dp(24), act.dp(24), act.dp(20))
        }
        box.addView(act.tv("Quit this workout?", 20f, C.WHITE, HEAD))
        box.addView(act.tv("Your progress for today will not be saved.", 14f, C.GRAY, BODY).also { it.margins(t = act.dp(8)) })
        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(act.button("CONTINUE", C.RED, C.WHITE, 14f, 48, 24f).apply { setOnClickListener { d.dismiss() } }, lp(0, act.dp(48), 1f).also { it.rightMargin = act.dp(8) })
        row.addView(act.button("QUIT", C.CARD2, C.WHITE, 14f, 48, 24f).apply { setOnClickListener { d.setOnDismissListener(null); d.dismiss(); dead = true; act.speaker.stop(); act.showHome() } }, lp(0, act.dp(48), 1f))
        box.addView(row, lp(MATCH, WRAP).also { it.topMargin = act.dp(20) })
        d.setContentView(box)
        d.window?.apply { setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); setLayout((act.resources.displayMetrics.widthPixels * 0.88f).toInt(), WRAP) }
        d.setOnDismissListener { if (!was && !dead) setPaused(false) }
        d.show()
    }

    private fun buzz(ms: Long) {
        try {
            val v = act.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    override fun onBack(): Boolean { askQuit(); return true }
    override fun onPause() { if (!dead) setPaused(true) }
    override fun onDestroy() { dead = true; handler.removeCallbacks(tickR) }
}
