package streamsBlendingRecommender

import java.util.function.DoubleBinaryOperator
import kotlin.math.*

abstract class AbstractSBR {

    fun getValue(): AbstractSBR {
        return this
    };

    //========================================================
    // Inverse index merging functions
    // ========================================================
    /**
     * This function merges two (hash-)maps into a third one.
     * The two hash-maps are converted into sequences then the merge/sum of values
     * is done using a group-by operation over the keys and summation for each group.
     *
     * @param first A (hash-)map.
     * @param second A (hash-)map.
     * @return A (hash-)map.
     */
    fun myMerge(first: Map<String?, Double?>, second: Map<String?, Double?>): Map<String?, Double?> {
        return (first.asSequence() + second.asSequence())
            .groupBy({ it.key!! }, { it.value!! })
            .mapValues { it.value.sum() }
    }

    /**
     * Merging of maps through reduction operations.
     */
    fun <K, V> Map<K, V>.mergeReduce(reduce: (V, V) -> V, others: List<Map<K, V>>): Map<K, V> =
        this.toMutableMap().apply { others.forEach { other -> other.forEach { merge(it.key, it.value, reduce) } } }

    /**
     * Merging if maps with for-each operations.
     */
    fun <K, V> MutableMap<K, V>.mergeReduceInPlace(reduce: (V, V) -> V, others: List<Map<K, V>>) =
        others.forEach { other -> other.forEach { merge(it.key, it.value, reduce) } }

    //========================================================
    // Norm computation
    //========================================================
    /**
     * Norm of a map.
     * @see norm
     */
    fun norm(mix: Map<String, Double>, spec: String = "euclidean"): Double {
        return this.norm(mix.values, spec)
    }

    /**
     * Norm of a vector
     *
     * @param vec A collection of doubles to be normalized.
     * @param spec Norm type identifier, one of "euclidean", "max-norm", "one-norm".
     */
    fun norm(vec: Collection<Double>, spec: String = "euclidean"): Double {
        return when (spec) {
            "max-norm" -> {
                vec.map { abs(it) }.maxOrNull()!!
            }
            "one-norm" -> {
                vec.map { abs(it) }.sum()
            }
            "euclidean" -> {
                sqrt(vec.map { it * it }.sum())
            }
            else -> {
                System.err.println("None of the tags is known in the recommender.")
                0.0
            }
        }
    }
}