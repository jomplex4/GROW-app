package com.digitalminds.grow

class Ex(
    val id: String, val name: String, val desc: String, val tips: List<String>,
    val pillar: String, val met: Float, val sided: Boolean, val loop: Float, val ease: Int,
    val front: Boolean, val bust: Boolean, val sc: Float,
    val bar: Float, val wall: Float, val matX0: Float, val matX1: Float, val hasMat: Boolean,
    val rope: Boolean, val poses: Array<FloatArray>, val nameEs: String = ""
)

class Step(val ex: Ex, val sec: Int, val rest: Int, val mirror: Boolean, val tag: Char) {
    val title: String get() = if (ex.sided) ex.name + (if (mirror) " LEFT" else " RIGHT") else ex.name
}
