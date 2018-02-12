package chatt.jbins

import chatt.jbins.JbinFilter.Comparator.*

object JbinFilterTranslator {

    fun translate(filter: JbinFilter): Result {
        return when (filter) {
            is JbinFilter.Or -> translateOr(filter)
            is JbinFilter.And -> translateAnd(filter)
            is JbinFilter.Match -> translateMatch(filter)
        }
    }

    private fun translateList(filters: Collection<JbinFilter>, separator: String): Result {
        val empty = Result("true", emptyList(), emptyList())
        val children = filters.map { translate(it) }.filterNot { it == empty }
        if (children.isEmpty()) return empty
        if (children.size == 1) return children.first()

        val sql = children.joinToString(separator = separator, prefix = "(", postfix = ")", transform = { it.sql })
        val params = children.flatMap { it.params }
        val functions = children.flatMap { it.functions }
        return Result(sql, params, functions)
    }

    private fun translateOr(filter: JbinFilter.Or): Result {
        return translateList(filter, " OR ")
    }

    private fun translateAnd(filter: JbinFilter.And): Result {
        return translateList(filter, " AND ")
    }

    private fun translateMatch(filter: JbinFilter.Match): Result {
        val funcName = getFunctionName(filter.path)
        val elements = splitToElements(filter.path)

        // signs has to be flipped so they can work with the 'ANY' function in postgresql
        val comparator = when (filter.comparator) {
            EQ -> "="
            NEQ -> "!="
            GT -> "<"
            GTE -> "<="
            LT -> ">"
            LTE -> ">="
        }

        val sql = if (elements.any { it.isArray }) {
            if (!filter.matchAll && filter.comparator == EQ) {
                "CAST(ARRAY[?] AS text[]) <@ $funcName(body)"
            } else {
                val arrayMatching = if (filter.matchAll) "ALL" else "ANY"
                "? $comparator $arrayMatching($funcName(body))"
            }
        } else {
            "? $comparator $funcName(body)"
        }

        val function = getPostgresFunction(filter.path)
        return Result(sql, listOf(filter.value.toString()), listOf(function))
    }

    data class Result(val sql: String,
                      val params: List<String>,
                      val functions: List<PostgresFunction>)

}