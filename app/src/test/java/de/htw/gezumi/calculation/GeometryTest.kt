package de.htw.gezumi.calculation

import org.junit.Assert
import org.junit.Test

class GeometryTest {
    @Test
    fun determineMatch_isCorrect() {
        Assert.assertTrue(
            Geometry.determineMatch(
                GamePositions(
                    listOf(
                        Vec(0, 0),
                        Vec(-4, -8),
                        Vec(5.1f, 6.8f)
                    ),
                    listOf(
                        Vec(5, 7),
                        Vec(-0.1f, 0.2f),
                        Vec(-4, -8)
                    ),
                )
            )
        )
        Assert.assertFalse(
            Geometry.determineMatch(
                GamePositions(
                    listOf(
                        Vec(0, 0),
                        Vec(-4, -27),
                        Vec(5.1f, 6.8f)
                    ),
                    listOf(
                        Vec(5, 7),
                        Vec(-0.1f, 0.2f),
                        Vec(-4, -8)
                    ),
                )
            )
        )
    }

}