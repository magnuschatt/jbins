package chatt.jbins

object JbinFilterTranslator {

    fun translate(filter: JbinFilter): Pair<String, List<Any>> {
        return when (filter) {
            is JbinFilter.Equals -> translateEquals(filter)
            is JbinFilter.Or -> translateOr(filter)
            is JbinFilter.And -> translateAnd(filter)
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

    private fun translateEquals(filter: JbinFilter.Equals): Pair<String, List<Any>> {
        val pathElements = filter.path.split('.').joinToString(separator = ", ", transform = { "'$it'" })
        return "jsonb_extract_path_text(body, $pathElements) = ?" to listOf(filter.value)
    }

}