package com.digitalminds.grow

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Minimal drawing surface so the figure code is identical on Android and in the JVM preview harness. */
interface Gfx {
    fun poly(p: FloatArray, n: Int, color: Int)
    fun circle(cx: Float, cy: Float, r: Float, color: Int)
    fun polyline(p: FloatArray, n: Int, w: Float, color: Int)
    fun roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int)
    fun gradRoundRect(l: Float, t: Float, r: Float, b: Float, rad: Float, c1: Int, c2: Int)
}

object Pal {
    var stageTop = 0xFFCFD0D5.toInt()
    var stageBot = 0xFFB4B6BD.toInt()
    var skin = 0xFF74767F.toInt();   var skinF = 0xFF565860.toInt()
    var red = 0xFFDC0000.toInt();    var redF = 0xFF9B0000.toInt()
    var shorts = 0xFF151517.toInt(); var shortsF = 0xFF0C0C0D.toInt()
    var shoe = 0xFFF5F5F7.toInt();   var shoeF = 0xFFC9CBD0.toInt()
    var sole = 0xFF9A9CA4.toInt();   var soleF = 0xFF7B7D85.toInt()
    var hair = 0xFF151517.toInt()
    var mat = 0xFF2A2B2F.toInt();    var bar = 0xFF3A3B40.toInt(); var wall = 0xFF9A9CA4.toInt()
    var floorLine = 0x33000000;      var shadow = 0x26000000
    var mouth = 0xFF151517.toInt();  var ear = 0xFF565860.toInt()
}

object Shapes {
    private fun chaikin(src: FloatArray, iters: Int): FloatArray {
        var a = src
        repeat(iters) {
            val n = a.size / 2
            val out = FloatArray(n * 4)
            for (i in 0 until n) {
                val j = (i + 1) % n
                out[i * 4] = 0.75f * a[i * 2] + 0.25f * a[j * 2]; out[i * 4 + 1] = 0.75f * a[i * 2 + 1] + 0.25f * a[j * 2 + 1]
                out[i * 4 + 2] = 0.25f * a[i * 2] + 0.75f * a[j * 2]; out[i * 4 + 3] = 0.25f * a[i * 2 + 1] + 0.75f * a[j * 2 + 1]
            }
            a = out
        }
        return a
    }

    /** (forward, up) pairs in head-radius units. */
    val upper = chaikin(floatArrayOf(
        -0.92f, 0.20f, -0.80f, 0.72f, -0.40f, 0.98f, 0.12f, 1.00f, 0.58f, 0.86f, 0.82f, 0.58f, 0.92f, 0.30f,
        0.94f, 0.10f, 1.20f, -0.22f, 0.96f, -0.34f, 1.00f, -0.46f, 0.93f, -0.56f,
        0.10f, -0.28f, -0.50f, -0.40f, -0.90f, -0.30f), 2)
    val jaw = chaikin(floatArrayOf(
        0.93f, -0.56f, 0.98f, -0.68f, 0.88f, -0.80f, 0.94f, -0.98f, 0.70f, -1.18f, 0.22f, -1.12f,
        -0.22f, -0.92f, -0.44f, -0.62f, -0.30f, -0.26f, 0.30f, -0.26f), 2)
    val hair = chaikin(floatArrayOf(
        0.84f, 0.58f, 0.62f, 0.92f, 0.12f, 1.05f, -0.40f, 1.01f, -0.86f, 0.74f, -0.98f, 0.22f, -0.95f, -0.28f,
        -0.78f, -0.46f, -0.66f, -0.10f, -0.38f, 0.26f, 0.05f, 0.50f, 0.48f, 0.60f), 2)
    val shoe = chaikin(floatArrayOf(
        -0.050f, -0.032f, -0.058f, 0.018f, -0.020f, 0.054f, 0.026f, 0.040f, 0.070f, 0.016f,
        0.108f, -0.002f, 0.118f, -0.018f, 0.110f, -0.032f), 1)
    val sole = floatArrayOf(-0.050f, -0.018f, -0.052f, -0.042f, 0.112f, -0.044f, 0.118f, -0.018f)
    val hairFront = chaikin(floatArrayOf(
        -0.95f, 0.05f, -0.92f, 0.60f, -0.50f, 1.05f, 0.00f, 1.14f, 0.50f, 1.05f, 0.92f, 0.60f, 0.95f, 0.05f,
        0.62f, 0.50f, 0.00f, 0.66f, -0.62f, 0.50f), 2)
}

