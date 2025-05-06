package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import ru.it_arch.clean_ddd.domain.toOptions

public class DddProcessorProvider(private val isTesting: Boolean = false) : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = DddProcessor(
        environment.codeGenerator,
        environment.options.toOptions(),
        LoggerImpl(environment.logger),
        isTesting
    )
}
