package nl.avisi.kotlinwebtest.rest

import nl.avisi.kotlinwebtest.http.HttpResponse

fun request() =
        RestRequest()

fun response(json: String) =
        RestStepResponse(HttpResponse(200, json.toByteArray(), emptyList()), true)

