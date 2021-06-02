package de.htw.gezumi.calculation

import android.graphics.Point
import kotlin.math.sqrt

data class Vec(val x: Float = 0f, val y: Float = 0f) {

    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

    constructor(point: Point) : this(point.x, point.y)

    fun toPoint(): Point = Point(x.toInt(), y.toInt())

    fun dot(b: Vec) = x * b.x + y * b.y

    fun length() = sqrt((x * x) + (y * y))

    operator fun unaryMinus() = Vec(-x, -y)

    operator fun plus(v: Float) = Vec(x + v, y + v)
    operator fun plus(v: Vec) = Vec(x + v.x, y + v.y)
    operator fun minus(v: Float) = Vec(x - v, y - v)
    operator fun minus(v: Vec) = Vec(x - v.x, y - v.y)
    operator fun times(v: Float) = Vec(x * v, y * v)
}