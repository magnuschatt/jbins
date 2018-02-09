package chatt.jbins

import chatt.jbins.JsonbFunctions.arrayElements
import chatt.jbins.JsonbFunctions.jsonbExtractPath

object JbinFilterTranslator {

    private val ARRAY_MARKER = "[]"

    fun translate(filter: JbinFilter): Pair<String, List<Any>> {
        return when (filter) {
            is JbinFilter.Or -> translateOr(filter)
            is JbinFilter.And -> translateAnd(filter)
            is JbinFilter.Equals -> translateEquals(filter.path, filter.value)
            is JbinFilter.NotEquals -> translateEquals(filter.path, filter.value, negate = true)
        }
    }

    private fun translateList(filters: Collection<JbinFilter>, separator: String): Pair<String, List<Any>> {
        val empty = "true" to emptyList<Any>()
        val pairs = filters.map { translate(it) }.filterNot { it == empty }
        if (pairs.isEmpty()) return empty
        if (pairs.size == 1) return pairs.first()

        val sql = pairs.joinToString(separator = separator, prefix = "(", postfix = ")", transform = { it.first })
        val params = pairs.flatMap { it.second }
        return sql to params
    }

    private fun translateOr(filter: JbinFilter.Or): Pair<String, List<Any>> {
        return translateList(filter, " OR ")
    }

    private fun translateAnd(filter: JbinFilter.And): Pair<String, List<Any>> {
        return translateList(filter, " AND ")
    }

    private fun translateEquals(path: String, value: String, negate: Boolean = false): Pair<String, List<Any>> {
        val elements = path.splitToElements()
        var sql = "body"
        elements.forEachIndexed { index, elem ->
            val lastOne = (index == elements.size - 1)
            val isArray = elem.isArray
            sql = jsonbExtractPath(sql, listOf(elem.name), asText = (lastOne && !isArray))
            if (isArray) sql = arrayElements(sql, lastOne)
        }

        val comparator = if (negate) "!=" else "="
        sql = if (elements.none { it.isArray }) "$sql $comparator ?"
        else "? $comparator ANY(ARRAY(SELECT $sql))"

        return sql to listOf(value)
    }

    private data class PathElement(val name: String, val isArray: Boolean)

    private fun String.splitToElements() = split('.').map {
        if (it.endsWith(ARRAY_MARKER)) {
            PathElement(it.removeSuffix(ARRAY_MARKER), true)
        } else {
            PathElement(it, false)
        }
    }

}