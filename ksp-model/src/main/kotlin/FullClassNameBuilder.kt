package ru.it_arch.clean_ddd.ksp_model

import com.squareup.kotlinpoet.ClassName
import ru.it_arch.clean_ddd.ksp_model.model.PackageName
import ru.it_arch.kddd.ValueObject

/**
 * Определяет сроковое представление полного квалифицированного имени класса.
 *
 * Необходим для обеспечения ссылки на тип из другой модели. Если он внутренний (inner), то будет содержать имя класса-контейнера
 *         Пример:
 *         ```
 *         class Clazz {
 *              class Inner {
 *              }
 *         }
 *         ```
 *         Результат: "<package>.Clazz.Inner"
 *
 * */
public class FullClassNameBuilder private constructor(
    public val boxed: String
) {

    //public val nested = mutableListOf<String>()

    override fun toString(): String =
        boxed

    /*
    public operator fun plus(className: ClassName): FullClassNameBuilder {
        boxed = "$boxed.${className.simpleName}"
        return this
    }*/

    public operator fun plus(className: ClassName): FullClassNameBuilder =
        FullClassNameBuilder("$boxed.${className.simpleName}")

    public companion object {
        public operator fun invoke(basePackageName: PackageName): FullClassNameBuilder =
            FullClassNameBuilder(basePackageName.boxed)
    }
}
