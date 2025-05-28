package ru.it_arch.kddd.presentation

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.domain.compositeClassName
import ru.it_arch.kddd.domain.fullClassName
import ru.it_arch.kddd.domain.kDddContext
import ru.it_arch.kddd.domain.model.CompositeClassName
import ru.it_arch.kddd.domain.model.Options
import ru.it_arch.kddd.domain.model.type.KdddType
import ru.it_arch.kddd.domain.toKDddType
import ru.it_arch.kddd.utils.Utils

public typealias TypeCatalog = Map<CompositeClassName.FullClassName, KspTypeHolder>

public typealias PreOutputFile = Pair<KdddType.DataClass, Dependencies>

/**
 * Преобразование [KSClassDeclaration] в Result<[KdddType]>, добавление [KspTypeHolder] в каталог типов, обход
 * и преобразование вложенных [KSClassDeclaration] и добавление преобразованного [KdddType] к родителю.
 *
 * @receiver входная преобразуемая декларация класса.
 * @param visitor посетитель шаблона проектирования.
 * @param container родительский контейнер вложенных [Kddd]-типов. null — если посещается корневой.
 * @return [Result] преобразованного [KdddType].
 * */
public fun KSClassDeclaration.visit(visitor: Visitor, container: KdddType.DataClass? = null): Result<KdddType> =
    typeParameters.map { visitor.resolver.getTypeArgument(it.bounds.first(), Variance.INVARIANT) }.let { args ->
        asType(args).let { ksType ->
            with(visitor.options) {
                ksType.toKdddTypeResult(visitor.utils, container)
                    .onSuccess { kdddType ->
                        KspTypeHolder(kdddType, ksType, getAllProperties().toList())
                            .also { visitor.addToTypeCatalog(kdddType.kddd.fullClassName, it) }
                        if (kdddType is KdddType.DataClass) accept(visitor, kdddType)
                        container?.addNestedType(kdddType)
                    }
                }
            }
    }

context(_: Options, declaration: KSClassDeclaration)
private fun KSType.toKdddTypeResult(
    //declaration: KSClassDeclaration,
    utils: Utils,
    container: KdddType.DataClass?
): Result<KdddType> = with(
    kDddContext {
        enclosing = container
        kddd = compositeClassName {
            packageName = CompositeClassName.PackageName(declaration.packageName.asString())
            fullClassName = this@toKdddTypeResult.toString()
        }
        annotations = (declaration.getAnnotations<KDGeneratable>() + declaration.getAnnotations<KDParsable>()).toSet()
        properties = utils.toProperties(declaration)
    }
) {
    declaration.superTypes.firstOrNull()?.toString()?.toKDddType()
        ?: Result.failure(IllegalStateException("No Kddd super type for: $declaration"))
}

@OptIn(KspExperimental::class)
public inline fun <reified T: Annotation> KSAnnotated.getAnnotations(): Sequence<T> =
    getAnnotationsByType(T::class)
