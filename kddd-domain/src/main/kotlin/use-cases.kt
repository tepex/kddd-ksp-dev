package ru.it_arch.clean_ddd.domain

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
public fun Options.toImplementationClassName(kDddClassName: String): String {
    var result = generatedClassNameResult.boxed
    generatedClassNameRe.find(kDddClassName)?.groupValues?.forEachIndexed { i, group ->
        group.takeIf { i > 0 }?.also { result = result.replace("\$$i", it) }
    }
    return result
}

/**
 *
 * */
//context(_: PackageName)
public fun String.toKddClassName(): ClassName {
    val classNames = split('.')
    var parent: ClassName? = null
    classNames.forEach { className ->
        parent = className {
            name = ClassName.Name.KdddType(className)
            enclosing = parent
        }
    }
    return parent!!
}

/**
 *
 * */
public fun String.toBoxedClassName(): ClassName = className {
    name = ClassName.Name.Primitive.entries.find { it.name == this@toBoxedClassName.uppercase() }
        ?: ClassName.Name.Common(this@toBoxedClassName)
}
