package de.htw.gezumi.canvas

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import de.htw.gezumi.calculation.Vec


class Animator {
    companion object {
        fun createGameAnimation(
            currentPlayers: List<Vec>,
            nextPlayers: List<Vec>,
            currentTarget: List<Vec>,
            nextTarget: List<Vec>,
            onUpdate: (players: List<Vec>, targets: List<Vec>) -> Unit
        ): ValueAnimator {

            val playerProperties = generatePropertiesVec("players", currentPlayers, nextPlayers)
            val targetProperties = generatePropertiesVec("targets", currentTarget, nextTarget)

            val animator = ValueAnimator()
            animator.setValues(*(playerProperties.map { it.x } + playerProperties.map { it.y }
                    + targetProperties.map { it.x } + targetProperties.map { it.y }).toTypedArray())
            animator.duration = 1000

            animator.addUpdateListener { animation ->
                val players = playerProperties.map {
                    Vec(
                        animation.getAnimatedValue(it.x.propertyName) as Float,
                        animation.getAnimatedValue(it.y.propertyName) as Float
                    )
                }
                val targets = targetProperties.map {
                    Vec(
                        animation.getAnimatedValue(it.x.propertyName) as Float,
                        animation.getAnimatedValue(it.y.propertyName) as Float
                    )
                }
                onUpdate(players, targets)
            }
            animator.start()
            return animator

        }

        private fun generatePropertiesVec(
            prefix: String,
            points: List<Vec>,
            nextPoints: List<Vec>
        ): List<VecValueHolder> = points.mapIndexed { i, point ->
            VecValueHolder(
                PropertyValuesHolder.ofFloat(
                    "${prefix}X$i",
                    point.x,
                    nextPoints[i].x
                ), PropertyValuesHolder.ofFloat(
                    "${prefix}Y$i",
                    point.y,
                    nextPoints[i].y
                )
            )
        }

    }
}

data class VecValueHolder(val x: PropertyValuesHolder, val y: PropertyValuesHolder)


