package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import ru.it_arch.kddd.domain.fullClassName
import ru.it_arch.kddd.domain.model.ILogger
import ru.it_arch.kddd.domain.model.Options
import ru.it_arch.kddd.presentation.Visitor
import ru.it_arch.kddd.presentation.collectAllKdddModels
import ru.it_arch.kddd.utils.Utils

internal class KdddProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Options,
    private val utils: Utils,
    private val logger: ILogger,
    private val isTesting: Boolean
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.log("options: $options, isTesting: $isTesting")
        Visitor(resolver, options, utils, logger).also { visitor ->
            resolver.collectAllKdddModels(visitor)
                .onEach { logger.log(it.first.kddd.fullClassName.boxed) }





            visitor.typeCatalog.entries.forEach { entry ->
                logger.log("${entry.key} -> ${entry.value}")
            }
        }

        return emptyList()
    }

    /** Перехват выходного потока с построчной буферизацией. Нужно для подмены строк на выходе. Грязный хак.
    private fun FileSpec.replaceAndWrite(codeGenerator: CodeGenerator, dependencies: Dependencies) {
        codeGenerator.createNewFile(dependencies, packageName, name).also { StringBufferWriter(it).use(::writeTo) }
    }*/

}
