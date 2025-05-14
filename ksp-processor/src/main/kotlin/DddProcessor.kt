package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
import ru.it_arch.clean_ddd.domain.ILogger
import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.clean_ddd.domain.Options

internal class DddProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Options,
    private val logger: ILogger,
    private val isTesting: Boolean
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.log("options: $options, isTesting: $isTesting")
        val visitor = Visitor(resolver, options, logger)

        // TODO: dirty!!! refactor this 💩
        // Пакет общих файлов-расширений. Пока нет определенности, как лучше его выбрать. Пока-что берется первый попавшийся.
        //var basePackageName: ClassName.PackageName? = null

        resolver.getNewFiles().toList().mapNotNull { file ->
            //logger.log("process $file")
            with(options) { with(logger) { file `to OutputFile with` visitor } }
        }.forEach { file ->
            val (model, dependencies) = file
            //buildAndAddNestedTypes(file.first)

            logger.log("creating file: {, ${model.impl}}")

            // add model content and build()
            with(visitor.typeCatalog) {
                val modelTypeSpec = with(logger) { model.typeSpecBuilder.build() }
                    //.also { logger.log("model: $it, ") }

                val fileSpec = file.fileSpecBuilder.apply {
                    addType(modelTypeSpec)
                }.build()

                codeGenerator.createNewFile(dependencies, model.impl.packageName.boxed, model.impl.className.boxed)
                    .also { StringBufferedWriter(it).use(fileSpec::writeTo) }
            }
        }


        //logger.log("type catalog: ${visitor.typeCatalog}")
        visitor.typeCatalog.entries.forEach { pair ->
            logger.log("${pair.key} -> ${pair.value}")
        }

        /*
        outputFiles.keys.forEach { file ->
            visitor.buildAndAddNestedTypes(file.first)

            //createBuilders(file.model)

            // TODO if (file.generatable.hasDsl) createDslBuilderExtensionFunction(file)
            //if (file.model.hasJson) createJsonExtensionFunctions(file)
        }*/

        /*
        outputFiles.entries
            .forEach { it.key.fileSpecBuilder.build().replaceAndWrite(codeGenerator, Dependencies(false, it.value)) }*/

        //basePackageName?.let(::generateJsonProperty)

        return emptyList()
    }


    /** Перехват выходного потока с построчной буферизацией. Нужно для подмены строк на выходе. Грязный хак. */
    private fun FileSpec.replaceAndWrite(codeGenerator: CodeGenerator, dependencies: Dependencies) {
        codeGenerator.createNewFile(dependencies, packageName, name).also { StringBufferedWriter(it).use(::writeTo) }
    }

    tailrec fun buildAndAddNestedTypes(model: KdddType.ModelContainer, isFinish: Boolean = false) {
        val nestedModels = model.nestedTypes.filterIsInstance<KdddType.ModelContainer>()
        return if (nestedModels.isEmpty() || isFinish) {
            // append
            model.nestedTypes.forEach { type ->
                //if (type is KDType.Model) createBuilders(type)

                // TODO:
                // model.builder.addType(type.builder.build())
            }
        } else {
            nestedModels.forEach(::buildAndAddNestedTypes)
            buildAndAddNestedTypes(model, true)
        }
    }
}
