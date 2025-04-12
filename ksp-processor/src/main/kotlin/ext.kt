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
import ru.it_arch.clean_ddd.ksp.model.KDProperty
import ru.it_arch.clean_ddd.ksp.model.KDType
import ru.it_arch.clean_ddd.ksp.model.KDTypeContext
import ru.it_arch.clean_ddd.ksp.model.KDTypeContext.PackageName
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.KDSerialName

context(_: KDTypeContext)
@OptIn(KspExperimental::class)
internal fun KSClassDeclaration.kdTypeOrNull(logger: KDLogger): Result<KDType?> {
    val annotations = getAnnotationsByType(KDGeneratable::class) + getAnnotationsByType(KDParsable::class)
    //logger.log("$this ${annotations.toList()}")
    superTypes.forEach { item -> item.kdTypeOrNull(annotations)?.also { return it } }
    // Not found
    return Result.success(null)
}

context(_: KDTypeContext)
private fun KSTypeReference.kdTypeOrNull(annotations: Sequence<Annotation>): Result<KDType>? =
    when(toString().substringBefore('<')) {
        KDType.Sealed::class.java.simpleName  -> Result.success(KDType.Sealed())
        KDType.Data::class.java.simpleName    -> Result.success(KDType.Data(annotations.toList(), false))
        KDType.IEntity::class.java.simpleName -> Result.success(KDType.IEntity(annotations.toList()))
        KDType.Boxed::class.java.simpleName   -> runCatching { KDType.Boxed(annotations.toList(), toTypeName()) }
        else -> null
    }

context(options: KDOptions)
internal fun createOutputFile(declaration: KSClassDeclaration, generatable: KDType.Generatable): KDOutputFile = KDOutputFile(
    generatable,
    options.getImplementationPackage(declaration.packageName.asString()),
    options.getBuilderFunctionName(declaration.simpleName.asString()),
    options.isUseContextParameters
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
    PackageName(declaration.packageName.asString()),
    declaration.getAllProperties().map {
        KDProperty.create(
            toBeGenerated.member(it.simpleName.asString()),
            it.type.toTypeName(),
            it.getAnnotationsByType(KDSerialName::class).firstOrNull()
        )
    }.toList()
)
