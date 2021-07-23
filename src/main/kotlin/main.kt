import streamsBlendingRecommender.CoreSBR
import java.io.File


fun main(args: Array<String>) {

    val fileName: String =
        "/Volumes/Macintosh HD 1/Users/antonov/RecommenderProjects/Kotlin/StreamsBlendingRecommender/src/main/resources/RandomGoods-dfSMRMatrix.csv"
    val smrDataFile = File(fileName)

    val sbr = CoreSBR()

    sbr.ingestSMRMatrixCSVFile(fileName, "Item", "TagType", "Value", "Weight", true)

    println("-----")
    println(sbr.profile(arrayOf("diametrical-1")))
    //println(sbr.profile(mapOf( "clemens-7-ml" to 3.0, "kalki-4-f" to 2.0 )))

    println("Recommendation by history: ")
    println(sbr.recommend(arrayOf("diametrical-1"), 10))

    println("Recommendation by profile: ")
    println(sbr.recommendByProfile(arrayOf("Good:milk", "Country:denmark"), 20))

    //println(sbr.profile(arrayOf("kalki-4-f")))
}