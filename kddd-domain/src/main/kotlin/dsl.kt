package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.core.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.core.BoxedWithPrimitive
import ru.it_arch.clean_ddd.domain.core.Generatable
import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd

/**
 *
 * */
public fun property(block: Property.Builder.() -> Unit): Property =
    Property.Builder().apply(block).build()

public fun compositeClassName(block: CompositeClassName.Builder.() -> Unit): CompositeClassName =
    CompositeClassName.Builder().apply(block).build()

/**
 *
 * */
public fun options(block: Options.Builder.() -> Unit): Options =
    Options.Builder().apply(block).build()


context(options: Options)
/**
 *
 * */
public fun kDddContext(block: Context.Builder.() -> Unit): Context =
    Context.Builder().apply(block).build()




internal fun generatable(block: GeneratableImpl.Builder.() -> Unit): Generatable =
    GeneratableImpl.Builder().apply(block).build()

context(_: Options)
internal fun Context.toGeneratable(): Generatable =
    generatable {
        kddd = this@toGeneratable.kddd
        impl = this@toGeneratable.kddd `to implementation class name from options or from` annotations
        enclosing = this@toGeneratable.enclosing
    }

context(_: Options)
/**
 * Создание класса имплементации [CompositeClassName.ClassName] из имени класса [Kddd]-типа.
 *
 * @receiver исходное полное имя [Kddd]-типа.
 * @param annotations список аннотаций, возможно содержащий аннотацию [KDGeneratable], переопределяющую имя имплементации.
 * @return класс имплементации [CompositeClassName].
 * */
internal infix fun CompositeClassName.`to implementation class name from options or from`(annotations: List<Annotation>): CompositeClassName =
    (annotations.filterIsInstance<KDGeneratable>().firstOrNull()?.implementationName
        ?.takeIf { it.isNotEmpty() }?.let { CompositeClassName.ClassName(it) }
        ?: className.`to implementation class name from options`)
        .let { CompositeClassName(packageName.toImplementationPackage, it) }

context(options: Options)
internal val CompositeClassName.PackageName.toImplementationPackage: CompositeClassName.PackageName
    get() = this + options.subpackage


context(options: Options)
/**
 * Создание имени класса имплементации из имени класса [Kddd]-типа.
 *
 * @receiver исходное полное имя [Kddd]-типа.
 * @return имя класса имплементации.
 * */
internal val CompositeClassName.ClassName.`to implementation class name from options`: CompositeClassName.ClassName get() {
    var result = options.generatedClassNameResult.boxed
    options.generatedClassNameRe.find(boxed)?.groupValues?.forEachIndexed { i, group ->
        group.takeIf { i > 0 }?.also { result = result.replace("\$$i", it) }
    }
    return CompositeClassName.ClassName(result)
}

context(ctx: Context)
/**
 * Преобразование параметризированного типа `BOXED` из [ValueObject.Boxed<BOXED>] в [KdddType.Boxed] с соотвествующим
 * типом `boxed`: [BoxedWithPrimitive.PrimitiveClassName] или [BoxedWithCommon.CommonClassName].
 *
 * @param generatable необходимый делегат [KdddType.Generatable].
 * @receiver имя параметризированного типа.
 * @return созданный [KdddType.Boxed].
 * @see "src/test/kotlin/ClassNameTest.kt"
 * */
internal infix fun String.toBoxedTypeWith(generatable: Generatable): KdddType.Boxed =
    BoxedWithPrimitive.PrimitiveClassName.entries.find { it.name == uppercase() }
        ?.let { BoxedWithPrimitive(generatable, it) }
        ?: BoxedWithCommon(
            generatable,
            BoxedWithCommon.CommonClassName(this),
            ctx.getAnnotation<KDParsable>() ?: KDParsable()
        )

private inline fun <reified T : Annotation> Context.getAnnotation(): T? =
    annotations.filterIsInstance<T>().firstOrNull()
