package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.KdddType
import ru.it_arch.clean_ddd.domain.Property
import ru.it_arch.clean_ddd.domain.property
import ru.it_arch.clean_ddd.domain.toKDddTypeOrNull

context(_: KDTypeContext)
/**
 * ValueObject.* -> KdddType.*
 */
internal fun KSTypeReference.toKdddTypeOrNull(): KdddType? =
    toString().substringBefore('<').toKDddTypeOrNull()

context(options: KDOptions)
internal fun createOutputFile(declaration: KSClassDeclaration, model: KdddType.ModelContainer): KDOutputFile =
    KDOutputFile(
        model,
        declaration toImplementationPackage options.subpackage,
        //options.getBuilderFunctionName(declaration.simpleName.asString()),
        //options.isUseContextParameters
    )

internal fun KSPropertyDeclaration.toProperty(): Property = type.toTypeName().let { type ->
    property {
        name = simpleName.asString()
        className = type.toString()
        isNullable = type.isNullable
    }
}
