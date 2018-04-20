package nl.avisi.kotlinwebtest.jdbc

import java.util.HashMap

//fun actual(): ArrayList<HashMap<String, String>> {
//    val results: ArrayList<HashMap<String, String>> = arrayListOf()
//    val row_1 = hashMapOf<String, String>()
//    row_1["name"] = "Audi"
//    row_1["price"] = "52642"
//    results[0] = row_1
//
//    val row_2 = hashMapOf<String, String>()
//    row_2["name"] = "Audi"
//    row_2["price"] = "52642"
//    results[1] = row_1
//
//    val row_3 = hashMapOf<String, String>()
//    row_3["name"] = "Audi"
//    row_3["price"] = "52642"
//    results[2] = row_1
//
//    val row_4 = hashMapOf<String, String>()
//    row_4["name"] = "Audi"
//    row_4["price"] = "52642"
//    results[3] = row_1
//
//    val row_5 = hashMapOf<String, String>()
//    row_5["name"] = "Audi"
//    row_5["price"] = "52642"
//    results[4] = row_1
//    return results
//}

fun request() =
        JdbcStepRequest()

fun response(results: ArrayList<HashMap<String, String>>) =
        JdbcStepResponse(results, true)



