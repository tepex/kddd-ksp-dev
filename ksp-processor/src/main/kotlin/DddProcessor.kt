package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import ru.it_arch.clean_ddd.domain.ILogger
import ru.it_arch.clean_ddd.domain.KdddType
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.kddd.KDIgnore

internal class DddProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Options,
    private val logger: ILogger,
    private val isTesting: Boolean
) : SymbolProcessor {

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.log("options: $options, isTesting: $isTesting")
        val visitor = Visitor(resolver, options, logger)

        // TODO: dirty!!! refactor this 💩
        // Пакет общих файлов-расширений. Пока нет определенности, как лучше его выбрать. Пока-что берется первый попавшийся.
        //var basePackageName: ClassName.PackageName? = null

        val outputFiles = resolver.getNewFiles().toList().mapNotNull { file ->
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.INTERFACE && it.getAnnotationsByType(KDIgnore::class).count() == 0 }
                .firstOrNull()
                ?.let { declaration ->
                    logger.log("process file: $file, declaration: $declaration")
                    //basePackageName ?: run { basePackageName = declaration toImplementationPackage options.subpackage }
                    visitor.visitKDDeclaration(declaration, null).let { kdddType ->
                        if (kdddType is KdddType.ModelContainer) with(options) { declaration.toOutputFile(kdddType, file) }
                        else null
                    }
                }
        }


        logger.log("type catalog: ")

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
}
