package streamsBlendingRecommender

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import kotlin.math.*

class CoreSBR() : AbstractSBR {

    //========================================================
    // Data members
    //========================================================
    var smrMatrix: ArrayList<Map<String, String?>> = arrayListOf()

    var itemInverseIndexes: Map<String?, Map<String?, Double?>?> = mutableMapOf()
    var tagInverseIndexes: Map<String?, Map<String?, Double?>> = mutableMapOf()

    var tagTypeToTags: Map<String?, Set<String?>> = mapOf();
    var globalWeights: Map<String, Double> = mapOf();

    var knownTags: Set<String?> = setOf();
    var knownItems: Set<String?> = setOf();

    //========================================================
    // Getters
    //========================================================
    override fun getValue(): AbstractSBR {
        return this;
    }

    //========================================================
    // Clone
    //========================================================
    /**
     * Deep copy.
     */
    fun clone(): CoreSBR {
        val sbr = CoreSBR()
        sbr.smrMatrix = this.smrMatrix

        sbr.itemInverseIndexes = this.itemInverseIndexes
        sbr.tagInverseIndexes = this.tagInverseIndexes

        sbr.tagTypeToTags = this.tagTypeToTags
        sbr.globalWeights = this.globalWeights

        sbr.knownTags = this.knownTags
        sbr.knownItems = this.knownItems

        return sbr
    }

    //========================================================
    // Ingest a SMR matrix CSV file
    //========================================================
    /**
     * Ingest a CSV file representing a Sparse Matrix Recommender (SMR) matrix.
     *
     * @param fileName File name.
     * @param itemColumnName Name of the column with items.
     * @param tagTypeColumnName Name of the column tag types.
     * @param valueColumnName Name of the column values (i.e. tags.)
     * @param weightColumnName Name of the column with item-value weights.
     * @param make Should the (tag) inverse indexes be create or not?
     * @return Is the ingestion successful or not?
     */
    fun ingestSMRMatrixCSVFile(
        fileName: String,
        itemColumnName: String = "Item",
        tagTypeColumnName: String = "TagType",
        valueColumnName: String = "Value",
        weightColumnName: String = "Weight",
        make: Boolean = false
    ): Boolean {

        val csvRows: List<Map<String, String>> = csvReader().readAllWithHeader(File(fileName))

        val expectedColumnNames: Set<String> =
            setOf(itemColumnName, tagTypeColumnName, valueColumnName, weightColumnName)

        if (expectedColumnNames.intersect(csvRows[0].keys).size < expectedColumnNames.size) {
            System.err.println(
                "The ingested CSV file does not have the expected column names:" + expectedColumnNames.joinToString(
                    ","
                ) + "."
            )
            return false
        }

        this.smrMatrix = arrayListOf()
        for (row in csvRows) {
            val newRow = mapOf(
                "Item" to row[itemColumnName],
                "TagType" to row[tagTypeColumnName],
                "Value" to row[valueColumnName],
                "Weight" to row[weightColumnName]
            )
            this.smrMatrix.add(newRow)
        };

        this.itemInverseIndexes = mapOf();
        this.tagInverseIndexes = mapOf();

        if (make) {
            this.makeTagInverseIndexes()
        }

        return true;
    }

    //========================================================
    //  Make tag inverse indexes
    //========================================================
    /**
     * Make tag inverse indexes.
     * These indexes (hash-maps) correspond to the columns of a sparse matrix.
     */
    fun makeTagInverseIndexes(): Boolean {

        // Split into a hash by tag type.
        var inverseIndexGroups = this.smrMatrix.groupBy { it["TagType"] };

        // For each tag type split into hash by Value.
        var inverseIndexesPerTagType = inverseIndexGroups.mapValues { it0 -> it0.value.groupBy { it["Value"] } }

        // Re-make each array of hashes into a hash.
        // println( inverseIndexesPerTagType.mapValues{ typeRec -> typeRec.value.mapValues{ it.value.size } } )

        var inverseIndexesPerTagType2 = inverseIndexesPerTagType.mapValues { typeRec ->
            typeRec.value.mapValues { tagRec ->
                tagRec.value.associateBy(
                    { it["Item"] },
                    { it["Weight"]!!.toDouble() })
            }
        }

        // Derive the tag type to tags hash map.
        this.tagTypeToTags = inverseIndexesPerTagType2.mapValues { it.value.keys.toSet() }

        // Flatten the inverse index groups.
        this.tagInverseIndexes = mutableMapOf();
        for (pair in inverseIndexesPerTagType2) {
            this.tagInverseIndexes = this.tagInverseIndexes.plus(pair.value)
        }

        // Assign known tags.
        this.knownTags = this.tagInverseIndexes.keys;

        // We make sure item inverse indexes are empty.
        this.itemInverseIndexes = mapOf();

        return true;
    }

