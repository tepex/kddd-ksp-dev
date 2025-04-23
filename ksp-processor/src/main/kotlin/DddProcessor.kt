package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate
import ru.it_arch.clean_ddd.ksp_model.model.KDOptions
import ru.it_arch.clean_ddd.ksp_model.model.KDType
import ru.it_arch.clean_ddd.ksp_model.BuilderHolder
import ru.it_arch.clean_ddd.ksp_model.JsonBuilderHolder

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

        // return canonical BuilderHolder
        override fun createBuilders(model: KDType.Model) {
            with(options) {
                with(KDLoggerImpl(logger)) {

                    // class MyTypeImpl.Builder
                    BuilderHolder(model, false).also { holder ->
                        model.builder.addType(holder.build())
                        // TODO: не прилеплять внутрь класса, а к расширению Kddd-типа!
                        //holder.buildToBuilderFun().also(model.builder::addFunction)
                    }
                    // class MyTypeImpl.DslBuilder
                    if (model.hasDsl) BuilderHolder(model, true).also { holder ->
                        model.builder.addType(holder.build())
                        // TODO: не прилеплять внутрь класса, а к расширению Kddd-типа!
                        //holder.buildToBuilderFun().also(model.builder::addFunction)
                    }
                    // MyTypeImpl.Companion
                    if (model.hasJson) JsonBuilderHolder(model).also { model.builder.addType(it.build()) }
                }
            }
        }
    }
}
