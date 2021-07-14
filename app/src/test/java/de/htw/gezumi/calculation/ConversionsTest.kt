package de.htw.gezumi.calculation

import org.junit.Assert
import org.junit.Test


class ConversionsTest {

    @Test
    fun rssiToDistances_isCorrect() {
        val dist1 = Conversions.rssiToDistance(-55f, -19)
        val dist2 = Conversions.rssiToDistance(-65f, -19)
        Assert.assertTrue(dist1 < dist2)
    }

    @Test
    fun distancesToPoints_isCorrect() {
        val positions = Conversions.distancesToPoints(
            arrayOf(
                floatArrayOf(0f, 2f, 1.5f),
                floatArrayOf(2f, 0f, 1.5f),
                floatArrayOf(1.5f, 1.5f, 0f),
            )
        )
        Assert.assertEquals(positions[0].getDist(positions[1]), 2f, 0.0002f)
        Assert.assertEquals(positions[1].getDist(positions[2]), 1.5f, 0.0002f)
        Assert.assertEquals(positions[0].getDist(positions[2]), 1.5f, 0.0002f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun distancesToPoints_throwsExceptionsOnInvalidInput() {
        Conversions.distancesToPoints(
            arrayOf(
                floatArrayOf(0f, 5f, 1.5f),
                floatArrayOf(5f, 0f, 1.5f),
                floatArrayOf(1.5f, 1.5f, 0f),
            )
        )
    }
}