    //========================================================
    // Transpose tag inverse indexes
    //========================================================
    /**
     * Transpose the tag inverse indexes into item inverse indexes.
     * This operation corresponds to changing the representation of sparse matrix
     * from column major to row major format.
     */
    fun transposeTagInverseIndexes(): Boolean {

        // Transpose tag inverse indexes into item inverse indexes.

        var items = this.tagInverseIndexes.values.fold(mapOf<String?, Double?>()) { acc, value -> acc + value }.keys

        this.itemInverseIndexes = items.map { it -> Pair(it, mutableMapOf<String?, Double?>()) }.toMap().toMutableMap()

        for (tIndex in this.tagInverseIndexes) {
            for (pair in tIndex.value) {
                this.itemInverseIndexes = this.itemInverseIndexes!!.plus(
                    Pair(
                        pair.key,
                        this.itemInverseIndexes[pair.key]?.plus(Pair(tIndex.key, pair.value))
                    )
                )
            }
        }

        // Assign known items.
        this.knownItems = this.itemInverseIndexes.keys;

        return true;
    }

    //========================================================
    // Profile
    //========================================================
    /**
     * Compute profile for an array of items.
     * Makes a map from the array and delegates to the corresponding overloaded version.
     *
     * @param items An string array of items.
     * @param normalize A Boolean: should the recommendations be normalized or not?
     * @param warn A Boolean: should warning messages be given or not?
     * @return A list of string-double pairs sorted in descending order of their values.
     * @see profile
     */
    fun profile(
        items: Array<String>,
        normalize: Boolean = false,
        warn: Boolean = true
    ): List<Pair<String?, Double?>> {

        var arrOfVals: Array<Double> = Array(items.size) { 1.0 };

        val scoredItems: Map<String, Double> = items.zip(arrOfVals).toMap();

        return this.profile(scoredItems, normalize, warn)
    }

    /**
     * Compute profile for scored items.
     *
     * @param items An string array of items.
     * @param normalize A Boolean: should the recommendations be normalized or not?
     * @param warn A Boolean: should warning messages be given or not?
     * @return A list of string-double pairs sorted in descending order of their values.
     */
    fun profile(
        items: Map<String, Double>,
        normalize: Boolean = false,
        warn: Boolean = true
    ): List<Pair<String?, Double?>> {

        // Transpose inverse indexes if needed
        if (this.itemInverseIndexes.isEmpty()) {
            this.transposeTagInverseIndexes()
        }

        // Make sure items are known
        var itemsQuery = items.filterKeys { this.knownItems.contains(it) }

        if (itemsQuery.isEmpty() && warn) {
            System.err.println("None of the items is known in the recommender.")
            return listOf()
        }

        if (itemsQuery.size < items.size && warn) {
            System.err.println("Some of the items are unknown in the recommender.")
        }

        // Compute the profile

        // Get the item inverse indexes and multiply their value by the corresponding item weight
        val weightedItemIndexes: List<Pair<String, Map<String?, Double?>?>> =
            itemsQuery.map { itemPair ->
                Pair(
                    itemPair.key,
                    this.itemInverseIndexes[itemPair.key]?.mapValues { it.value?.times(itemPair.value) })
            }

        // Reduce the maps of maps into one map by adding the weights that correspond to the same keys
        var itemMix: Map<String?, Double?> =
            weightedItemIndexes.fold(mapOf<String?, Double?>()) { acc, index -> myMerge(acc, index.second!!) }

        // Normalize
        //if ( normalize ) { itemMix = this.normalize( itemMix, "max-norm") }

        // Sorted result
        return itemMix.toList().sortedByDescending { it.second }
    }

    //========================================================
    // Recommend by history
    //========================================================
    /**
     * Compute recommendations by array items.
     * Makes a map from the array and delegates to the corresponding overloaded version.
     *
     * @param items An string array of items.
     * @param nrecs A positive integer for the (maximum) number of recommendations.
     * @param normalize A Boolean: should the recommendations be normalized or not?
     * @param warn A Boolean: should warning messages be given or not?
     * @return A list of string-double pairs sorted in descending order of their values.
     * @see recommend
     */
    fun recommend(
        items: Array<String>,
        nrecs: Int = 12,
        normalize: Boolean = false,
        warn: Boolean = true
    ): List<Pair<String?, Double?>> {

        var arrOfVals: Array<Double> = Array(items.size) { 1.0 };

        val scoredItems: Map<String, Double> = items.zip(arrOfVals).toMap();

        return this.recommend(scoredItems, nrecs, normalize, warn)
    }

