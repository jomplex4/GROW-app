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
            field = v; last = 0L; shownAt = 0L
            // exercises that switch sides with a quick fade start just after the fade, so they never open on an empty stage
            t = if (v != null && Sprites.meta(v.id)?.dip == true) 0.24f else 0f
            v?.let { e ->
                if (Sprites.meta(e.id) != null) {
                    if (thumb) { if (active) Sprites.loadHalf(context, e.id) { invalidate() } }
                    else Sprites.load(context, e.id) { invalidate() }
                }
            }
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
    /** list thumbnail: half-size frames, animated only while on screen */
    var thumb = false
    var active = true
        set(v) {
            if (field == v) return
            field = v; last = 0L
            if (v && thumb) ex?.let { e -> if (Sprites.meta(e.id) != null) Sprites.loadHalf(context, e.id) { invalidate() } }
            invalidate()
        }

    private var t = 0f
    private var last = 0L
    private var shownAt = 0L
    private val gfx = CanvasGfx()
    private val renderer = FigureRenderer()
    private val bp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dst = RectF()

    private fun advance(): Boolean {
        val animate = still < 0f && playing && (!thumb || active)
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
        val fr: Array<Bitmap>? = if (thumb) (if (active) Sprites.halfFrames(e.id) else null) else Sprites.frames(e.id)
        layout(m, w, h)
        if (fr == null || fr.size < m.n) {
            // list rows keep a sharp first frame in place until their animation is ready; the main screen shows the stage only
            if (thumb) Sprites.still(context, e.id)?.let { b -> c.save(); if (mirror && e.sided) c.scale(-1f, 1f, w / 2f, h / 2f); bp.alpha = 255; c.drawBitmap(b, null, dst, bp); c.restore() }
            return
        }
        val animate = advance()
        // short fade-in the first time the frames are drawn (no pop, no blurry placeholder)
        val now = System.nanoTime()
        if (shownAt == 0L) shownAt = now
        val appear = if (thumb) 1f else ((now - shownAt) / 160e6f).coerceIn(0f, 1f)
        c.save()
        if (mirror && e.sided) c.scale(-1f, 1f, w / 2f, h / 2f)
        if (m.ground && !m.bust) gfx.oval(dst.centerX(), dst.bottom, dst.width() * 0.30f, h * 0.018f, (0x26 * appear).toInt() shl 24)
        val loopSec = if (m.loop > 0f) m.loop else e.loop
        if (!m.step) {
            val total = if (m.alt) loopSec * 2f else loopSec
            val ph = ((t / total) % 1f + 1f) % 1f
            val flip: Boolean; val local: Float
            if (m.alt) { flip = ph >= 0.5f; local = (ph * 2f) % 1f } else { flip = false; local = ph }
            var vis = appear
            if (m.dip) {   // brief fade while the figure switches to the other side (it is at rest there)
                val d = minOf(local, 1f - local) * loopSec
                vis *= Seq.smooth(d / 0.22f)
            }
            if (m.breath) {   // subtle breathing so static holds never look frozen
                val k = 1f + 0.012f * kotlin.math.sin(2.0 * Math.PI * t / 4.0).toFloat()
                val dh = dst.height() * k
                if (!m.ground && !m.bust) dst.bottom = dst.top + dh else dst.top = dst.bottom - dh
            }
            val a: Int; val b: Int; val f: Float
            if (m.cycle) {
                val fi = local * m.n
                a = fi.toInt() % m.n; b = (a + 1) % m.n; f = fi - fi.toInt()
            } else {
                val pos = if (m.cyc) Seq.cosPos(local) else Seq.profile(local, m.ri, m.ho, m.fa)
                val fi = pos * (m.n - 1)
                a = fi.toInt().coerceIn(0, m.n - 1); b = minOf(a + 1, m.n - 1); f = fi - a
            }
            blit(c, fr[a], flip, (if (b == a) 1f else 1f - f) * vis)
            if (b != a && f > 0.01f) blit(c, fr[b], flip, f * vis)
        } else {
            val loop = if (m.alt) loopSec * 2f else loopSec
            val beats = Seq.beats(m.n, m.alt); val flips = Seq.flips(m.n, m.alt)
            val len = beats.size
            val perBeat = maxOf(loop / len, 0.16f)
            val u = (t / perBeat) % len
            val i = u.toInt() % len; val f = u - u.toInt()
            val fade = 0.30f
            val tb = if (f > 1f - fade) Seq.smooth((f - (1f - fade)) / fade) else 0f
            val nx = (i + 1) % len
            blit(c, fr[beats[i]], flips[i], (1f - tb) * appear)
            if (tb > 0.01f) blit(c, fr[beats[nx]], flips[nx], tb * appear)
        }
        c.restore()
        if (animate || appear < 1f) postInvalidateOnAnimation()
    }
}

/** Representative frame (second keyframe) used for thumbnails of the code-drawn fallback. */
fun Ex.repPos(): Float = if (poses.size > 1) 1f / poses.size + 0.0001f else 0f
