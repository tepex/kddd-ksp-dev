package ru.it_arch.clean_ddd.ksp_model.model

import com.squareup.kotlinpoet.ClassName
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 *
 * */
public class KDClassName private constructor(
    private val implClassNameBuilder: FullClassNameBuilder,
    public val properties: List<KDProperty>

) {

    public val className: ClassName by lazy { ClassName.bestGuess(implClassNameBuilder.toString()) }

    public companion object {
        public operator fun invoke(fullClassName: FullClassNameBuilder): KDClassName =
            KDClassName(fullClassName)
    }

    /**
     * Определяет сроковое представление полного квалифицированного имени класса.
     *
     * Необходим для обеспечения ссылки на тип из другой модели. Если он внутренний (inner), то будет содержать имя класса-контейнера
     * Пример:
     * ```
     * class Clazz {
     *    class Inner {
     *    }
     * }
     * ```
     * Результат: "<package>.Clazz.Inner"
     *
     * @property packageName имя пакета.
     * @property nested цепочка вложенных классов.
     * */
    @ConsistentCopyVisibility
    public data class FullClassNameBuilder private constructor(
        private val packageName: PackageName,
        private val nested: List<String>
    ) : ValueObject.Data {

        override fun validate() {}

        override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
            TODO("Must not used")

        override fun toString(): String =
            StringBuilder(packageName.boxed).apply {
                nested.forEach { append(".$it") }
            }.toString()

        public operator fun plus(className: ClassName): FullClassNameBuilder =
            copy(nested = nested + className.simpleName)

        public companion object {
            public operator fun invoke(basePackageName: PackageName): FullClassNameBuilder =
                FullClassNameBuilder(basePackageName, emptyList())
        }
    }
}