class FigureRenderer {
    private val cur = FloatArray(17)
    private val buf = FloatArray(512)
    private var mir = false
    private var mw = 0f

    private fun shortest(a: Float, b: Float): Float = ((((b - a) % 360f) + 540f) % 360f) - 180f

    private fun isAngle(k: Int) = k == 2 || k == 3 || (k in 5..12) || k == 14 || k == 15 || k == 16

    fun pose(e: Ex, pos: Float) {
        val n = e.poses.size
        val u = (((pos % 1f) + 1f) % 1f) * n
        val i = u.toInt().coerceIn(0, n - 1)
        val f0 = u - i
        val p1 = e.poses[i]
        val p2 = e.poses[(i + 1) % n]
        val p0 = e.poses[(i + n - 1) % n]
        val p3 = e.poses[(i + 2) % n]
        val smooth = e.ease == 0 && n >= 4
        val f = if (e.ease == 1) f0 * f0 * (3f - 2f * f0) else f0
        for (k in 0 until 17) {
            val a1 = p1[k]; val a2 = p2[k]
            if (k == 16 && (a1 < -900f || a2 < -900f)) { cur[k] = a1; continue }
            val ang = isAngle(k)
            val d2 = if (ang) shortest(a1, a2) else a2 - a1
            if (!smooth) { cur[k] = a1 + d2 * f; continue }
            // cyclic Catmull-Rom on values unwrapped around p1
            val d0 = if (ang) shortest(a1, p0[k]) else p0[k] - a1
            val d3 = if (ang) shortest(a1, p3[k]) else p3[k] - a1
            val v0 = d0; val v1 = 0f; val v2 = d2
            val v3 = if (ang) d2 + shortest(a2, p3[k]) else d3
            val t = f; val t2 = t * t; val t3 = t2 * t
            val r = 0.5f * ((2f * v1) + (-v0 + v2) * t + (2f * v0 - 5f * v1 + 4f * v2 - v3) * t2 + (-v0 + 3f * v1 - 3f * v2 + v3) * t3)
            cur[k] = a1 + r
        }
    }

    private fun rad(d: Float) = d * (PI / 180.0).toFloat()
    private fun dnx(a: Float) = sin(rad(a)); private fun dny(a: Float) = cos(rad(a))
    private fun upx(a: Float) = sin(rad(a)); private fun upy(a: Float) = -cos(rad(a))
    private fun mx(x: Float) = if (mir) mw - x else x

    // ------------------------------------------------------------- primitives
    private fun circ(g: Gfx, x: Float, y: Float, r: Float, c: Int) = g.circle(mx(x), y, r, c)

    private fun emit(g: Gfx, n: Int, c: Int) {
        if (mir) for (i in 0 until n) buf[2 * i] = mw - buf[2 * i]
        g.poly(buf, n, c)
    }

