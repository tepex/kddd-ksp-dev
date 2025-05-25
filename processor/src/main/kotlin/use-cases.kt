package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.clean_ddd.domain.model.CompositeClassName
import ru.it_arch.clean_ddd.domain.model.kddd.KdddType
import ru.it_arch.clean_ddd.domain.model.kddd.Data
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.generateBoxed
import ru.it_arch.clean_ddd.domain.generateData
import ru.it_arch.clean_ddd.domain.generateEntity
import ru.it_arch.clean_ddd.domain.model.Context
import ru.it_arch.clean_ddd.domain.model.ILogger
import ru.it_arch.clean_ddd.domain.model.Options
import ru.it_arch.clean_ddd.domain.model.kddd.IEntity
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
// TODO: return abstract wrapper KotlinCodeBuilder
internal fun KdddType.toTypeSpecBuilder(/*typeCatalog: TypeCatalog,*/ dslFile: ExtensionFile): TypeSpec.Builder =
    typeCatalog.getTypeHolderOrError(kddd.fullClassName).let { holder ->
        TypeSpec.classBuilder(impl.className.shortName).addSuperinterface(holder.classType).apply {
            when(this@toTypeSpecBuilder) {
                is KdddType.ModelContainer -> when (val model = (this@toTypeSpecBuilder as KdddType.ModelContainer)) {
                    is Data -> KotlinPoetDataBuilder(this, holder, model).generateData()
                    is IEntity -> KotlinPoetEntityBuilder(this, holder, model).generateEntity()
                }
                is KdddType.Boxed -> KotlinPoetBoxedBuilder(this, holder, this@toTypeSpecBuilder).generateBoxed()
            }
        }
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

private fun `preserve imports for Android Studio, not used`(context: Context, options: Options, logger: ILogger) {}
