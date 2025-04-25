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
@JvmInline
public value class FullClassNameBuilder private constructor(override val boxed: String): ValueObject.Boxed<String> {
    init {
        validate()
    }

    override fun validate() {}

    override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
        TODO("Not yet implemented")

    override fun toString(): String =
        boxed

    public operator fun plus(className: ClassName): FullClassNameBuilder =
        FullClassNameBuilder("$boxed.${className.simpleName}")

    public companion object {
        public operator fun invoke(basePackageName: PackageName): FullClassNameBuilder =
            FullClassNameBuilder(basePackageName.boxed)
    }
}