    private fun hull(g: Gfx, ax: Float, ay: Float, ra: Float, bx: Float, by: Float, rb: Float, c: Int) {
        val dx = bx - ax; val dy = by - ay; val l = hypot(dx, dy)
        if (l < 1e-3f || l <= abs(ra - rb)) { circ(g, ax, ay, ra, c); circ(g, bx, by, rb, c); return }
        val th = atan2(dy, dx); val a = acos(((ra - rb) / l).coerceIn(-1f, 1f))
        val c1 = cos(th + a); val s1 = sin(th + a); val c2 = cos(th - a); val s2 = sin(th - a)
        buf[0] = ax + ra * c1; buf[1] = ay + ra * s1
        buf[2] = bx + rb * c1; buf[3] = by + rb * s1
        buf[4] = bx + rb * c2; buf[5] = by + rb * s2
        buf[6] = ax + ra * c2; buf[7] = ay + ra * s2
        emit(g, 4, c)
        circ(g, ax, ay, ra, c); circ(g, bx, by, rb, c)
    }

    /** limb with a muscle bulge at fraction t along the segment */
    private fun limb(g: Gfx, ax: Float, ay: Float, ra: Float, bx: Float, by: Float, rb: Float, t: Float, rm: Float, c: Int) {
        val mxp = ax + (bx - ax) * t; val myp = ay + (by - ay) * t
        hull(g, ax, ay, ra, mxp, myp, rm, c)
        hull(g, mxp, myp, rm, bx, by, rb, c)
    }

    private fun oval(g: Gfx, cx: Float, cy: Float, dirx: Float, diry: Float, a: Float, b: Float, c: Int) {
        val px = -diry; val py = dirx
        val n = 18
        for (i in 0 until n) {
            val t = (2.0 * PI * i / n).toFloat()
            val u = cos(t) * a; val v = sin(t) * b
            buf[2 * i] = cx + dirx * u + px * v; buf[2 * i + 1] = cy + diry * u + py * v
        }
        emit(g, n, c)
    }

    private fun shape(g: Gfx, s: FloatArray, ox: Float, oy: Float, fx: Float, fy: Float, ux: Float, uy: Float, k: Float, c: Int) {
        val n = s.size / 2
        for (i in 0 until n) {
            val f = s[2 * i] * k; val u = s[2 * i + 1] * k
            buf[2 * i] = ox + fx * f + ux * u; buf[2 * i + 1] = oy + fy * f + uy * u
        }
        emit(g, n, c)
    }

    private fun shoe(g: Gfx, ax: Float, ay: Float, fda: Float, k: Float, body: Int, sole: Int) {
        val fx = dnx(fda); val fy = dny(fda)
        val ux = fy; val uy = -fx
        shape(g, Shapes.sole, ax, ay, fx, fy, ux, uy, k, sole)
        shape(g, Shapes.shoe, ax, ay, fx, fy, ux, uy, k, body)
    }

