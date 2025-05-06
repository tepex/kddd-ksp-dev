package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.domain.Context
import ru.it_arch.clean_ddd.domain.KdddType
import ru.it_arch.clean_ddd.domain.Options
import ru.it_arch.clean_ddd.domain.Property
import ru.it_arch.clean_ddd.domain.property
import ru.it_arch.clean_ddd.domain.toImplementationPackage
import ru.it_arch.clean_ddd.domain.toKDddTypeOrNull

internal fun KSPropertyDeclaration.toProperty(): Property = type.toTypeName().let { type ->
    property {
        name = simpleName.asString()
        className = type.toString()
        isNullable = type.isNullable
    }
}

context(_: Context)
/**
 * ValueObject.* -> KdddType.*
 */
internal fun KSTypeReference.toKdddTypeOrNull(): KdddType? =
    toString().substringBefore('<').toKDddTypeOrNull()

internal typealias OutputFile = Triple<KdddType.ModelContainer, String, KSFile>

context(_: Options)
internal fun KSClassDeclaration.toOutputFile(model: KdddType.ModelContainer, file: KSFile): OutputFile =
    OutputFile(model, packageName.asString().toImplementationPackage, file)
