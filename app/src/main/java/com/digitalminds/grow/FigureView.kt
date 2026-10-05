package com.digitalminds.grow

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

private class CanvasGfx : Gfx {
    lateinit var c: Canvas
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()
    private var shader: LinearGradient? = null
    private var sKey = ""

    override fun poly(p: FloatArray, n: Int, color: Int) {
        path.rewind(); path.moveTo(p[0], p[1])
        for (i in 1 until n) path.lineTo(p[2 * i], p[2 * i + 1])
        path.close()
        this.p.shader = null; this.p.style = Paint.Style.FILL; this.p.color = color
        c.drawPath(path, this.p)
    }
    override fun circle(cx: Float, cy: Float, r: Float, color: Int) {
        p.shader = null; p.style = Paint.Style.FILL; p.color = color; c.drawCircle(cx, cy, r, p)
    }
    override fun polyline(pts: FloatArray, n: Int, w: Float, color: Int) {
        path.rewind(); path.moveTo(pts[0], pts[1])
        for (i in 1 until n) path.lineTo(pts[2 * i], pts[2 * i + 1])
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = w; p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND; p.color = color
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
    }
    override fun roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int) {
        p.shader = null; p.style = Paint.Style.FILL; p.color = color
        rf.set(l, t, r, b); c.drawRoundRect(rf, rad, rad, p)
    }
    override fun gradRoundRect(l: Float, t: Float, r: Float, b: Float, rad: Float, c1: Int, c2: Int) {
        val key = "$t,$b,$c1,$c2"
        if (key != sKey) { shader = LinearGradient(0f, t, 0f, b, c1, c2, Shader.TileMode.CLAMP); sKey = key }
        p.shader = shader; p.style = Paint.Style.FILL; p.color = 0xFFFFFFFF.toInt()
        rf.set(l, t, r, b); c.drawRoundRect(rf, rad, rad, p)
        p.shader = null
    }
}

/** Draws the flat human figure from keyframe poses. No external libs; geometry lives in FigureRenderer. */
class FigureView(ctx: Context) : View(ctx) {
    var ex: Ex? = null
        set(v) { field = v; t = 0f; last = 0L; invalidate() }
    var mirror = false
        set(v) { field = v; invalidate() }
    var playing = true
        set(v) { field = v; last = 0L; invalidate() }
    /** if >= 0, render a single frame at this normalized loop position and stop animating */
    var still = -1f
        set(v) { field = v; invalidate() }
    var radiusDp = 20f

    private var t = 0f
    private var last = 0L
    private val gfx = CanvasGfx()
    private val renderer = FigureRenderer()

    override fun onDraw(c: Canvas) {
        val e = ex ?: return
        val now = System.nanoTime()
        val animate = still < 0f && playing
        if (animate) { if (last != 0L) t += (now - last) / 1e9f; last = now } else last = 0L
        val pos = if (still >= 0f) still else t / e.loop
        gfx.c = c
        renderer.draw(gfx, e, pos, width.toFloat(), height.toFloat(), mirror, true, radiusDp * resources.displayMetrics.density)
        if (animate) postInvalidateOnAnimation()
    }
}

/** Representative frame (second keyframe) used for thumbnails. */
fun Ex.repPos(): Float = if (poses.size > 1) 1f / poses.size + 0.0001f else 0f