    // ------------------------------------------------------------- scene
    fun draw(g: Gfx, e: Ex, pos: Float, w: Float, h: Float, mirror: Boolean, stage: Boolean, radius: Float) {
        pose(e, pos)
        mir = mirror; mw = w
        val unit = min(w, h); val ox = (w - unit) / 2f; val oy = (h - unit) / 2f
        val floorY = oy + 0.90f * unit
        if (stage) g.gradRoundRect(0f, 0f, w, h, radius, Pal.stageTop, Pal.stageBot)

        val L = unit * e.sc
        val front = e.front
        val hxp = ox + cur[0] * unit; val hyp = oy + cur[1] * unit
        val tA = cur[2]; val hA = cur[3]
        val tor = 0.30f * L
        val tdx = upx(tA); val tdy = upy(tA)
        val sx = hxp + tdx * tor; val sy = hyp + tdy * tor
        val hr = 0.072f * L
        val cx = sx + upx(hA) * (0.03f * L + hr) + cur[4] * unit
        val cy = sy + upy(hA) * (0.03f * L + hr)

        val ua = 0.16f * L; val fa = 0.15f * L; val hand = 0.035f * L
        val th = 0.24f * L; val sh = 0.23f * L; val ft = 0.085f * L
        val us = floatArrayOf(cur[5], cur[7]); val fs = floatArrayOf(cur[6], cur[8])
        val ts = floatArrayOf(cur[9], cur[11]); val ss = floatArrayOf(cur[10], cur[12])
        val fos = floatArrayOf(cur[14], cur[15])
        val sox = FloatArray(2); val soy = FloatArray(2); val hox = FloatArray(2); val hoy = FloatArray(2)
        val ex1 = FloatArray(2); val ey1 = FloatArray(2); val wx1 = FloatArray(2); val wy1 = FloatArray(2)
        val dx1 = FloatArray(2); val dy1 = FloatArray(2)
        val kx = FloatArray(2); val ky = FloatArray(2); val ax = FloatArray(2); val ay = FloatArray(2)
        val fda = FloatArray(2)
        var minX = hxp; var maxX = hxp
        for (s in 0..1) {
            val sg = if (s == 0) 1f else -1f
            sox[s] = sx + (if (front) sg * 0.085f * L else 0f); soy[s] = sy
            hox[s] = hxp + (if (front) sg * 0.040f * L else 0f); hoy[s] = hyp
            ex1[s] = sox[s] + dnx(us[s]) * ua; ey1[s] = soy[s] + dny(us[s]) * ua
            wx1[s] = ex1[s] + dnx(fs[s]) * fa; wy1[s] = ey1[s] + dny(fs[s]) * fa
            dx1[s] = wx1[s] + dnx(fs[s]) * hand; dy1[s] = wy1[s] + dny(fs[s]) * hand
            kx[s] = hox[s] + dnx(ts[s]) * th; ky[s] = hoy[s] + dny(ts[s]) * th
            ax[s] = kx[s] + dnx(ss[s]) * sh; ay[s] = ky[s] + dny(ss[s]) * sh
            val fo = if (s == 1 && front) -fos[s] else fos[s]
            fda[s] = ss[s] + fo
            minX = min(minX, min(ax[s], dx1[s])); maxX = maxOf(maxX, maxOf(ax[s], dx1[s]), sx)
        }

        // ---- props
        if (!e.bust) {
            g.roundRect(0f, floorY - 0.0025f * unit, w, floorY + 0.0025f * unit, 0f, Pal.floorLine)
            if (e.hasMat) {
                val l = mx(ox + e.matX0 * unit); val r = mx(ox + e.matX1 * unit)
                g.roundRect(min(l, r), floorY - 0.002f * unit, maxOf(l, r), floorY + 0.022f * unit, 0.011f * unit, Pal.mat)
            }
            if (e.bar >= 0f) {
                val by = oy + e.bar * unit
                g.roundRect(w * 0.08f, by - 0.007f * unit, w * 0.92f, by + 0.007f * unit, 0.007f * unit, Pal.bar)
            }
            if (e.wall >= 0f) {
                val wl = mx(ox + e.wall * unit); val wr = mx(ox + e.wall * unit + 0.03f * unit)
                g.roundRect(min(wl, wr), h * 0.04f, maxOf(wl, wr), floorY, 0f, Pal.wall)
            }
            // contact shadow
            val scx = (minX + maxX) / 2f; val sw = ((maxX - minX) * 0.5f + 0.10f * unit).coerceAtMost(0.5f * unit)
            oval(g, scx, floorY + 0.004f * unit, 1f, 0f, sw, 0.016f * unit, Pal.shadow)
        }

        val wide = if (front) 1.38f else 1.0f
        // ---- far arm
        arm(g, sox[1], soy[1], ex1[1], ey1[1], wx1[1], wy1[1], fs[1], L, Pal.skinF, false)
        // ---- far leg
        leg(g, hox[1], hoy[1], kx[1], ky[1], ax[1], ay[1], fda[1], L, Pal.skinF, Pal.shortsF, Pal.shoeF, Pal.soleF)
        // ---- torso (shirt) + pelvis shorts
        val p1x = hxp + tdx * 0.12f * L; val p1y = hyp + tdy * 0.12f * L
        val p2x = hxp + tdx * 0.22f * L; val p2y = hyp + tdy * 0.22f * L
        val p3x = sx - tdx * 0.01f * L; val p3y = sy - tdy * 0.01f * L
        hull(g, hxp, hyp, 0.062f * L * wide, p1x, p1y, 0.054f * L * wide, Pal.red)
        hull(g, p1x, p1y, 0.054f * L * wide, p2x, p2y, 0.074f * L * wide, Pal.red)
        hull(g, p2x, p2y, 0.074f * L * wide, p3x, p3y, 0.058f * L * wide, Pal.red)
        hull(g, hxp, hyp, 0.062f * L * wide, hxp + tdx * 0.05f * L, hyp + tdy * 0.05f * L, 0.058f * L * wide, Pal.shorts)
        // ---- neck + head
        val hdx = upx(hA); val hdy = upy(hA)
        hull(g, sx, sy, 0.032f * L, cx - hdx * hr * 0.55f, cy - hdy * hr * 0.55f, 0.028f * L, Pal.skinF)
        if (!front) headSide(g, cx, cy, hr, hA, cur[13]) else headFront(g, cx, cy, hr, hA)
        // ---- near leg
        leg(g, hox[0], hoy[0], kx[0], ky[0], ax[0], ay[0], fda[0], L, Pal.skin, Pal.shorts, Pal.shoe, Pal.sole)
        // ---- near arm
        arm(g, sox[0], soy[0], ex1[0], ey1[0], wx1[0], wy1[0], fs[0], L, Pal.skin, true)
        if (front) circ(g, sox[1], soy[1], 0.044f * L, Pal.skin)

        // ---- rope
        if (e.rope && cur[16] > -900f) {
            val R = 0.50f * L; val a = cur[16]
            val x0 = dx1[0]; val y0 = dy1[0]
            val p1x2 = x0 + R * 1.3f * sin(rad(a + 40f)); val p1y2 = y0 - R * 1.3f * cos(rad(a + 40f))
            val p2x2 = x0 + R * 1.3f * sin(rad(a - 40f)); val p2y2 = y0 - R * 1.3f * cos(rad(a - 40f))
            val n = 28
            for (i in 0..n) {
                val t = i / n.toFloat(); val u = 1 - t
                buf[2 * i] = mx(u * u * u * x0 + 3 * u * u * t * p1x2 + 3 * u * t * t * p2x2 + t * t * t * x0)
                buf[2 * i + 1] = u * u * u * y0 + 3 * u * u * t * p1y2 + 3 * u * t * t * p2y2 + t * t * t * y0
            }
            g.polyline(buf, n + 1, 0.007f * unit, Pal.red)
        }
    }

