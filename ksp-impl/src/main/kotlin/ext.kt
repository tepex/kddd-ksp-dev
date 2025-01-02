package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.ksp.model.KDLogger
import ru.it_arch.clean_ddd.ksp.model.KDOptions
import ru.it_arch.clean_ddd.ksp.model.KDOutputFile
import ru.it_arch.clean_ddd.ksp.model.KDProperty
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.clean_ddd.ksp.model.KDTypeContext
import ru.it_arch.clean_ddd.ksp.model.KDTypeContext.PackageName
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.KDSerialName

context(KDTypeContext)
@OptIn(KspExperimental::class)
internal fun KSClassDeclaration.kdTypeOrNull(logger: KDLogger): Result<KDType?> {
    val annotations = getAnnotationsByType(KDGeneratable::class) + getAnnotationsByType(KDParsable::class)
    //logger.log("$this ${annotations.toList()}")
    superTypes.forEach { item -> item.kdTypeOrNull(annotations)?.also { return it } }
    // Not found
    return Result.success(null)
}

context(KDTypeContext)
@OptIn(KspExperimental::class)
private fun KSTypeReference.kdTypeOrNull(annotations: Sequence<Annotation>): Result<KDType>? = when(toString()) {
    KDType.Sealed::class.java.simpleName  -> Result.success(KDType.Sealed.create())
    KDType.Data::class.java.simpleName    -> Result.success(KDType.Data.create(annotations.toList(), false))
    KDType.IEntity::class.java.simpleName -> Result.success(KDType.IEntity.create(annotations.toList()))
    KDType.Boxed::class.java.simpleName   -> runCatching { KDType.Boxed.create(annotations.toList(), toTypeName()) }
    else -> null
}

context(KDOptions)
internal fun createOutputFile(declaration: KSClassDeclaration, generatable: KDType.Generatable): KDOutputFile = KDOutputFile(
    generatable,
    getImplementationPackage(declaration.packageName.asString()),
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
        KDProperty.create(
            toBeGenerated.member(it.simpleName.asString()),
            it.type.toTypeName(),
            it.getAnnotationsByType(KDSerialName::class).firstOrNull()
        )
    }.toList()
)
