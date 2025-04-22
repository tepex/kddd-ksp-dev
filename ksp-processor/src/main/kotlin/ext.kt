package ru.it_arch.clean_ddd.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.ksp_model.TypeCatalog
import ru.it_arch.clean_ddd.ksp_model.utils.KDLogger
import ru.it_arch.clean_ddd.ksp_model.model.KDOutputFile
import ru.it_arch.clean_ddd.ksp_model.model.KDProperty
import ru.it_arch.clean_ddd.ksp_model.model.KDType
import ru.it_arch.clean_ddd.ksp_model.model.KDTypeContext
import ru.it_arch.clean_ddd.ksp_model.utils.PackageName
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
        KDType.Data::class.java.simpleName    -> Result.success(KDType.Data())
        KDType.IEntity::class.java.simpleName -> Result.success(KDType.IEntity())
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


context(options: KDOptions, logger: KDLogger)
@OptIn(KspExperimental::class)
internal fun typeContext(
    declaration: KSClassDeclaration,
    typeCatalog: TypeCatalog,
    kDddName: TypeName
): KDTypeContext {

    //val options: KDOptions = KDOptions

    val kDddPackage = PackageName(declaration.packageName.asString())
    val implPackage = kDddPackage + options.subpackage

/*
    override val className = annotations.filterIsInstance<KDGeneratable>().firstOrNull()?.implementationName
        ?.takeIf { it.isNotBlank() }?.let(ClassName::bestGuess) ?: context.toBeGenerated
    ClassName.bestGuess("${implPackage.boxed}.${options.toImplementationClassName(kDddType.toString())}")
*/

    val annotations = declaration.getAnnotationsByType(KDGeneratable::class) + declaration.getAnnotationsByType(KDParsable::class)
    // Имя класса имплементации задается через аннотацию `@KDGeneratable` или на основе привила, определенного в опциях.
    val implClass = (annotations.filterIsInstance<KDGeneratable>().firstOrNull()?.implementationName
        ?.takeIf { it.isNotBlank() } ?: "${implPackage.boxed}.${options.toImplementationClassName(kDddName.toString())}")
        .let(ClassName::bestGuess)

    val properties = declaration.getAllProperties().map { property ->
        KDProperty(
            implClass.member(property.simpleName.asString()),
            property.type.toTypeName(),
            property.getAnnotationsByType(KDSerialName::class).firstOrNull()
        )
    }.toList()

    return KDTypeContext(
        typeCatalog,
        kDddPackage,
        kDddPackage + options.subpackage,
        kDddName,
        implClass,
        annotations,
        properties
    )
}
