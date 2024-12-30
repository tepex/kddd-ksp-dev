package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.ksp.model.KDLogger
import ru.it_arch.clean_ddd.ksp.model.KDOptions
import ru.it_arch.clean_ddd.ksp.model.KDOutputFile
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.clean_ddd.ksp.model.KDTypeContext
import ru.it_arch.clean_ddd.ksp.model.KDTypeContext.PackageName
import ru.it_arch.clean_ddd.ksp.model.KDTypeContext.Property
import ru.it_arch.kddd.KDSerialName

context(KDTypeContext)
internal fun KSClassDeclaration.kdTypeOrNull(): Result<KDType?> {
    superTypes.forEach { item -> item.kdTypeOrNull()?.also { return it } }
    // Not found
    return Result.success(null)
}

context(KDTypeContext)
private fun KSTypeReference.kdTypeOrNull(): Result<KDType>? = when(toString()) {
    KDType.Sealed::class.java.simpleName      -> Result.success(KDType.Sealed.create())
    KDType.Data::class.java.simpleName        -> Result.success(KDType.Data.create(false))
    KDType.IEntity::class.java.simpleName     -> Result.success(KDType.IEntity.create())
    KDType.Boxed::class.java.simpleName       -> runCatching { KDType.Boxed.create(toTypeName()) }
    else -> null
}

context(KDOptions)
internal fun createOutputFile(declaration: KSClassDeclaration, kdType: KDType, /*srcFile: KSFile*/): KDOutputFile = KDOutputFile(
    kdType,
    getImplementationPackage(declaration.packageName.asString()),
    getImplementationClassName(declaration.simpleName.asString()),
    getBuilderFunctionName(declaration.simpleName.asString()),
    useContextReceivers
)

@OptIn(KspExperimental::class)
internal fun typeContext(
    options: KDOptions,
    logger: KDLogger,
    globalKDTypes: Map<TypeName, KDType>,
    toBeGenerated: ClassName,
    typeName: TypeName,
    declaration: KSClassDeclaration
): KDTypeContext = KDTypeContext(
    options,
    logger,
    globalKDTypes,
    toBeGenerated,
    typeName,
    PackageName.packageName(declaration.packageName.asString()),
    declaration.getAllProperties().map {
        Property(
            toBeGenerated.member(it.simpleName.asString()),
            it.type.toTypeName(),
            it.getAnnotationsByType(KDSerialName::class).firstOrNull()
        )
    }.toList()
)
