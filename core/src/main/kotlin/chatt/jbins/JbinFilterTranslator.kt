package chatt.jbins

import chatt.jbins.JbinDocument.Companion.ID_PATH
import chatt.jbins.JbinFilter.Comparator.*
import chatt.jbins.utils.*

object JbinFilterTranslator {

    private val empty = Translation("true", emptyList(), emptyList())

    data class Translation(val sql: String,
                           val params: List<String>,
                           val functions: List<PostgresFunction>)

    fun translate(filter: JbinFilter?): Translation {
        return when (filter) {
            null -> empty
            is JbinFilter.Or -> translateOr(filter)
            is JbinFilter.And -> translateAnd(filter)
            is JbinFilter.Match -> translateMatch(filter)
            is JbinFilter.IsEmpty -> translateMissing(filter)
        }
    }

    private fun translateList(filters: Collection<JbinFilter>, separator: String): Translation {
        val children = filters.map { translate(it) }.filterNot { it == empty }
        if (children.isEmpty()) return empty
        if (children.size == 1) return children.first()

        val sql = children.joinToString(separator = separator, prefix = "(", postfix = ")", transform = { it.sql })
        val params = children.flatMap { it.params }
        val functions = children.flatMap { it.functions }
        return Translation(sql, params, functions)
    }

    private fun translateOr(filter: JbinFilter.Or): Translation {
        return translateList(filter, " OR ")
    }

    private fun translateAnd(filter: JbinFilter.And): Translation {
        return translateList(filter, " AND ")
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
            return Translation("? $comparator id", listOf(value), emptyList())
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

        return Translation(sql, listOf(value), listOf(func))
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

        return Translation(sql, emptyList(), listOf(func))
    }

}