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
import ru.it_arch.k3dm.Generatable
import ru.it_arch.k3dm.Parsable
import ru.it_arch.k3dm.SerialName

context(_: KDTypeContext)
@OptIn(KspExperimental::class)
internal fun KSClassDeclaration.kdTypeOrNull(logger: KDLogger): Result<KDType?> {
    val annotations = getAnnotationsByType(Generatable::class) + getAnnotationsByType(Parsable::class)
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
        KDType.Entity::class.java.simpleName -> Result.success(KDType.Entity(annotations.toList()))
        KDType.Value::class.java.simpleName   -> runCatching { KDType.Value(annotations.toList(), toTypeName()) }
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
        KDProperty(
            toBeGenerated.member(it.simpleName.asString()),
            it.type.toTypeName(),
            it.getAnnotationsByType(SerialName::class).firstOrNull()
        )
    }.toList()
)