    private fun arm(g: Gfx, sx: Float, sy: Float, ex: Float, ey: Float, wx: Float, wy: Float, fAng: Float, L: Float, skin: Int, near: Boolean) {
        circ(g, sx, sy, 0.044f * L, skin)
        limb(g, sx, sy, 0.040f * L, ex, ey, 0.031f * L, 0.35f, 0.043f * L, skin)
        limb(g, ex, ey, 0.031f * L, wx, wy, 0.022f * L, 0.30f, 0.033f * L, skin)
        val hx = wx + dnx(fAng) * 0.034f * L; val hy = wy + dny(fAng) * 0.034f * L
        oval(g, hx, hy, dnx(fAng), dny(fAng), 0.038f * L, 0.025f * L, skin)
    }

    private fun leg(g: Gfx, hx: Float, hy: Float, kx: Float, ky: Float, ax: Float, ay: Float, fAng: Float, L: Float,
                    skin: Int, shorts: Int, shoe: Int, sole: Int) {
        limb(g, hx, hy, 0.058f * L, kx, ky, 0.040f * L, 0.40f, 0.058f * L, skin)
        limb(g, kx, ky, 0.040f * L, ax, ay, 0.024f * L, 0.30f, 0.043f * L, skin)
        shoe(g, ax, ay, fAng, L * 1.25f, shoe, sole)
        val sx2 = hx + (kx - hx) * 0.50f; val sy2 = hy + (ky - hy) * 0.50f
        hull(g, hx, hy, 0.064f * L, sx2, sy2, 0.058f * L, shorts)
    }

