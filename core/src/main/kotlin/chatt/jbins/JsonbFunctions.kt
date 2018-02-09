package chatt.jbins

object JsonbFunctions {

    fun jsonbExtractPath(json: String, pathElements: Collection<String>, asText: Boolean = false): String {
        val params = pathElements.toParams()
        val text = if (asText) "_text" else ""
        return "jsonb_extract_path$text($json, $params)"
    }

    fun arrayElements(json: String, asText: Boolean = false): String {
        val text = if (asText) "_text" else ""
        return "jsonb_array_elements$text($json)"
    }

    private fun Collection<String>.toParams(): String {
        return joinToString(separator = ", ") { "'$it'" }
    }

}