    /**
     * Compute recommendations by array items.
     * Makes a profile and delegates to recommendByProfile.
     *
     * @param items An string-double (hash-)map of scored items.
     * @param nrecs A positive integer for the (maximum) number of recommendations.
     * @param normalize A Boolean: should the recommendations be normalized or not?
     * @param warn A Boolean: should warning messages be given or not?
     * @return A list of string-double pairs sorted in descending order of their values.
     * @see recommendByProfile
     */
    fun recommend(
        items: Map<String, Double>,
        nrecs: Int = 12,
        normalize: Boolean = false,
        warn: Boolean = true
    ): List<Pair<String?, Double?>> {
        // It is not fast, but it is just easy to compute the profile and call recommendByProfile.
        val prof: Map<String?, Double?> = this.profile(items).toMap()
        val prof2: Map<String, Double> =
            prof.filterKeys { it != null }.filterValues { it != null } as Map<String, Double>;

        return this.recommendByProfile(prof2, nrecs, normalize, warn)
    }

    //========================================================
    // Recommend by profile
    //========================================================
    /**
     * Compute recommendations by array profile.
     * Makes a map from the array and delegates to the corresponding overloaded version.
     *
     * @param prof An string array of tags.
     * @param nrecs A positive integer for the (maximum) number of recommendations.
     * @param normalize A Boolean: should the recommendations be normalized or not?
     * @param warn A Boolean: should warning messages be given or not?
     * @return A list of string-double pairs sorted in descending order of their values.
     * @see recommendByProfile
     */
    fun recommendByProfile(
        prof: Array<String>,
        nrecs: Int = 12,
        normalize: Boolean = false,
        warn: Boolean = true
    ): List<Pair<String?, Double?>> {

        var arrOfVals: Array<Double> = Array(prof.size) { 1.0 };

        val scoredTags: Map<String, Double> = prof.zip(arrOfVals).toMap();

        return this.recommendByProfile(scoredTags, nrecs, normalize, warn)
    }

    /**
     * Compute recommendations by profile.
     *
     * @param prof A (hash-)map that is a profile. The keys are tags, the values are scores.
     * @param nrecs A positive integer for the (maximum) number of recommendations.
     * @param normalize A Boolean: should the recommendations be normalized or not?
     * @param warn A Boolean: should warning messages be given or not?
     * @return A list of string-double pairs sorted in descending order of their values.
     */
    fun recommendByProfile(
        prof: Map<String, Double>,
        nrecs: Int = 12,
        normalize: Boolean = false,
        warn: Boolean = true
    ): List<Pair<String?, Double?>> {

        // Make sure tags are known
        var profQuery = prof.filterKeys { this.knownTags.contains(it) }

        if (profQuery.isEmpty() && warn) {
            System.err.println("None of the tags is known in the recommender.")
            return listOf()
        }

        if (profQuery.size < prof.size && warn) {
            System.err.println("Some of the tags are unknown in the recommender.")
        }

        // Compute the profile

        // Get the tag inverse indexes and multiply their value by the corresponding item weight
        val weightedTagIndexes: List<Pair<String, Map<String?, Double?>?>> =
            profQuery.map { tagPair ->
                Pair(
                    tagPair.key,
                    this.tagInverseIndexes[tagPair.key]?.mapValues { it.value?.times(tagPair.value) })
            }

        // Reduce the maps of maps into one map by adding the weights that correspond to the same keys
        var profMix: Map<String, Double> = mapOf<String, Double>().mergeReduce( { a, b -> a + b}, weightedTagIndexes.map{it.second} as List<Map<String, Double>> )

        // Timing comparisons would be nice:
        //        var profMix: Map<String?, Double?> =
        //            weightedTagIndexes.fold(mapOf<String?, Double?>()) { acc, index -> myMerge(acc, index.second!!) }

        // Normalize
        //if ( normalize ) { itemMix = this.normalize( itemMix, "max-norm") }

        // Convert to list of pairs and reverse sort
        val res = profMix.toList().sortedByDescending { it.second }

        // Result
        return res.take(min(nrecs, res.size))
    }
}

//========================================================
// Inverse index merging functions
//========================================================
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


fun <K, V> Map<K, V>.mergeReduce(reduce: (V, V) -> V, others: List<Map<K, V>>): Map<K, V> =
    this.toMutableMap().apply { others.forEach { other -> other.forEach { merge(it.key, it.value, reduce) } } }

fun <K, V> MutableMap<K, V>.mergeReduceInPlace(reduce: (V, V) -> V, others: List<Map<K, V>>) =
    others.forEach { other -> other.forEach { merge(it.key, it.value, reduce) } }
