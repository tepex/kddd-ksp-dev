package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.core.Data
import ru.it_arch.clean_ddd.domain.core.IEntity
import ru.it_arch.clean_ddd.domain.core.KdddType

public fun Map<String, String>.toOptions(): Options = options {
    subpackage = get(Options.OPTION_IMPLEMENTATION_SUBPACKAGE)
    generatedClassNameRe = get(Options.OPTION_GENERATED_CLASS_NAME_RE)
    generatedClassNameResult = get(Options.OPTION_GENERATED_CLASS_NAME_RESULT)
    useContextParameters = get(Options.OPTION_CONTEXT_PARAMETERS)?.toBooleanStrictOrNull()
    jsonNamingStrategy = get(Options.OPTION_JSON_NAMING_STRATEGY)
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

context(ctx: Context, _: Options)
public fun String.toKDddTypeOrNull(): KdddType? = ctx.toGeneratable().let { generatable ->
    when (this) {
        Data::class.java.simpleName           -> Data(generatable, ctx.properties)
        IEntity::class.java.simpleName        -> IEntity(Data(generatable, ctx.properties))
        KdddType.Boxed::class.java.simpleName -> this toBoxedTypeWith generatable
        else -> null
    }
}

context(options: Options)
public val String.toImplementationPackage: CompositeClassName.PackageName
    get() = CompositeClassName.PackageName("$this.${options.subpackage}")

public val CompositeClassName.fullClassName: CompositeClassName.FullClassName
    get() = CompositeClassName.FullClassName("$packageName.$className")
