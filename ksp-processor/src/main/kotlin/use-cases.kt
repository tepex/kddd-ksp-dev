package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.clean_ddd.domain.CompositeClassName
import ru.it_arch.clean_ddd.domain.ILogger
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.clean_ddd.domain.model.KdddType
import ru.it_arch.clean_ddd.domain.model.Data
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.shortName
import ru.it_arch.clean_ddd.ksp.model.ExtensionFile
import ru.it_arch.kddd.KDIgnore

context(options: Options, logger: ILogger)
@OptIn(KspExperimental::class)
internal infix fun KSFile.`to OutputFile with`(visitor: Visitor): OutputFile? =
    declarations
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.INTERFACE && it.getAnnotationsByType(KDIgnore::class).count() == 0 }
        .firstOrNull()
        ?.let { declaration ->
            visitor.visitKDDeclaration(declaration, null).let { kdddType ->
                if (kdddType is KdddType.ModelContainer) OutputFile(kdddType, Dependencies(false, this))
                else null
            }
        }

context(typeCatalog: TypeCatalog, logger: ILogger)
/**
 *
 * */
internal fun KdddType.toTypeSpecBuilder(dslFile: ExtensionFile): TypeSpec.Builder {
    //val typeCatalog: TypeCatalog = TypeCatalog
    return typeCatalog[kddd]?.let { holder ->
        TypeSpec.classBuilder(impl.className.shortName).addSuperinterface(holder.classType).apply {
            when(this@toTypeSpecBuilder) {
                is KdddType.ModelContainer ->
                    if (this@toTypeSpecBuilder is Data) build(holder, dslFile)
                is KdddType.Boxed -> build(holder)
            }
        }
    } ?: error("Type ${kddd.fullClassName} not found in type catalog!")
}

internal fun List<OutputFile>.createDslFile(): ExtensionFile =
    findShortestPackageName().let { shortestPackageName ->
        FileSpec.builder(shortestPackageName.boxed, "dsl")
            .addFileComment(FILE_HEADER_STUB)
            .let { dslFileBuilder ->
                extensionFile {
                    builder = dslFileBuilder
                    packageName = shortestPackageName
                    name = "dsl"
                }
            }
    }

internal fun List<OutputFile>.findShortestPackageName(): CompositeClassName.PackageName =
    reduce { shortest, outputFile ->
        outputFile.takeIf { it.first.impl.packageName.boxed.length < shortest.first.impl.packageName.boxed.length } ?: shortest
    }.first.impl.packageName

