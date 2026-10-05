package com.digitalminds.grow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

class CanvasGfx : Gfx {
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
    fun oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Int) {
        p.shader = null; p.style = Paint.Style.FILL; p.color = color
        rf.set(cx - rx, cy - ry, cx + rx, cy + ry); c.drawOval(rf, p)
    }
    override fun gradRoundRect(l: Float, t: Float, r: Float, b: Float, rad: Float, c1: Int, c2: Int) {
        val key = "$t,$b,$c1,$c2"
        if (key != sKey) { shader = LinearGradient(0f, t, 0f, b, c1, c2, Shader.TileMode.CLAMP); sKey = key }
        p.shader = shader; p.style = Paint.Style.FILL; p.color = 0xFFFFFFFF.toInt()
        rf.set(l, t, r, b); c.drawRoundRect(rf, rad, rad, p)
        p.shader = null
    }
}

/** Shows the exercise as an animated sprite (ChatGPT art, aligned and morphed offline); falls back to the code-drawn figure. */
class FigureView(ctx: Context) : View(ctx) {
    var ex: Ex? = null
        set(v) {
            field = v; t = 0f; last = 0L
            v?.let { e -> if (Sprites.meta(e.id) != null) Sprites.load(context, e.id) { invalidate() } }
            invalidate()
        }
    var mirror = false
        set(v) { field = v; invalidate() }
    var playing = true
        set(v) { field = v; last = 0L; invalidate() }
    /** if >= 0, render one frame at this normalized loop position and stop animating (code-drawn fallback only) */
    var still = -1f
        set(v) { field = v; invalidate() }
    var radiusDp = 20f

    private var t = 0f
    private var last = 0L
    private val gfx = CanvasGfx()
    private val renderer = FigureRenderer()
    private val bp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dst = RectF()

    private fun advance(): Boolean {
        val animate = still < 0f && playing
        val now = System.nanoTime()
        if (animate) { if (last != 0L) t += (now - last) / 1e9f; last = now } else last = 0L
        return animate
    }

    override fun onDraw(c: Canvas) {
        val e = ex ?: return
        val m = Sprites.meta(e.id)
        if (m != null && !Sprites.failed(e.id)) { drawSprite(c, e, m); return }
        val animate = advance()
        val pos = if (still >= 0f) still else t / e.loop
        gfx.c = c
        renderer.draw(gfx, e, pos, width.toFloat(), height.toFloat(), mirror, true, radiusDp * resources.displayMetrics.density)
        if (animate) postInvalidateOnAnimation()
    }

    private fun layout(m: SpriteMeta, w: Float, h: Float) {
        if (m.bust) {
            val s = minOf(w * 0.98f / m.w, h / m.h)
            val dw = m.w * s; val dh = m.h * s
            dst.set((w - dw) / 2f, h - dh, (w + dw) / 2f, h)
            return
        }
        val s = minOf(w * 0.92f / m.w, h * 0.88f / m.h)
        val dw = m.w * s; val dh = m.h * s
        val wide = m.w.toFloat() / m.h > 1.1f
        val cy = when { wide -> h * 0.58f; !m.ground -> h * 0.50f; else -> h * 0.94f - dh / 2f }
        dst.set((w - dw) / 2f, cy - dh / 2f, (w + dw) / 2f, cy + dh / 2f)
    }

    private fun blit(c: Canvas, b: Bitmap, flip: Boolean, alpha: Float) {
        bp.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        if (flip) {
            c.save(); c.scale(-1f, 1f, dst.centerX(), dst.centerY()); c.drawBitmap(b, null, dst, bp); c.restore()
        } else c.drawBitmap(b, null, dst, bp)
    }

    private fun drawSprite(c: Canvas, e: Ex, m: SpriteMeta) {
        val w = width.toFloat(); val h = height.toFloat()
        gfx.c = c
        gfx.gradRoundRect(0f, 0f, w, h, radiusDp * resources.displayMetrics.density, Pal.stageTop, Pal.stageBot)
        val fr = Sprites.frames(e.id)
        if (fr == null || fr.size < m.n) return          // still loading: stage only, redrawn when ready
        val animate = advance()
        layout(m, w, h)
        c.save()
        if (mirror && e.sided) c.scale(-1f, 1f, w / 2f, h / 2f)
        if (m.ground && !m.bust) {
            bp.alpha = 255
            gfx.oval(dst.centerX(), dst.bottom, dst.width() * 0.30f, h * 0.018f, 0x26000000)
        }
        val loop = if (m.alt) e.loop * 2f else e.loop
        if (!m.step) {
            val phase = ((t / loop) % 1f + 1f) % 1f
            val fi = Seq.smoothPos(phase) * (m.n - 1)
            val a = fi.toInt().coerceIn(0, m.n - 1); val b = minOf(a + 1, m.n - 1); val f = fi - a
            blit(c, fr[a], false, if (b == a) 1f else 1f - f)
            if (b != a && f > 0.01f) blit(c, fr[b], false, f)
        } else {
            val beats = Seq.beats(m.n, m.alt); val flips = Seq.flips(m.n, m.alt)
            val len = beats.size
            val perBeat = maxOf(loop / len, 0.16f)
            val u = (t / perBeat) % len
            val i = u.toInt() % len; val f = u - u.toInt()
            val fade = 0.30f
            val tb = if (f > 1f - fade) Seq.smooth((f - (1f - fade)) / fade) else 0f
            val nx = (i + 1) % len
            blit(c, fr[beats[i]], flips[i], 1f - tb)
            if (tb > 0.01f) blit(c, fr[beats[nx]], flips[nx], tb)
        }
        c.restore()
        if (animate) postInvalidateOnAnimation()
    }
}

/** Static small preview used in the exercise list. */
class ThumbView(ctx: Context, private val bmp: Bitmap) : View(ctx) {
    private val gfx = CanvasGfx()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val r = RectF()
    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        gfx.c = c
        gfx.gradRoundRect(0f, 0f, w, h, 14f * resources.displayMetrics.density, Pal.stageTop, Pal.stageBot)
        val s = minOf(w * 0.86f / bmp.width, h * 0.86f / bmp.height)
        val dw = bmp.width * s; val dh = bmp.height * s
        r.set((w - dw) / 2f, h - dh - h * 0.07f, (w + dw) / 2f, h - h * 0.07f)
        c.drawBitmap(bmp, null, r, p)
    }
}

/** Representative frame (second keyframe) used for thumbnails of the code-drawn fallback. */
fun Ex.repPos(): Float = if (poses.size > 1) 1f / poses.size + 0.0001f else 0f
