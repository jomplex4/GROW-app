package com.digitalminds.grow

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate
import java.time.LocalTime

interface Screen {
    val view: View
    fun onBack(): Boolean = false
    fun onPause() {}
    fun onDestroy() {}
}

fun MainActivity.card(pad: Int = 16): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = round(C.CARD, 22f)
    setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
}

fun MainActivity.statTriple(a: Pair<String, String>, b: Pair<String, String>, c: Pair<String, String>, valueColor: Int = C.WHITE): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        for (p in listOf(a, b, c)) {
            addView(LinearLayout(this@statTriple).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                addView(tv(p.first, 28f, valueColor, HEAD, Gravity.CENTER))
                addView(tv(p.second, 12f, C.GRAY, MED, Gravity.CENTER).also { it.margins(t = dp(4)) })
            }, lp(0, WRAP, 1f))
        }
    }

/** +/- value row. */
class Stepper(val act: MainActivity, val label: String, val unit: String, var value: Float, val step: Float, val min: Float, val max: Float) {
    val view: LinearLayout
    private val valueTv: TextView
    private val h = Handler(Looper.getMainLooper())
    var onChange: (() -> Unit)? = null

    init {
        view = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        view.addView(act.tv(label, 16f, C.WHITE, MED), lp(0, WRAP, 1f))
        view.addView(btn("−", -1))
        valueTv = act.tv("", 20f, C.RED, HEAD, Gravity.CENTER).apply { minWidth = act.dp(92) }
        view.addView(valueTv)
        view.addView(btn("+", 1))
        refresh()
    }

    fun refresh() { valueTv.text = "%.1f %s".format(value, unit) }

    private fun bump(dir: Int) {
        value = (Math.round((value + dir * step) * 10f) / 10f).coerceIn(min, max); refresh(); onChange?.invoke()
    }

    private fun btn(sign: String, dir: Int): TextView {
        val t = act.tv(sign, 24f, C.WHITE, HEAD, Gravity.CENTER)
        t.background = act.ripple(act.round(C.CARD2, 20f))
        t.layoutParams = lp(act.dp(44), act.dp(44))
        var rep: Runnable? = null
        t.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true; bump(dir)
                    var n = 0
                    rep = object : Runnable { override fun run() { bump(dir); n++; h.postDelayed(this, if (n > 8) 40L else 110L) } }
                    h.postDelayed(rep!!, 420L)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; rep?.let { h.removeCallbacks(it) } }
            }
            true
        }
        return t
    }
}

fun MainActivity.reminderCard(): View {
    val c = card()
    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    left.addView(tv("Daily reminder", 16f, C.WHITE, MED))
    val value = tv("", 22f, C.RED, HEAD).also { it.margins(t = dp(4)) }
    left.addView(value)
    row.addView(left, lp(0, WRAP, 1f))
    val off = tv("OFF", 13f, C.GRAY, HEAD, Gravity.CENTER).apply {
        background = ripple(round(C.CARD2, 18f)); setPadding(dp(16), dp(9), dp(16), dp(9))
    }
    row.addView(off)
    c.addView(row)
    fun refresh() {
        val m = store.reminder
        value.text = if (m < 0) "Off" else "%02d:%02d".format(m / 60, m % 60)
        off.visibility = if (m < 0) View.GONE else View.VISIBLE
        value.setTextColor(if (m < 0) C.DIM else C.RED)
    }
    refresh()
    c.setOnClickListener {
        val m = if (store.reminder >= 0) store.reminder else 5 * 60
        TimePickerDialog(this, android.R.style.Theme_DeviceDefault_Dialog_Alert, { _, hh, mm ->
            store.reminder = hh * 60 + mm
            Reminder.schedule(this, store.reminder)
            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 11)
            refresh()
        }, m / 60, m % 60, true).show()
    }
    off.setOnClickListener { store.reminder = -1; Reminder.schedule(this, -1); refresh() }
    return c
}

