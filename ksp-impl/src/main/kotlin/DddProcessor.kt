package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.clean_ddd.ksp.model.KDOptions
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.clean_ddd.ksp.model.KDTypeBuilderBuilder
import ru.it_arch.clean_ddd.ksp.model.KDTypeJsonBuilder

internal class DddProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KDOptions,
    private val isTesting: Boolean
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        //val symbols = resolver.getAllFiles()
        val symbols = resolver.getNewFiles()
        if (!symbols.iterator().hasNext()) return emptyList()

        logger.warn("options: $options, isTesting: $isTesting")
        logger.warn("symbols: ${symbols.toList()}")

        DomainLayerVisitor(resolver).generate(symbols)

        /*
        resolver.getDeclarationsFromPackage("ru.it_arch.ddd")
            .filterIsInstance<KSClassDeclaration>()
            .find { it.simpleName.asString() == "TestStruct" }
            ?.also { processDeclaration(null, it) }

         */

        return symbols.filterNot { it.validate() }.toList()
    }

    private inner class DomainLayerVisitor(
        resolver: Resolver
    ) : KDVisitor(resolver, options, codeGenerator, logger) {

        override fun createBuilder(model: KDType.Model) {
            with(options) {
                KDTypeBuilderBuilder.create(model, false, kdLogger).also { builderBuilder ->
                    model.builder.addType(builderBuilder.build())
                    builderBuilder.buildFunToBuilder().also(model.builder::addFunction)
                }
                if (model.hasDsl) KDTypeBuilderBuilder.create(model, true, kdLogger).also { builderBuilder ->
                    model.builder.addType(builderBuilder.build())
                    builderBuilder.buildFunToBuilder().also(model.builder::addFunction)
                }
                if (model.hasJson) KDTypeJsonBuilder.create(model, kdLogger).also { model.builder.addType(it.build()) }
            }
        }
    }
}
