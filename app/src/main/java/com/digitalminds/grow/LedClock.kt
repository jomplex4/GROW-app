package com.digitalminds.grow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Scoreboard-style seven segment clock ("12:34"), drawn in code so it looks the same on every phone. */
class LedClock(ctx: Context, private val digitDp: Float, var color: Int = C.WHITE) : View(ctx) {
    var text: CharSequence = "00:00"
        set(v) { if (field.toString() != v.toString()) { val relayout = field.length != v.length; field = v; if (relayout) requestLayout(); invalidate() } }

    private val lit = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val off = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()
    private val density = ctx.resources.displayMetrics.density
    private val H get() = digitDp * density
    private val W get() = H * 0.56f
    private val T get() = H * 0.15f
    private val gapDigit get() = H * 0.16f
    private val colonW get() = H * 0.22f

    // segments a b c d e f g
    private val seg = mapOf(
        '0' to "abcdef", '1' to "bc", '2' to "abdeg", '3' to "abcdg", '4' to "bcfg",
        '5' to "acdfg", '6' to "acdefg", '7' to "abc", '8' to "abcdefg", '9' to "abcdfg"
    )

    private fun contentWidth(): Float {
        var w = 0f
        val s = text.toString()
        for ((i, ch) in s.withIndex()) {
            w += if (ch == ':') colonW else W
            if (i < s.length - 1) w += gapDigit
        }
        return w
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        setMeasuredDimension(Math.ceil(contentWidth().toDouble()).toInt() + paddingLeft + paddingRight, Math.ceil(H.toDouble()).toInt() + paddingTop + paddingBottom)
    }

    private fun hseg(x: Float, y: Float, len: Float, t: Float) {
        path.moveTo(x, y + t / 2); path.lineTo(x + t / 2, y); path.lineTo(x + len - t / 2, y)
        path.lineTo(x + len, y + t / 2); path.lineTo(x + len - t / 2, y + t); path.lineTo(x + t / 2, y + t); path.close()
    }
    private fun vseg(x: Float, y: Float, len: Float, t: Float) {
        path.moveTo(x + t / 2, y); path.lineTo(x + t, y + t / 2); path.lineTo(x + t, y + len - t / 2)
        path.lineTo(x + t / 2, y + len); path.lineTo(x, y + len - t / 2); path.lineTo(x, y + t / 2); path.close()
    }

    private fun drawDigit(c: Canvas, x: Float, y: Float, ch: Char) {
        val on = seg[ch] ?: ""
        val w = W; val h = H; val t = T; val gp = t * 0.16f
        val hLen = w - t - 2 * gp
        val yt = y + t / 2; val ym = y + h / 2; val yb = y + h - t / 2
        val lenV = (ym - yt) - 2 * gp
        fun d(name: Char, build: () -> Unit) {
            path.reset(); build()
            c.drawPath(path, if (on.indexOf(name) >= 0) lit else off)
        }
        d('a') { hseg(x + t / 2 + gp, y, hLen, t) }
        d('g') { hseg(x + t / 2 + gp, ym - t / 2, hLen, t) }
        d('d') { hseg(x + t / 2 + gp, y + h - t, hLen, t) }
        d('f') { vseg(x, yt + gp, lenV, t) }
        d('b') { vseg(x + w - t, yt + gp, lenV, t) }
        d('e') { vseg(x, ym + gp, lenV, t) }
        d('c') { vseg(x + w - t, ym + gp, lenV, t) }
    }

    override fun onDraw(c: Canvas) {
        lit.color = color; off.color = (color and 0x00FFFFFF) or 0x1C000000
        var x = paddingLeft.toFloat(); val y = paddingTop.toFloat()
        val s = text.toString()
        for ((i, ch) in s.withIndex()) {
            if (ch == ':') {
                val side = T * 1.05f
                val cx = x + (colonW - side) / 2f
                c.drawRect(cx, y + H * 0.30f, cx + side, y + H * 0.30f + side, lit)
                c.drawRect(cx, y + H * 0.70f - side, cx + side, y + H * 0.70f, lit)
                x += colonW
            } else { drawDigit(c, x, y, ch); x += W }
            if (i < s.length - 1) x += gapDigit
        }
    }
}
