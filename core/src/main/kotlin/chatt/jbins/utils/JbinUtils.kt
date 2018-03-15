package chatt.jbins.utils

import chatt.jbins.JsonbFunctions

const val ARRAY_MARKER = "[]"

data class PathElement(val name: String,
                       val isArray: Boolean)

data class PostgresFunction(val name: String,
                            val sql: String)

fun splitToElements(path: String) = path.split('.').map {
    if (it.endsWith(ARRAY_MARKER)) {
        PathElement(it.removeSuffix(ARRAY_MARKER), true)
    } else {
        PathElement(it, false)
    }
}

fun getFunctionName(path: String): String {
    return "jbins_func_" + path.replace('.','_').replace("[]", "$").toLowerCase()
}

fun getPostgresFunction(path: String): PostgresFunction {
    val funcName = getFunctionName(path)
    val elements = splitToElements(path)

    var funcBody = "body"
    elements.forEachIndexed { index, elem ->
        val lastOne = (index == elements.size - 1)
        val isArray = elem.isArray
        funcBody = JsonbFunctions.jsonbExtractPath(funcBody, listOf(elem.name), asText = (lastOne && !isArray))
        if (isArray) funcBody = JsonbFunctions.arrayElements(funcBody, lastOne)
    }

    val funcSql = if (elements.none { it.isArray }) {
        "CREATE FUNCTION $funcName(body jsonb) RETURNS text AS " +
                "$$ SELECT $funcBody $$ LANGUAGE SQL IMMUTABLE;"
    } else {
        "CREATE FUNCTION $funcName(body jsonb) RETURNS text[] AS " +
                "$$ SELECT ARRAY(SELECT $funcBody) $$ LANGUAGE SQL IMMUTABLE;"
    }
    return PostgresFunction(funcName, funcSql)
}