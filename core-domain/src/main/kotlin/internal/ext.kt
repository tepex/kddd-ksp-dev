package ru.it_arch.kddd.domain.internal

import ru.it_arch.kddd.domain.options
import ru.it_arch.kddd.domain.model.CompositeClassName
import ru.it_arch.kddd.domain.model.Context
import ru.it_arch.kddd.domain.model.ILogger
import ru.it_arch.kddd.domain.model.Options
import ru.it_arch.kddd.domain.model.type.ValueClassWithCommon
import ru.it_arch.kddd.domain.model.type.ValueClassWithPrimitive
import ru.it_arch.kddd.domain.model.type.Generatable
import ru.it_arch.kddd.domain.model.type.KdddType
import ru.it_arch.kddd.domain.shortName
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd

internal fun generatable(block: GeneratableImpl.Builder.() -> Unit): Generatable =
    GeneratableImpl.Builder().apply(block).build()

context(_: Options)
/**
 * @param _ [Options] как Context Parameter
 * */
internal fun Context.toGeneratable(): Generatable =
    generatable {
        kddd = this@toGeneratable.kddd
        impl = (this@toGeneratable.kddd.className `to implementation class name from options or from` annotations)
            .let { className -> this@toGeneratable.enclosing?.impl?.className?.let { it + className } ?: className }
            .let { className ->
                CompositeClassName(
                    this@toGeneratable.enclosing?.impl?.packageName
                        ?: this@toGeneratable.kddd.packageName.toImplementationPackage,
                    className
                )
            }
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
internal infix fun CompositeClassName.ClassName.`to implementation class name from options or from`(
    annotations: Set<Annotation>
): CompositeClassName.ClassName =
    annotations.filterIsInstance<KDGeneratable>().firstOrNull()?.implementationName
        ?.takeIf { it.isNotEmpty() }?.let { CompositeClassName.ClassName(it) }
        ?: `to implementation class name from options`

context(options: Options)
/**
 * Создание имени класса имплементации из имени класса [Kddd]-типа.
 *
 * @receiver исходное полное имя [Kddd]-типа.
 * @return имя класса имплементации.
 * */
internal val CompositeClassName.ClassName.`to implementation class name from options`: CompositeClassName.ClassName get() {
    var result = options.generatedClassNameResult.boxed
    options.generatedClassNameRe.find(shortName)?.groupValues?.forEachIndexed { i, group ->
        group.takeIf { i > 0 }?.also { result = result.replace("\$$i", it) }
    }
    return CompositeClassName.ClassName(result)
}

context(options: Options)
internal val CompositeClassName.PackageName.toImplementationPackage: CompositeClassName.PackageName
    get() = this + options.subpackage

context(ctx: Context)
/**
 * Преобразование параметризированного типа `BOXED` из [ValueObject.Boxed<BOXED>] в [KdddType.ValueClass] с соотвествующим
 * типом `boxed`: [ValueClassWithPrimitive.PrimitiveClassName] или [ValueClassWithCommon.CommonClassName].
 *
 * @param generatable необходимый делегат [KdddType.Generatable].
 * @receiver имя параметризированного типа.
 * @return созданный [KdddType.ValueClass].
 * @see "src/test/kotlin/ClassNameTest.kt"
 * */
internal infix fun String.toBoxedTypeWith(generatable: Generatable): KdddType.ValueClass =
    BOXED_INVARIANT_RE.find(this)?.groupValues?.getOrNull(1)?.uppercase().let { boxedType ->
        ValueClassWithPrimitive.PrimitiveClassName.entries.find { it.name == boxedType }
            ?.let { ValueClassWithPrimitive(generatable, it) }
            ?: ValueClassWithCommon(
                generatable,
                ValueClassWithCommon.CommonClassName(boxedType ?: error("Can't find boxed type for: `$this`")),
                ctx.getAnnotation<KDParsable>() ?: KDParsable()
            )
    }

internal val Context.hasDsl: Boolean
    get() = getAnnotation<KDGeneratable>()?.dsl ?: true

private val BOXED_INVARIANT_RE = "<INVARIANT (\\w+)>".toRegex()

private inline fun <reified T : Annotation> Context.getAnnotation(): T? =
    annotations.filterIsInstance<T>().firstOrNull()



private fun `preserve imports, not used`(context: Context, options: Options, logger: ILogger) {}
