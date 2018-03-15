package chatt.jbins.utils

import chatt.jbins.JbinDocument
import chatt.jbins.JbinDocument.Companion.ID_PATH
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

private val mapper = ObjectMapper()
private val mapTypeRef = object : TypeReference<Map<String, Any>>() {}

fun String.toMap(): Map<String, Any> {
    return mapper.readValue(this, mapTypeRef)
}

fun Map<String, Any>.toJson(): String {
    return mapper.writeValueAsString(this)
}

fun Map<String, Any>.toDocument(): JbinDocument {
    val map = this.toMutableMap()
    val id = map.remove(ID_PATH) as? String ?: uuid()
    return JbinDocument(id, map.toJson())
}

fun document(vararg entries: Pair<String, Any>): JbinDocument {
    return mapOf(*entries).toDocument()
}