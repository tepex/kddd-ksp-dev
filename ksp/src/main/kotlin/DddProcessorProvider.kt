package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

// ru.it_arch.clean_ddd.ksp.DddProcessorProvider

public class DddProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        DddProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
}
