package de.htw.gezumi.canvas

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import de.htw.gezumi.calculation.Vec


class Animator {
    companion object {
        fun createVecAnimation(
            from: List<Vec>,
            to: List<Vec>,
            duration: Long = 2000,
            onUpdate: (updatedVecs: List<Vec>) -> Unit
        ): ValueAnimator {

            val properties = generatePropertiesVec(from, to)

            val animator = ValueAnimator()
            animator.setValues(*(properties.map { it.x } + properties.map { it.y }
                    ).toTypedArray())
            animator.duration = duration


            animator.addUpdateListener { animation ->
                val updatedVecs = properties.map {
                    Vec(
                        animation.getAnimatedValue(it.x.propertyName) as Float,
                        animation.getAnimatedValue(it.y.propertyName) as Float
                    )
                }
                onUpdate(updatedVecs)
            }
            animator.start()
            return animator

        }

        private fun generatePropertiesVec(
            points: List<Vec>,
            nextPoints: List<Vec>
        ): List<VecValueHolder> = points.mapIndexed { i, point ->
            VecValueHolder(
                PropertyValuesHolder.ofFloat(
                    "X$i",
                    point.x,
                    nextPoints[i].x
                ), PropertyValuesHolder.ofFloat(
                    "Y$i",
                    point.y,
                    nextPoints[i].y
                )
            )
        }

    }
}

data class VecValueHolder(val x: PropertyValuesHolder, val y: PropertyValuesHolder)


