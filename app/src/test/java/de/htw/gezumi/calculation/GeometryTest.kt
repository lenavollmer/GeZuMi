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


    @Test
    fun generateGeometricObject_isCorrect() {
        val shape = Geometry.generateGeometricObject(3)
        Assert.assertEquals(shape.size, 3)
    }

    @Test
    fun arrangeGamePositions_isCorrect() {
        val gamePositions = Geometry.arrangeGamePositions(
            GamePositions(
                listOf(
                    Vec(2, 2),
                    Vec(1, 1),
                    Vec(2, 1),
                ), listOf(
                    Vec(-1, 0),
                    Vec(0, -1),
                    Vec(0, 0),
                )
            )
        )
        Assert.assertEquals(
            gamePositions.players, gamePositions.targets
        )
    }

    @Test
    fun scaleGamePositions_isCorrect() {
        val gamePositions = Geometry.scaleGamePositions(
            GamePositions(
                listOf(
                    Vec(0, 0),
                    Vec(10, 10),
                    Vec(5, 5),
                ),
                listOf(
                    Vec(0, 0),
                    Vec(10, 10),
                    Vec(5, 5),
                ),
            ),
            100,
            100,
            0
        )
        Assert.assertEquals(
            gamePositions.players, gamePositions.targets
        )
        Assert.assertEquals(
            gamePositions.players,  listOf(
                Vec(0, 0),
                Vec(100, 100),
                Vec(50, 50),
            )
        )
    }
}