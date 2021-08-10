package streamsBlendingRecommender

import java.util.function.DoubleBinaryOperator
import kotlin.math.*

abstract class AbstractSBR {

    fun getValue(): AbstractSBR {
        return this
    };

    fun norm(mix: Map<String, Double>, spec: String = "euclidean"): Double {
        return this.norm(mix.values, spec)
    }

    fun norm(vec: Collection<Double>, spec: String = "euclidean"): Double {
        return when (spec) {
            "max-norm" -> { vec.map { abs(it) }.maxOrNull()!! }
            "one-norm" -> { vec.map { abs(it) }.sum() }
            "euclidean" -> { sqrt(vec.map { it * it }.sum()) }
            else -> {
                System.err.println("None of the tags is known in the recommender.")
                0.0
            }
        }
    }
}