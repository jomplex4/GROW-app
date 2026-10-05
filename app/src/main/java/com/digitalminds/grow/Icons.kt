package com.digitalminds.grow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Ic { BACK, GEAR, CHART, PAUSE, PLAY, PREV, NEXT, CHECK, CLOSE, SLIDERS, BARS }

class IconView(ctx: Context, var kind: Ic, var color: Int = C.WHITE) : View(ctx) {
    var scale = 1f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val f = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val s = minOf(w, h) * scale
        val cx = w / 2; val cy = h / 2
        p.color = color; f.color = color; p.strokeWidth = s * 0.09f
        when (kind) {
            Ic.BACK -> { path.reset(); path.moveTo(cx + s * .12f, cy - s * .25f); path.lineTo(cx - s * .14f, cy); path.lineTo(cx + s * .12f, cy + s * .25f); c.drawPath(path, p) }
            Ic.CLOSE -> { c.drawLine(cx - s * .2f, cy - s * .2f, cx + s * .2f, cy + s * .2f, p); c.drawLine(cx + s * .2f, cy - s * .2f, cx - s * .2f, cy + s * .2f, p) }
            Ic.GEAR -> {
                c.drawCircle(cx, cy, s * .17f, p)
                for (i in 0 until 8) {
                    val a = i * PI / 4
                    c.drawLine(cx + cos(a).toFloat() * s * .26f, cy + sin(a).toFloat() * s * .26f, cx + cos(a).toFloat() * s * .35f, cy + sin(a).toFloat() * s * .35f, p)
                }
                c.drawCircle(cx, cy, s * .26f, p)
            }
            Ic.CHART -> {
                p.strokeWidth = s * .11f
                c.drawLine(cx - s * .24f, cy + s * .24f, cx - s * .24f, cy + s * .02f, p)
                c.drawLine(cx, cy + s * .24f, cx, cy - s * .12f, p)
                c.drawLine(cx + s * .24f, cy + s * .24f, cx + s * .24f, cy - s * .28f, p)
            }
            Ic.PAUSE -> { f.style = Paint.Style.FILL
                c.drawRoundRect(RectF(cx - s * .22f, cy - s * .25f, cx - s * .06f, cy + s * .25f), s * .04f, s * .04f, f)
                c.drawRoundRect(RectF(cx + s * .06f, cy - s * .25f, cx + s * .22f, cy + s * .25f), s * .04f, s * .04f, f) }
            Ic.PLAY -> { path.reset(); path.moveTo(cx - s * .16f, cy - s * .26f); path.lineTo(cx + s * .26f, cy); path.lineTo(cx - s * .16f, cy + s * .26f); path.close(); c.drawPath(path, f) }
            Ic.PREV -> { c.drawLine(cx - s * .22f, cy - s * .2f, cx - s * .22f, cy + s * .2f, p)
                path.reset(); path.moveTo(cx + s * .2f, cy - s * .22f); path.lineTo(cx - s * .12f, cy); path.lineTo(cx + s * .2f, cy + s * .22f); path.close(); c.drawPath(path, f) }
            Ic.NEXT -> { c.drawLine(cx + s * .22f, cy - s * .2f, cx + s * .22f, cy + s * .2f, p)
                path.reset(); path.moveTo(cx - s * .2f, cy - s * .22f); path.lineTo(cx + s * .12f, cy); path.lineTo(cx - s * .2f, cy + s * .22f); path.close(); c.drawPath(path, f) }
            Ic.SLIDERS -> {
                p.strokeWidth = s * .075f
                val ys = floatArrayOf(-.30f, 0f, .30f); val ks = floatArrayOf(-.14f, .20f, -.02f)
                for (i in 0..2) {
                    c.drawLine(cx - s * .46f, cy + ys[i] * s, cx + s * .46f, cy + ys[i] * s, p)
                    f.color = color; c.drawCircle(cx + ks[i] * s, cy + ys[i] * s, s * .105f, f)
                }
            }
            Ic.BARS -> {
                p.strokeWidth = s * .15f
                val hs = floatArrayOf(.30f, .52f, .76f); val xs = floatArrayOf(-.26f, 0f, .26f)
                for (i in 0..2) c.drawLine(cx + xs[i] * s, cy + s * .38f, cx + xs[i] * s, cy + s * .38f - hs[i] * s, p)
            }
            Ic.CHECK -> { p.strokeWidth = s * .13f; path.reset(); path.moveTo(cx - s * .22f, cy); path.lineTo(cx - s * .06f, cy + s * .17f); path.lineTo(cx + s * .24f, cy - s * .18f); c.drawPath(path, p) }
        }
    }
}

/** Animated flame for the completion screen. */
class FlameView(ctx: Context) : View(ctx) {
    private val f = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()
    private val inner = Path()
    private var t = 0f
    private var born = System.nanoTime()

    private fun build(p: Path) {
        p.reset()
        p.moveTo(50f, 4f)
        p.cubicTo(60f, 24f, 86f, 40f, 83f, 66f)
        p.cubicTo(81f, 86f, 66f, 96f, 50f, 96f)
        p.cubicTo(34f, 96f, 17f, 86f, 17f, 66f)
        p.cubicTo(17f, 50f, 27f, 42f, 34f, 30f)
        p.cubicTo(37f, 40f, 42f, 46f, 47f, 46f)
        p.cubicTo(44f, 30f, 44f, 14f, 50f, 4f)
        p.close()
    }
    init { build(path); build(inner) }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val age = (now - born) / 1e9f
        t = age
        val pop = if (age < 0.6f) { val x = age / 0.6f; (1f - Math.pow(1.0 - x, 3.0).toFloat()) * 1.0f + sin(x * PI.toFloat()) * 0.12f } else 1f
        val pulse = 1f + 0.035f * sin(t * 3.2f)
        val s = minOf(width, height) / 100f
        c.save()
        c.translate(width / 2f, height * 0.98f)
        c.scale(s * pop, s * pop * pulse)
        c.rotate(1.8f * sin(t * 2.4f))
        c.translate(-50f, -96f)
        f.color = C.RED
        c.drawPath(path, f)
        c.save()
        c.translate(50f, 96f); c.scale(0.5f, 0.5f); c.translate(-50f, -96f)
        f.color = 0xFFFFFFFF.toInt()
        c.drawPath(inner, f)
        c.restore()
        c.restore()
        postInvalidateOnAnimation()
    }
}