    private fun headSide(g: Gfx, cx: Float, cy: Float, hr: Float, hA: Float, jaw: Float) {
        val r = rad(hA)
        val fx = cos(r); val fy = sin(r); val ux = sin(r); val uy = -cos(r)
        shape(g, Shapes.upper, cx, cy, fx, fy, ux, uy, hr, Pal.skin)
        val ang = rad(jaw)
        val hf = -0.30f; val hu = -0.40f
        val jn = Shapes.jaw.size / 2
        val rj = FloatArray(Shapes.jaw.size)
        for (i in 0 until jn) {
            val df = Shapes.jaw[2 * i] - hf; val du = Shapes.jaw[2 * i + 1] - hu
            rj[2 * i] = hf + df * cos(ang) + du * sin(ang)
            rj[2 * i + 1] = hu - df * sin(ang) + du * cos(ang)
        }
        if (jaw > 2f) {
            val qx = FloatArray(8)
            for (i in 0 until 2) {
                val pf = if (i == 0) 0.30f else 0.95f; val pu = if (i == 0) -0.26f else -0.54f
                val df = pf - hf; val du = pu - hu
                qx[if (i == 0) 4 else 6] = hf + df * cos(ang) + du * sin(ang)
                qx[if (i == 0) 5 else 7] = hu - df * sin(ang) + du * cos(ang)
            }
            val m = floatArrayOf(0.95f, -0.54f, 0.15f, -0.30f, qx[4], qx[5], qx[6], qx[7])
            shape(g, m, cx, cy, fx, fy, ux, uy, hr, Pal.mouth)
        }
        shape(g, rj, cx, cy, fx, fy, ux, uy, hr, Pal.skin)
        // ear
        oval(g, cx + (fx * -0.20f + ux * -0.12f) * hr, cy + (fy * -0.20f + uy * -0.12f) * hr, ux, uy, 0.27f * hr, 0.17f * hr, Pal.ear)
        // eye and mouth line
        circ(g, cx + (fx * 0.56f + ux * 0.16f) * hr, cy + (fy * 0.56f + uy * 0.16f) * hr, 0.075f * hr, Pal.hair)
        if (jaw <= 2f) {
            val m1x = cx + (fx * 0.93f + ux * -0.56f) * hr; val m1y = cy + (fy * 0.93f + uy * -0.56f) * hr
            val m2x = cx + (fx * 0.62f + ux * -0.57f) * hr; val m2y = cy + (fy * 0.62f + uy * -0.57f) * hr
            buf[0] = mx(m1x); buf[1] = m1y; buf[2] = mx(m2x); buf[3] = m2y
            g.polyline(buf, 2, 0.045f * hr, Pal.skinF)
        }
        shape(g, Shapes.hair, cx, cy, fx, fy, ux, uy, hr, Pal.hair)
    }

    private fun headFront(g: Gfx, cx: Float, cy: Float, hr: Float, hA: Float) {
        val r = rad(hA)
        val fx = cos(r); val fy = sin(r); val ux = sin(r); val uy = -cos(r)
        circ(g, cx + fx * -0.93f * hr, cy + fy * -0.93f * hr, 0.17f * hr, Pal.ear)
        circ(g, cx + fx * 0.93f * hr, cy + fy * 0.93f * hr, 0.17f * hr, Pal.ear)
        oval(g, cx, cy, ux, uy, 1.08f * hr, 0.90f * hr, Pal.skin)
        shape(g, Shapes.hairFront, cx, cy, fx, fy, ux, uy, hr, Pal.hair)
    }
}
