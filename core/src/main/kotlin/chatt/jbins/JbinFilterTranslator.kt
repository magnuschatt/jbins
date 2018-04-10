package chatt.jbins

import chatt.jbins.JbinDocument.Companion.ID_PATH
import chatt.jbins.JbinFilter.Comparator.*
import chatt.jbins.utils.*

object JbinFilterTranslator {

    private val whereTrue = Translation("true", emptyList(), emptySet())
    private val whereFalse = Translation("false", emptyList(), emptySet())
    private enum class Separator { AND, OR }

    data class Translation(val sql: String,
                           val params: List<Any>,
                           val functions: Set<PostgresFunction>)

    fun translate(filter: JbinFilter): Translation = when (filter) {
        is JbinFilter.True -> whereTrue
        is JbinFilter.False -> whereFalse
        is JbinFilter.Or -> translateOr(filter)
        is JbinFilter.And -> translateAnd(filter)
        is JbinFilter.Match -> translateMatch(filter)
        is JbinFilter.IsEmpty -> translateMissing(filter)
    }

    private fun translateList(filters: Collection<JbinFilter>, separator: Separator): Translation {

        val redundant = when (separator) {
            Separator.OR -> whereTrue
            Separator.AND -> whereFalse
        }

        val children = filters.map { translate(it) }.filterNot { it == redundant }
        if (children.isEmpty()) return redundant
        if (children.size == 1) return children.first()

        val sql = children.joinToString(
                separator = " ${separator.name} ",
                prefix = "(",
                postfix = ")",
                transform = { it.sql }
        )

        val params = children.flatMap { it.params }
        val functions = children.flatMap { it.functions }.toSet()
        return Translation(sql, params, functions)
    }

    private fun translateOr(filter: JbinFilter.Or): Translation {

        // check if we can join the ORs using the IN operator
        val ins: Map<String, List<Any>> = filter
                .mapNotNull { it as? JbinFilter.Match }
                .filter { it.comparator == EQ }
                .filter { splitToElements(it.path).none { it.isArray } }
                .groupBy { it.path }
                .filter { it.value.size > 1 }
                .mapValues { it.value.map { it.value } }

        if (ins.size == 1 && ins.values.first().size == filter.size) {
            val path: String = ins.keys.first()
            val values: List<Any> = ins.values.first()

            return if (path == ID_PATH) {
                Translation("id IN (?)", listOf(values), emptySet())
            } else {
                val func = getPostgresFunction(path)
                Translation("${func.name}(body) IN (?)", listOf(values), setOf(func))
            }
        }

        return translateList(filter, Separator.OR)
    }

    private fun translateAnd(filter: JbinFilter.And): Translation {
        return translateList(filter, Separator.AND)
    }

    private fun translateMatch(filter: JbinFilter.Match): Translation {
        val value = filter.value.toString()

        // signs has to be flipped so they can work with the 'ANY' function in postgresql
        val comparator = when (filter.comparator) {
            LIKE -> LIKE_OPERATOR
            ILIKE -> ILIKE_OPERATOR
            EQ -> "="
            NEQ -> "!="
            GT -> "<"
            GTE -> "<="
            LT -> ">"
            LTE -> ">="
        }

        if (filter.path == ID_PATH) {
            return Translation("? $comparator id", listOf(value), emptySet())
        }

        val func = getPostgresFunction(filter.path)
        val elements = splitToElements(filter.path)

        val sql = if (elements.any { it.isArray }) {
            if (!filter.matchAll && filter.comparator == EQ) {
                "CAST(ARRAY[?] AS text[]) <@ ${func.name}(body)"
            } else {
                val arrayMatching = if (filter.matchAll) "ALL" else "ANY"
                "? $comparator $arrayMatching(${func.name}(body))"
            }
        } else {
            "? $comparator ${func.name}(body)"
        }

        return Translation(sql, listOf(value), setOf(func))
    }

    private fun translateMissing(filter: JbinFilter.IsEmpty): Translation {
        val func = getPostgresFunction(filter.path)
        val elements = splitToElements(filter.path)

        val sql = if (elements.any { it.isArray }) {
            val operator = if (filter.isEmpty) "=" else "<>"
            "CAST(ARRAY[] AS text[]) $operator ${func.name}(body)"
        } else {
            val operator = if (filter.isEmpty) "IS NULL" else "IS NOT NULL"
            "${func.name}(body) $operator"
        }

        return Translation(sql, emptyList(), setOf(func))
    }

}