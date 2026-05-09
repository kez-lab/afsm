package afsm.graph.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

public class AfsmGraphProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return AfsmGraphProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}

