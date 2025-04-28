package ru.it_arch.clean_ddd.ksp_model.model

import com.squareup.kotlinpoet.ClassName
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

/**
 * Обертка класса [ClassName].
 *
 * private val implClassNameBuilder: FullClassNameBuilder,
 *
 * TODO: ref to parent KDClassName for annotation inheritance
 * TODO: hasJson, hasDsl, here
 *
 * */
@ConsistentCopyVisibility
public data class KDClassName private constructor(
    public val name: Name,
    public val packageName: PackageName,
    public val enclosing: KDClassName?
) : ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        TODO("Not yet implemented")

    /*
    public val className: ClassName by lazy {
        ClassName.bestGuess(implClassNameBuilder.toString())
    }*/

    @JvmInline
    public value class Name(override val boxed: String) : ValueObject.Boxed<String> {
        override fun validate() {}

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
            TODO("Not yet implemented")

        override fun toString(): String = boxed
    }

    /** Тип для имени пакета. */
    @JvmInline
    public value class PackageName(override val boxed: String) : ValueObject.Boxed<String> {
        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
            TODO("Not yet implemented")

        override fun validate() {}

        override fun toString(): String = boxed
    }

    public class Builder {
        public var name: Name? = null
        public var packageName: PackageName? = null
        public var enclosing: KDClassName? = null

        public fun build(): KDClassName {
            requireNotNull(name) { "Property 'name' must be initialized!" }
            requireNotNull(packageName) { "Property 'packageName' must be initialized!" }

            return KDClassName(name!!, packageName!!, enclosing)
        }
    }

    public class DslBuilder {
        public var name: String? = null
        public var packageName: String? = null
        public var enclosing: KDClassName? = null

        public fun build(): KDClassName {
            requireNotNull(name) { "Property 'name' must be initialized!" }
            requireNotNull(packageName) { "Property 'packageName' must be initialized!" }

            return KDClassName(Name(name!!), PackageName(packageName!!), enclosing)
        }
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

        public val simpleName: String by lazy { nested.last() }

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
