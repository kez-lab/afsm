package io.github.afsm.ide

internal data class AfsmGraphSource(
    val id: String,
    val fileName: String,
    val declarationName: String,
)

/**
 * Reads only the small source contract needed to locate an already generated
 * graph. It deliberately does not interpret the Afsm DSL or execute consumer
 * code inside the IDE process.
 */
internal object AfsmGraphSourceParser {
    private const val annotationName = "AfsmGraph"
    private val declaration = Regex(
        """\b(class|object|val|var|interface|fun)\s+(`[^`]+`|[A-Za-z_][A-Za-z0-9_]*)""",
    )

    fun parseAt(source: String, annotationNameOffset: Int): AfsmGraphSource? {
        if (annotationNameOffset !in source.indices) return null
        if (!source.regionMatches(annotationNameOffset, annotationName, 0, annotationName.length)) {
            return null
        }

        val annotationStart = findAnnotationStart(source, annotationNameOffset) ?: return null
        if (!isCodePosition(source, annotationStart)) return null

        val nameEnd = annotationNameOffset + annotationName.length
        if (nameEnd < source.length && source[nameEnd].isIdentifierPart()) return null

        val annotationEnd = findAnnotationEnd(source, nameEnd) ?: return null
        val declarationMatch = declaration.find(source, annotationEnd) ?: return null
        if (declarationMatch.range.first - annotationEnd > 2_000) return null

        val kind = declarationMatch.groupValues[1]
        if (kind != "class" && kind != "object" && kind != "val") return null

        val declarationName = declarationMatch.groupValues[2].removeSurrounding("`")
        val annotationText = source.substring(annotationStart, annotationEnd)
        val explicitId = annotationText.stringArgument("id")
        val explicitFileName = annotationText.stringArgument("fileName")
        val id = explicitId?.takeUnless(String::isBlank) ?: declarationName
        val fileName = explicitFileName?.takeUnless(String::isBlank) ?: "$id.mmd"

        return AfsmGraphSource(
            id = id,
            fileName = fileName,
            declarationName = declarationName,
        )
    }

    private fun findAnnotationStart(source: String, annotationNameOffset: Int): Int? {
        var cursor = annotationNameOffset - 1
        while (cursor >= 0 && (source[cursor].isIdentifierPart() || source[cursor] == '.')) {
            cursor--
        }
        return cursor.takeIf { it >= 0 && source[it] == '@' }
    }

    private fun findAnnotationEnd(source: String, nameEnd: Int): Int? {
        var cursor = nameEnd
        while (cursor < source.length && source[cursor].isWhitespace()) cursor++
        if (cursor >= source.length || source[cursor] != '(') return nameEnd

        var depth = 0
        var quote: Char? = null
        var escaped = false
        while (cursor < source.length) {
            val char = source[cursor]
            if (quote != null) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == quote) {
                    quote = null
                }
            } else {
                when (char) {
                    '\'', '"' -> quote = char
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) return cursor + 1
                    }
                }
            }
            cursor++
        }
        return null
    }

    private fun String.stringArgument(name: String): String? {
        val match = Regex(
            """\b${Regex.escape(name)}\s*=\s*"((?:\\.|[^"\\])*)"""",
        ).find(this) ?: return null
        return match.groupValues[1].unescapeKotlinString()
    }

    private fun String.unescapeKotlinString(): String = buildString(length) {
        var cursor = 0
        while (cursor < this@unescapeKotlinString.length) {
            val char = this@unescapeKotlinString[cursor]
            if (char != '\\' || cursor == this@unescapeKotlinString.lastIndex) {
                append(char)
                cursor++
                continue
            }

            val escaped = this@unescapeKotlinString[cursor + 1]
            append(
                when (escaped) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    'b' -> '\b'
                    else -> escaped
                },
            )
            cursor += 2
        }
    }

    /** True when [offset] is not inside a comment or a string/character literal. */
    private fun isCodePosition(source: String, offset: Int): Boolean {
        var cursor = 0
        var state = LexicalState.CODE
        while (cursor < offset) {
            val char = source[cursor]
            val next = source.getOrNull(cursor + 1)
            when (state) {
                LexicalState.CODE -> when {
                    char == '/' && next == '/' -> {
                        state = LexicalState.LINE_COMMENT
                        cursor++
                    }
                    char == '/' && next == '*' -> {
                        state = LexicalState.BLOCK_COMMENT
                        cursor++
                    }
                    char == '"' -> state = LexicalState.STRING
                    char == '\'' -> state = LexicalState.CHAR
                }
                LexicalState.LINE_COMMENT -> if (char == '\n') state = LexicalState.CODE
                LexicalState.BLOCK_COMMENT -> if (char == '*' && next == '/') {
                    state = LexicalState.CODE
                    cursor++
                }
                LexicalState.STRING -> when {
                    char == '\\' -> cursor++
                    char == '"' -> state = LexicalState.CODE
                }
                LexicalState.CHAR -> when {
                    char == '\\' -> cursor++
                    char == '\'' -> state = LexicalState.CODE
                }
            }
            cursor++
        }
        return state == LexicalState.CODE
    }

    private fun Char.isIdentifierPart(): Boolean = isLetterOrDigit() || this == '_'

    private enum class LexicalState {
        CODE,
        LINE_COMMENT,
        BLOCK_COMMENT,
        STRING,
        CHAR,
    }
}
