package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.type.BoxedWithCommon
import ru.it_arch.clean_ddd.domain.type.BoxedWithPrimitive
import ru.it_arch.clean_ddd.domain.type.GeneratableDelegate
import ru.it_arch.clean_ddd.domain.type.KdddType
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd

public fun Map<String, String>.toOptions(): Options = options {
    subpackage = get(Options.OPTION_IMPLEMENTATION_SUBPACKAGE)
    generatedClassNameRe = get(Options.OPTION_GENERATED_CLASS_NAME_RE)
    generatedClassNameResult = get(Options.OPTION_GENERATED_CLASS_NAME_RESULT)
    useContextParameters = get(Options.OPTION_CONTEXT_PARAMETERS)?.toBooleanStrictOrNull()
    jsonNamingStrategy = get(Options.OPTION_JSON_NAMING_STRATEGY)
}

/**
 * Создание класса имплементации из имени класса [Kddd]-типа.
 *
 * @param kDddClassName исходное полное имя класса.
 * @receiver опции фреймворка.
 * @return [ClassName] имплементации.
 * */
context(options: Options)
public fun String.toImplementationClassName(): String {
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

/**
 * Преобразование параметризированного типа `BOXED` из [ValueObject.Boxed<BOXED>] в [KdddType.Boxed] с соотвествующим типом `boxed`.
 * @see "src/test/kotlin/ClassNameTest.kt"
 * */
public infix fun String.toBoxedTypeWith(generatable: GeneratableDelegate): KdddType.Boxed =
    BoxedWithPrimitive.PrimitiveClassName.entries.find { it.name == this@toBoxedTypeWith.uppercase() }
        ?.let { BoxedWithPrimitive(generatable, it) }
        ?: BoxedWithCommon(generatable, BoxedWithCommon.CommonClassName(this@toBoxedTypeWith), KDParsable())
