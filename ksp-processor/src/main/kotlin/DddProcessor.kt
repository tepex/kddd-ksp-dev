package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate
import ru.it_arch.clean_ddd.ksp_model.model.KDOptions

internal class DddProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KDOptions,
    private val isTesting: Boolean
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getNewFiles()
        if (!symbols.iterator().hasNext()) return emptyList()

        logger.warn("options: $options, isTesting: $isTesting")
        logger.warn("symbols: ${symbols.toList()}")

        with(options) {
            with(logger) {
                KDVisitor(resolver, codeGenerator).generate(symbols)
            }
        }

        return symbols.filterNot { it.validate() }.toList()
    }
}
