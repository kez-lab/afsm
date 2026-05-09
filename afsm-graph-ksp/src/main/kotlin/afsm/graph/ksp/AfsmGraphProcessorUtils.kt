package afsm.graph.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration

internal fun KSClassDeclaration.hasSuperType(qualifiedName: String): Boolean {
    return getAllSuperTypes().any { type ->
        type.declaration.qualifiedName?.asString() == qualifiedName
    }
}

internal fun KSClassDeclaration.hasRequiredConstructorParameters(): Boolean {
    return primaryConstructor?.parameters.orEmpty().any { parameter ->
        !parameter.hasDefault && !parameter.isVararg
    }
}

internal fun KSAnnotation?.stringArgument(name: String): String {
    val value = this?.arguments
        ?.firstOrNull { argument -> argument.name?.asString() == name }
        ?.value

    return value as? String ?: ""
}

internal fun String.kotlinLiteral(): String {
    return buildString {
        append('"')
        this@kotlinLiteral.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }
}
