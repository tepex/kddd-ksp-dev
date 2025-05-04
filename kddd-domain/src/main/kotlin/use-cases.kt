package ru.it_arch.clean_ddd.domain

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
 * Создание класса имплементации из имени класса [Kddd]-типа.
 *
 * @receiver исходное полное имя класса.
 * @return имя класса имплементации.
 * */
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
 * Преобразование параметризированного типа `BOXED` из [ValueObject.Boxed<BOXED>] в [KdddType.Boxed] с соотвествующим
 * типом `boxed`: [BoxedWithPrimitive.PrimitiveClassName] или [BoxedWithCommon.CommonClassName].
 *
 * @param необходимый делегат [KdddType.Generatable].
 * @receiver имя параметризированного типа.
 * @return созданный [KdddType.Boxed].
 * @see "src/test/kotlin/ClassNameTest.kt"
 * */
public infix fun String.toBoxedTypeWith(generatable: GeneratableDelegate): KdddType.Boxed =
    BoxedWithPrimitive.PrimitiveClassName.entries.find { it.name == uppercase() }
        ?.let { BoxedWithPrimitive(generatable, it) }
        ?: BoxedWithCommon(generatable, BoxedWithCommon.CommonClassName(this), KDParsable())
