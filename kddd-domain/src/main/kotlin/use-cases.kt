package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.Kddd

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
