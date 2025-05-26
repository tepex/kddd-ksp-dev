package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import ru.it_arch.clean_ddd.ksp.model.LoggerImpl
import ru.it_arch.kddd.domain.toOptions

public class KdddProcessorProvider(private val isTesting: Boolean = false) : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = KdddProcessor(
        environment.codeGenerator,
        environment.options.toOptions(),
        utils,
        LoggerImpl(environment.logger),
        isTesting
    )
}