/** Measurement entry. AM/PM slot chosen automatically by clock, switchable. */
class MeasureCard(val act: MainActivity) {
    var slot = if (java.time.LocalTime.now().hour < 12) 0 else 1
    val height = Stepper(act, "Height", "cm", act.store.lastHeight(), 0.1f, 100f, 230f)
    val weight = Stepper(act, "Weight", "kg", act.store.lastWeight(), 0.1f, 25f, 200f)
    val view: LinearLayout = act.card()
    private val chip: TextView
    var touched = false

    init {
        height.onChange = { touched = true }
        weight.onChange = { touched = true }
        val head = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(act.tv("Measurements", 16f, C.WHITE, MED), lp(0, WRAP, 1f))
        chip = act.tv("", 12f, C.WHITE, HEAD, Gravity.CENTER).apply {
            background = act.ripple(act.round(C.RED, 16f)); setPadding(act.dp(14), act.dp(8), act.dp(14), act.dp(8)); letterSpacing = 0.1f
        }
        chip.setOnClickListener { slot = 1 - slot; fillFromSlot(); refreshChip() }
        head.addView(chip)
        view.addView(head)
        view.addView(height.view.also { it.margins(t = act.dp(14)) })
        view.addView(weight.view.also { it.margins(t = act.dp(10)) })
        view.addView(act.tv("Measure in the morning when you are tallest, and again in the afternoon.", 12f, C.DIM, BODY).also { it.margins(t = act.dp(12)) })
        refreshChip()
    }

    private fun fillFromSlot() {
        val today = LocalDate.now().toEpochDay()
        val l = act.store.logs().lastOrNull { it.epochDay == today && it.slot == slot }
        if (l != null) { height.value = l.height; weight.value = l.weight; height.refresh(); weight.refresh(); touched = false }
    }
    private fun refreshChip() { chip.text = if (slot == 0) "MORNING" else "AFTERNOON" }

    fun save() { if (touched) { act.store.addLog(LocalDate.now().toEpochDay(), slot, height.value, weight.value); touched = false } }
}

object InfoSheet {
    fun show(act: MainActivity, ex: Ex, mirror: Boolean, onDismiss: (() -> Unit)? = null) {
        val d = Dialog(act)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(C.BG); val r = act.dp(28f).toFloat(); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            setPadding(act.dp(18), act.dp(18), act.dp(18), act.dp(22))
        }
        val stage = FrameLayout(act)
        val fig = FigureView(act).also { it.ex = ex; it.mirror = mirror && ex.sided; it.radiusDp = 22f }
        stage.addView(fig, fl(MATCH, MATCH))
        root.addView(stage, lp(MATCH, act.dp(260)))
        root.addView(act.tv(ex.nameEs, 22f, C.WHITE, HEAD).also { it.letterSpacing = 0.04f; it.margins(t = act.dp(16)) })
        root.addView(act.tv(Data.sectionEs(ex.pillar[0]), 11f, C.RED, HEAD).also { it.letterSpacing = 0.15f; it.margins(t = act.dp(4)) })
        root.addView(act.tv(ex.desc, 15f, C.GRAY, BODY).also { it.margins(t = act.dp(12)); it.setLineSpacing(act.dp(3).toFloat(), 1f) })
        for (t in ex.tips) root.addView(act.tv("•  $t", 14f, C.WHITE, BODY).also { it.margins(t = act.dp(8)) })
        root.addView(act.button("CERRAR", C.RED, C.WHITE, 15f, 52).also { it.margins(t = act.dp(18)); it.setOnClickListener { d.dismiss() } }, lp(MATCH, act.dp(52)))
        val sv = ScrollView(act).apply { addView(root); isVerticalScrollBarEnabled = false }
        d.setContentView(sv)
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
        }
        d.setOnDismissListener { onDismiss?.invoke() }
        d.show()
    }
}
