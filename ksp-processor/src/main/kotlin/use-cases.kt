package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.CompositeClassName
import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.clean_ddd.domain.Context
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.clean_ddd.domain.Property
import ru.it_arch.clean_ddd.domain.core.Generatable
import ru.it_arch.clean_ddd.domain.property
import ru.it_arch.clean_ddd.domain.toImplementationPackage
import ru.it_arch.clean_ddd.domain.toKDddTypeOrNull
import ru.it_arch.kddd.KDIgnore

internal fun KSPropertyDeclaration.toProperty(): Property = type.toTypeName().let { type ->
    property {
        name = simpleName.asString()
        className = type.toString()
        isNullable = type.isNullable
    }
}

context(_: Context, _: Options)
/**
 * ValueObject.* -> KdddType.*
 */
internal fun KSTypeReference.toKdddTypeOrNull(): KdddType? =
    toString().substringBefore('<').toKDddTypeOrNull()

/**
 * */
internal typealias OutputFile = Triple<KdddType.ModelContainer, CompositeClassName.PackageName, Dependencies>

context(options: Options)
@OptIn(KspExperimental::class)
internal infix fun KSFile.`to OutputFile with`(visitor: Visitor): OutputFile? =
    declarations
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.INTERFACE && it.getAnnotationsByType(KDIgnore::class).count() == 0 }
        .firstOrNull()
        ?.let { declaration ->
            //basePackageName ?: run { basePackageName = declaration toImplementationPackage options.subpackage }
            visitor.visitKDDeclaration(declaration, null).let { kdddType ->
                if (kdddType is KdddType.ModelContainer)
                    OutputFile(kdddType, packageName.asString().toImplementationPackage, Dependencies(false, this))
                else null
            }
        }

internal fun List<OutputFile>.findShortestPackageName(): CompositeClassName.PackageName =
    reduce { acc, outputFile ->
        outputFile.takeIf { it.second.boxed.length < acc.second.boxed.length } ?: acc
    }.second

internal val OutputFile.fileSpecBuilder: FileSpec.Builder
    get() = FileSpec.builder(second.boxed, first.impl.boxed).also { it.addFileComment(FILE_HEADER_STUB) }

private const val FILE_HEADER_STUB: String = """
AUTO-GENERATED FILE. DO NOT MODIFY.
This file generated automatically by «KDDD» framework.
Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
"""

internal fun Generatable.toTypeSpecBuilder(type: TypeName): TypeSpec.Builder =
    TypeSpec.classBuilder(impl.boxed).addSuperinterface(type)
