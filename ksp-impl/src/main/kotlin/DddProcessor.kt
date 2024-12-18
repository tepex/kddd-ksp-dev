package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import ru.it_arch.clean_ddd.ksp.interop.FILE_HEADER_STUB
import ru.it_arch.clean_ddd.ksp.interop.KDContext
import ru.it_arch.clean_ddd.ksp.interop.KDOptions
import ru.it_arch.clean_ddd.ksp.interop.KDVisitor
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.clean_ddd.ksp.model.KDTypeBuilderBuilder

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

        val classNameGenerator: KSClassDeclaration.() -> ClassName = {
            options.generateClassName(simpleName.asString()).let(ClassName::bestGuess)
        }

        val globalKDTypes = mutableMapOf<TypeName, KDType>()
        symbols.forEach { file ->
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.INTERFACE && !it.simpleName.asString().startsWith('_') }
                .forEach { declaration ->
                    DomainLayerVisitor(resolver, globalKDTypes, classNameGenerator).visitKDDeclaration(declaration)?.let { kdType ->
                        KDContext.create(declaration, options.getPackage(declaration), kdType, classNameGenerator).also { it.generate(file) }
                    }
                }
        }

        /*
        resolver.getDeclarationsFromPackage("ru.it_arch.ddd")
            .filterIsInstance<KSClassDeclaration>()
            .find { it.simpleName.asString() == "TestStruct" }
            ?.also { processDeclaration(null, it) }

         */

        return symbols.filterNot { it.validate() }.toList()
    }

    private fun KDContext.generate(file: KSFile?) {
        FileSpec.builder(packageName.boxed, toBeGenerated.simpleName).also { fileBuilder ->
            fileBuilder.addFileComment(FILE_HEADER_STUB)

            (kdType as? KDType.Generatable)?.builder?.build()?.also(fileBuilder::addType)

            /* Root DSL builder */
            ParameterSpec.builder(
                "block",
                LambdaTypeName.get(receiver = receiver, returnType = Unit::class.asTypeName())
            ).build().also { builderParam ->
                FunSpec.builder(builderFunName.boxed)
                    .addParameter(builderParam)
                    .addStatement(
                        "return %T().apply(%N).${KDType.Data.BUILDER_BUILD_METHOD_NAME}()",
                        receiver,
                        builderParam
                    )
                    .returns(toBeGenerated)
                    .build().also(fileBuilder::addFunction)
            }
            file?.also { fileBuilder.build().writeTo(codeGenerator, Dependencies(false, file)) }
        }
    }

    private inner class DomainLayerVisitor(
        resolver: Resolver,
        globalKDTypes: MutableMap<TypeName, KDType>,
        generateClassName: KSClassDeclaration.() -> ClassName
    ) : KDVisitor(resolver, globalKDTypes, logger, generateClassName) {

        override fun createBuilder(model: KDType.Model) {
            KDTypeBuilderBuilder.create(model, false, kdLogger).also { helper ->
                model.builder.addType(helper.build())
                helper.buildFunToBuilder().also(model.builder::addFunction)
            }
            KDTypeBuilderBuilder.create(model, true, kdLogger).also { builderBuilder ->
                model.builder.addType(builderBuilder.build())
                builderBuilder.buildFunToBuilder().also(model.builder::addFunction)
            }
        }
    }
}
