package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd

internal val CompositeClassName.fullClassName: CompositeClassName.FullClassName
    get() = CompositeClassName.FullClassName("$packageName.$className")

internal inline fun <reified T : Annotation> Context.getAnnotation(): T? =
    annotations.filterIsInstance<T>().firstOrNull()

context(ctx: Context, _: Options)
internal fun toGeneratable(annotations: List<Annotation>): KdddType.Generatable =
    generatable {
        kddd = ctx.kddd
        impl = ctx.kddd.boxed `to implementation class name with @KDGeneratable in` annotations
        enclosing = ctx.parent
    }

context(_: Options)
/**
 * Создание класса имплементации [CompositeClassName.ClassName] из имени класса [Kddd]-типа.
 *
 * @receiver исходное полное имя [Kddd]-типа.
 * @param annotations список аннотаций, возможно содержащий аннотацию [KDGeneratable], переопределяющую имя имплементации.
 * @return класс имплементации [CompositeClassName.ClassName].
 * */
internal infix fun String.`to implementation class name with @KDGeneratable in`(annotations: List<Annotation>): CompositeClassName.ClassName =
    (annotations.filterIsInstance<KDGeneratable>().firstOrNull()?.implementationName
        ?.takeIf { it.isNotEmpty() } ?: `to implementation class name`)
        .let { CompositeClassName.ClassName(it) }

context(options: Options)
/**
 * Создание имени класса имплементации из имени класса [Kddd]-типа.
 *
 * @receiver исходное полное имя [Kddd]-типа.
 * @return имя класса имплементации.
 * */
internal val String.`to implementation class name`: String get() {
    var result = options.generatedClassNameResult.boxed
    options.generatedClassNameRe.find(this)?.groupValues?.forEachIndexed { i, group ->
        group.takeIf { i > 0 }?.also { result = result.replace("\$$i", it) }
    }
    return result
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
internal infix fun String.toBoxedTypeWith(generatable: KdddType.Generatable): KdddType.Boxed =
    BoxedWithPrimitive.PrimitiveClassName.entries.find { it.name == uppercase() }
        ?.let { BoxedWithPrimitive(generatable, it) }
        ?: BoxedWithCommon(
            generatable,
            BoxedWithCommon.CommonClassName(this),
            ctx.getAnnotation<KDParsable>() ?: KDParsable()
        )
