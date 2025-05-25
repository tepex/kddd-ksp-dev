package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
import ru.it_arch.clean_ddd.domain.model.ILogger
import ru.it_arch.clean_ddd.domain.model.Options
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.shortName
import ru.it_arch.clean_ddd.ksp.model.StringBufferWriter

internal class DddProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Options,
    private val logger: ILogger,
    private val isTesting: Boolean
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.log("options: $options, isTesting: $isTesting")
        val visitor = Visitor(resolver, options, logger)

        resolver.getNewFiles().toList().mapNotNull { file ->
            //logger.log("process $file")
            with(options) { with(logger) { file `to OutputFile with` visitor } }
        }

            // application logic. Separate DSL, JSON

            .takeIf { it.isNotEmpty() }?.also { files ->
            val dslFile = files.createDslFile()
            files.forEach { file ->
                val (model, dependencies) = file
                logger.log("creating file: {${model.impl.className.shortName}, }")

                with(visitor.typeCatalog) {
                    // Генерация класса имплементации
                    with(logger) { model.toTypeSpecBuilder(dslFile) }

                        // --- test converting for dsl mode
                        .also {
                            it.typeSpecs.forEach { it.toBuilder() }
                        }
                        // ---

                        .build().also { typeSpec ->



                        // Генерация файла
                        file.fileSpecBuilder.addType(typeSpec).build()
                            // Запись файла
                            .also { fileSpec ->
                                codeGenerator.createNewFile(
                                    dependencies,
                                    model.impl.packageName.boxed,
                                    model.impl.className.shortName
                                ).also { StringBufferWriter(it).use(fileSpec::writeTo) }
                            }
                    }
                }
            }
            codeGenerator.createNewFile(
                Dependencies.ALL_FILES,
                dslFile.packageName.boxed,
                dslFile.name.boxed
            ).also { StringBufferWriter(it).use(dslFile.builder.build()::writeTo) }
        }

        //logger.log("type catalog: ${visitor.typeCatalog}")
        visitor.typeCatalog.entries.forEach { pair ->
            logger.log("${pair.key} -> ${pair.value}")
        }

        /*
        outputFiles.entries
            .forEach { it.key.fileSpecBuilder.build().replaceAndWrite(codeGenerator, Dependencies(false, it.value)) }*/

        //basePackageName?.let(::generateJsonProperty)

        return emptyList()
    }

    /** Перехват выходного потока с построчной буферизацией. Нужно для подмены строк на выходе. Грязный хак. */
    private fun FileSpec.replaceAndWrite(codeGenerator: CodeGenerator, dependencies: Dependencies) {
        codeGenerator.createNewFile(dependencies, packageName, name).also { StringBufferWriter(it).use(::writeTo) }
    }
}
