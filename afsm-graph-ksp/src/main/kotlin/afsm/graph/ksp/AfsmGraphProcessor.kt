package afsm.graph.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate

internal class AfsmGraphProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) {
            return emptyList()
        }

        val symbols = resolver.getSymbolsWithAnnotation(AFSM_GRAPH_ANNOTATION).toList()
        if (symbols.isEmpty()) {
            return emptyList()
        }

        val invalidSymbols = symbols.filterNot { it.validate() }
        val declarations = symbols
            .filter { it.validate() }
            .mapNotNull { symbol ->
                val declaration = symbol as? KSClassDeclaration
                if (declaration == null) {
                    logger.error("@AfsmGraph can only be used on StateMachine classes.", symbol)
                }
                declaration
            }

        val entries = declarations.mapNotNull(::validateAndCreateEntry)
            .sortedBy { it.id }

        validateDuplicates(entries)

        if (entries.isNotEmpty()) {
            generateRegistry(entries)
            generated = true
        }

        return invalidSymbols
    }

    private fun validateAndCreateEntry(
        declaration: KSClassDeclaration,
    ): GraphEntry? {
        val qualifiedName = declaration.qualifiedName?.asString()
        if (qualifiedName == null) {
            logger.error("@AfsmGraph class must have a qualified name.", declaration)
            return null
        }

        if (Modifier.PRIVATE in declaration.modifiers) {
            logger.error("@AfsmGraph class must not be private.", declaration)
            return null
        }

        if (declaration.classKind != ClassKind.CLASS && declaration.classKind != ClassKind.OBJECT) {
            logger.error("@AfsmGraph can only be used on classes or objects.", declaration)
            return null
        }

        if (!declaration.hasSuperType(AFSM_REDUCER)) {
            logger.error("@AfsmGraph class must implement AfsmReducer.", declaration)
            return null
        }

        if (!declaration.hasSuperType(AFSM_GRAPH_SOURCE)) {
            logger.error("@AfsmGraph class must implement AfsmGraphSource.", declaration)
            return null
        }

        if (declaration.classKind == ClassKind.CLASS && declaration.hasRequiredConstructorParameters()) {
            logger.error(
                "@AfsmGraph class must be constructible with no required parameters or be an object.",
                declaration,
            )
            return null
        }

        val annotation = declaration.annotations.firstOrNull { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString() == AFSM_GRAPH_ANNOTATION
        }
        val id = annotation.stringArgument("id")
            .ifBlank { declaration.simpleName.asString() }
        val fileName = annotation.stringArgument("fileName")
            .ifBlank { "$id.mmd" }

        if (!fileName.endsWith(".mmd")) {
            logger.error("Afsm graph fileName must end with .mmd.", declaration)
            return null
        }

        return GraphEntry(
            id = id,
            fileName = fileName,
            sourceExpression = if (declaration.classKind == ClassKind.OBJECT) {
                qualifiedName
            } else {
                "$qualifiedName()"
            },
            sourceFile = declaration.containingFile,
        )
    }

    private fun validateDuplicates(entries: List<GraphEntry>) {
        entries.groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { id ->
                logger.error("Duplicate Afsm graph id: $id.")
            }

        entries.groupBy { it.fileName }
            .filterValues { it.size > 1 }
            .keys
            .forEach { fileName ->
                logger.error("Duplicate Afsm graph fileName: $fileName.")
            }
    }

    private fun generateRegistry(entries: List<GraphEntry>) {
        val dependencies = Dependencies(
            aggregating = true,
            sources = entries.mapNotNull { it.sourceFile }.toTypedArray(),
        )
        val file = codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = GENERATED_PACKAGE,
            fileName = GENERATED_REGISTRY_NAME,
        )

        file.bufferedWriter().use { writer ->
            writer.appendLine("package $GENERATED_PACKAGE")
            writer.appendLine()
            writer.appendLine("internal object $GENERATED_REGISTRY_NAME : afsm.core.AfsmGraphRegistry {")
            writer.appendLine("    override val entries: kotlin.collections.List<afsm.core.AfsmGraphEntry> =")
            writer.appendLine("        kotlin.collections.listOf(")
            entries.forEach { entry ->
                writer.appendLine("            afsm.core.AfsmGraphEntry(")
                writer.appendLine("                id = ${entry.id.kotlinLiteral()},")
                writer.appendLine("                fileName = ${entry.fileName.kotlinLiteral()},")
                writer.appendLine("                createTopology = { ${entry.sourceExpression}.topology },")
                writer.appendLine("            ),")
            }
            writer.appendLine("        )")
            writer.appendLine("}")
        }
    }

    private data class GraphEntry(
        val id: String,
        val fileName: String,
        val sourceExpression: String,
        val sourceFile: KSFile?,
    )

    private companion object {
        const val AFSM_GRAPH_ANNOTATION = "afsm.core.AfsmGraph"
        const val AFSM_GRAPH_SOURCE = "afsm.core.AfsmGraphSource"
        const val AFSM_REDUCER = "afsm.core.AfsmReducer"
        const val GENERATED_PACKAGE = "afsm.generated"
        const val GENERATED_REGISTRY_NAME = "AfsmGeneratedGraphRegistry"
    }
}
