package de.htw.gezumi.calculation

import org.junit.Assert
import org.junit.Test
import java.lang.Float.NaN

class VecTest {
    @Test
    fun dot_isCorrect() {
        val product = Vec(2, 3).dot(Vec(3, 2))
        Assert.assertEquals(product, 12f)
    }

    @Test
    fun length_isCorrect() {
        val length = Vec(6f, -2.5f).length()
        Assert.assertEquals(length, 6.5f)
    }

    @Test
    fun isNan_isCorrect() {
        val isNan = Vec(NaN, -2.5f).isNan()
        Assert.assertEquals(isNan, true)
        val isNotNan = Vec(2f, -2.5f).isNan()
        Assert.assertEquals(isNotNan, false)
    }

    @Test
    fun unaryMinus_isCorrect() {
        val vector = -Vec(6f, -2.5f)
        Assert.assertEquals(vector, Vec(-6f, 2.5f))
    }

    @Test
    fun plus_isCorrect() {
        val vector = Vec(6f, -2.5f) + 1f
        Assert.assertEquals(vector, Vec(7f, -1.5f))
        val vector2 = Vec(6f, -2.5f) + Vec(-6f, 2.5f)
        Assert.assertEquals(vector2, Vec(0, 0))
    }

    @Test
    fun minus_isCorrect() {
        val vector = Vec(6f, -2.5f) - 1f
        Assert.assertEquals(vector, Vec(5f, -3.5f))
        val vector2 = Vec(6f, -2.5f) - Vec(6f, -2.5f)
        Assert.assertEquals(vector2, Vec(0, 0))
    }

    @Test
    fun times_isCorrect() {
        val vector = Vec(6f, -2.5f) * 2f
        Assert.assertEquals(vector, Vec(12, -5))
    }

}