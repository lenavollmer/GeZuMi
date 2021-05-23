package de.htw.gezumi.calculation

import android.graphics.Point

data class Vec(val x: Double = 0.0, val y: Double = 0.0) {

    constructor(point: Point) : this(point.x.toDouble(), point.y.toDouble())

    fun toPoint(): Point = Point(x.toInt(), y.toInt())

    fun dot(b: Vec) = x * b.x + y * b.y

    operator fun unaryMinus() = Vec(-x, -y)

    operator fun plus(v: Double) = Vec(x + v, y + v)
    operator fun plus(v: Vec) = Vec(x + v.x, y + v.y)
    operator fun minus(v: Double) = Vec(x - v, y - v)
    operator fun minus(v: Vec) = Vec(x - v.x, y - v.y)
    operator fun times(v: Double) = Vec(x * v, y * v)
}