package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd

public fun Map<String, String>.toOptions(): Options = options {
    subpackage = get(Options.OPTION_IMPLEMENTATION_SUBPACKAGE)
    generatedClassNameRe = get(Options.OPTION_GENERATED_CLASS_NAME_RE)
    generatedClassNameResult = get(Options.OPTION_GENERATED_CLASS_NAME_RESULT)
    useContextParameters = get(Options.OPTION_CONTEXT_PARAMETERS)?.toBooleanStrictOrNull()
    jsonNamingStrategy = get(Options.OPTION_JSON_NAMING_STRATEGY)
}

context(options: Options)
/**
 * Создание класса имплементации [KdddType.Generatable.ImplClassName] из имени класса [Kddd]-типа.
 *
 * @receiver исходное полное имя [Kddd]-типа.
 * @param annotations список аннотаций, возможно содержащий аннотацию [KDGeneratable], переопределяющую имя имплементации.
 * @return класс имплементации [KdddType.Generatable.ImplClassName].
 * */
internal infix fun String.`to implementation class name with @KDGeneratable in`(annotations: List<Annotation>): KdddType.Generatable.ImplClassName =
    (annotations.filterIsInstance<KDGeneratable>().firstOrNull()?.implementationName
        ?.takeIf { it.isNotEmpty() } ?: `to implementation class name`)
        .let { KdddType.Generatable.ImplClassName(it) }

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

/**
 * Преобразование строкового имени класса (вложенного) в цепочку [ClassName].
 *
 * Пример:
 * ```
 * "A.B.C": ClassName(name = "C").enclosing -> ClassName(name = "B").enclosing -> ClassName(name = "A").enclosing = null
 * ```
 * @see "src/test/kotlin/ClassNameTest.kt"
 * */
//context(_: PackageName)
/*
public fun String.toKddClassName(): KdddType.KdddClassName {
    val classNames = split('.')
    var parent: KdddType.KdddClassName? = null
    classNames.forEach { className ->
        parent = className {
            name = ClassName.Name.KdddType(className)
            enclosing = parent
        }
    }
    return parent!!
}*/

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
public infix fun String.toBoxedTypeWith(generatable: KdddType.Generatable): KdddType.Boxed {
    //val ctx = kDddContext {  }
    return BoxedWithPrimitive.PrimitiveClassName.entries.find { it.name == uppercase() }
        ?.let { BoxedWithPrimitive(generatable, it) }
        ?: BoxedWithCommon(
            generatable,
            BoxedWithCommon.CommonClassName(this),
            ctx.getAnnotation<KDParsable>() ?: KDParsable()
        )
}

context(ctx: Context)
internal fun toGeneratable(): KdddType.Generatable =
    GeneratableImpl(ctx.kddd, ctx.impl, ctx.parent)

internal inline fun <reified T : Annotation> Context.getAnnotation(): T? =
    annotations.filterIsInstance<T>().firstOrNull()

context(ctx: Context)
public fun String.toKDddTypeOrNull(): KdddType? = toGeneratable().let { generatable ->
    when(this) {
        Data::class.java.simpleName    -> Data(generatable, ctx.properties)
        IEntity::class.java.simpleName -> IEntity(Data(generatable, ctx.properties))
        KdddType.Boxed::class.java.simpleName -> this toBoxedTypeWith generatable
        else -> null
    }
}

context(options: Options)
public val String.toImplementationPackage: String
    get() ="$this.${options.subpackage}"
