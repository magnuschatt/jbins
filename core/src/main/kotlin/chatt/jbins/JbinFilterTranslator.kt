package chatt.jbins

import chatt.jbins.JbinFilter.Comparator.*
import chatt.jbins.JsonbFunctions.arrayElements
import chatt.jbins.JsonbFunctions.jsonbExtractPath

object JbinFilterTranslator {

    private const val ARRAY_MARKER = "[]"

    fun translate(filter: JbinFilter): Pair<String, List<Any>> {
        return when (filter) {
            is JbinFilter.Or -> translateOr(filter)
            is JbinFilter.And -> translateAnd(filter)
            is JbinFilter.Match -> translateMatch(filter)
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

    private fun translateMatch(filter: JbinFilter.Match): Pair<String, List<Any>> {
        val elements = filter.path.splitToElements()
        var sql = "body"
        elements.forEachIndexed { index, elem ->
            val lastOne = (index == elements.size - 1)
            val isArray = elem.isArray
            sql = jsonbExtractPath(sql, listOf(elem.name), asText = (lastOne && !isArray))
            if (isArray) sql = arrayElements(sql, lastOne)
        }

        // signs has to be flipped so they can work with the 'ANY' function in postgresql
        val comparator = when (filter.comparator) {
            EQ -> "="
            NEQ -> "!="
            GT -> "<"
            GTE -> "<="
            LT -> ">"
            LTE -> ">="
        }

        val arrayMatching = if (filter.matchAll) "ALL" else "ANY"
        sql = if (elements.none { it.isArray }) "? $comparator $sql"
        else "? $comparator $arrayMatching(ARRAY(SELECT $sql))"

        return sql to listOf(filter.value.toString())
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