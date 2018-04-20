package nl.avisi.kotlinwebtest.rest

import nl.avisi.kotlinwebtest.http.HttpResponse

fun request() =
        RestStepRequest("", null)

fun response(json: String) =
        RestStepResponse(HttpResponse(200, json.toByteArray(), emptyList()), true)

fun responseNotFound(json: String) =
        RestStepResponse(HttpResponse(404, json.toByteArray(), emptyList()), true)

fun responseNoResponse() =
        RestStepResponse(null, true)

