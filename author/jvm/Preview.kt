import com.digitalminds.grow.*
import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class G2(val g: Graphics2D) : Gfx {
    private fun col(c: Int) = Color(c, true)
    override fun poly(p: FloatArray, n: Int, color: Int) {
        val path = Path2D.Float(); path.moveTo(p[0].toDouble(), p[1].toDouble())
        for (i in 1 until n) path.lineTo(p[2 * i].toDouble(), p[2 * i + 1].toDouble())
        path.closePath(); g.color = col(color); g.fill(path)
    }
    override fun circle(cx: Float, cy: Float, r: Float, color: Int) { g.color = col(color); g.fill(Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r)) }
    override fun polyline(p: FloatArray, n: Int, w: Float, color: Int) {
        val path = Path2D.Float(); path.moveTo(p[0].toDouble(), p[1].toDouble())
        for (i in 1 until n) path.lineTo(p[2 * i].toDouble(), p[2 * i + 1].toDouble())
        g.color = col(color); g.stroke = BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); g.draw(path)
    }
    override fun roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int) {
        g.color = col(color); g.fill(RoundRectangle2D.Float(l, t, r - l, b - t, 2 * rad, 2 * rad))
    }
    override fun gradRoundRect(l: Float, t: Float, r: Float, b: Float, rad: Float, c1: Int, c2: Int) {
        g.paint = GradientPaint(0f, t, col(c1), 0f, b, col(c2)); g.fill(RoundRectangle2D.Float(l, t, r - l, b - t, 2 * rad, 2 * rad))
    }
}

fun main(args: Array<String>) {
    val lines = File("/tmp/ex_test.txt").readLines()
    val exs = ArrayList<Ex>()
    var i = 0
    while (i < lines.size) {
        val h = lines[i].split('|'); i++
        val n = h[14].toInt()
        val poses = Array(n) { k -> lines[i + k].substring(2).split(',').map { it.toFloat() }.toFloatArray() }
        i += n
        exs.add(Ex(h[1], h[2], "", emptyList(), "", 0f, false, h[6].toFloat(), h[7].toInt(), h[3] == "1", h[4] == "1", h[5].toFloat(),
            h[8].toFloat(), h[9].toFloat(), h[11].toFloat(), h[12].toFloat(), h[10] == "1", h[13] == "1", poses))
    }
    if (args.getOrNull(0) == "ref") {
        val e = exs.first { it.id == args[1] }
        Pal.floorLine = 0; Pal.shadow = 0
        val cw = 1024; val ch = 1024
        val im = BufferedImage(cw * 2, ch * 3, BufferedImage.TYPE_INT_RGB)
        val g0 = im.createGraphics()
        g0.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g0.color = Color(0xD9, 0xD9, 0xD9); g0.fillRect(0, 0, im.width, im.height)
        val rr = FigureRenderer()
        for (k in 0 until 6) {
            val gg = g0.create() as Graphics2D
            gg.translate((k % 2) * cw, (k / 2) * ch)
            gg.clip = Rectangle(0, 0, cw, ch)
            val zm = (args.getOrNull(3) ?: "1.0").toDouble()
            gg.translate(cw / 2.0, ch * 0.92); gg.scale(zm, zm); gg.translate(-cw / 2.0, -ch * 0.92)
            gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            rr.draw(G2(gg), e, k / 6f, cw.toFloat(), ch.toFloat(), false, false, 0f)
            gg.dispose()
        }
        g0.dispose()
        ImageIO.write(im, "png", File(args[2])); println("ref done"); return
    }
    if (args.getOrNull(0) == "frames") {
        val ids2 = args[1].split(',').filter { it.isNotBlank() }
        val nf = args[2].toInt(); val dir = File(args[3]); dir.mkdirs()
        val W2 = 240; val H2 = 300; val S2 = 2
        val r2 = FigureRenderer()
        for (e in exs.filter { it.id in ids2 }) {
            for (k in 0 until nf) {
                val im = BufferedImage(W2 * S2, H2 * S2, BufferedImage.TYPE_INT_ARGB)
                val gg = im.createGraphics()
                gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                gg.color = Color.BLACK; gg.fillRect(0, 0, im.width, im.height)
                r2.draw(G2(gg), e, k.toFloat() / nf, im.width.toFloat(), im.height.toFloat(), false, true, 18f * S2)
                gg.dispose()
                ImageIO.write(im, "png", File(dir, "${e.id}_${"%02d".format(k)}.png"))
            }
        }
        println("frames done"); return
    }
    val ids = args.getOrNull(0)?.split(',')?.filter { it.isNotBlank() }
    val out = args.getOrNull(1) ?: "/tmp/preview.png"
    val sel = if (ids == null || ids.isEmpty()) exs else exs.filter { it.id in ids }
    val W = 300; val H = 360; val S = 2
    val cols = 6
    val frames = sel.map { e -> e to (if (e.poses.size > 1) listOf(0, 1).map { it.toFloat() / e.poses.size + 0.001f } else listOf(0f)) }
    // one tile per exercise: show frame index 1 (or 0)
    val rows = (sel.size + cols - 1) / cols
    val img = BufferedImage(W * cols * S, (H + 22) * rows * S, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2.color = Color(0, 0, 0); g2.fillRect(0, 0, img.width, img.height)
    val r = FigureRenderer()
    for ((idx, e) in sel.withIndex()) {
        val ox = (idx % cols) * W * S; val oy = (idx / cols) * (H + 22) * S
        val gg = g2.create() as Graphics2D
        gg.translate(ox, oy + 22 * S)
        gg.clip = Rectangle(0, 0, W * S, H * S)
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val pos = if (e.poses.size > 1) 1f / e.poses.size + 0.0001f else 0f
        r.draw(G2(gg), e, pos, (W * S).toFloat() - 12f * S, (H * S).toFloat(), false, true, 18f * S)
        gg.dispose()
        g2.color = Color.WHITE; g2.font = Font("SansSerif", Font.BOLD, 11 * S)
        g2.drawString(e.name, ox + 8 * S, oy + 15 * S)
    }
    g2.dispose()
    ImageIO.write(img, "png", File(out))
    println("rendered ${sel.size} to $out")
}
