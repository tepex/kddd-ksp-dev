package ru.it_arch.clean_ddd.domain

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
public data class ClassName private constructor(
    public val name: Name,
    public val packageName: PackageName,
    public val enclosing: ClassName?
) : ValueObject.Data {

    public val shortClassName: String by lazy {
        var current = enclosing
        var className = name.boxed
        while (current != null) {
            className = "${current.name}.$className"
            current = current.enclosing
        }
        className
    }

    public val fullClassName: String by lazy {
        "$packageName.$shortClassName"
    }

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
        public var enclosing: ClassName? = null

        public fun build(): ClassName {
            requireNotNull(name) { "Property 'name' must be initialized!" }
            requireNotNull(packageName) { "Property 'packageName' must be initialized!" }

            return ClassName(name!!, packageName!!, enclosing)
        }
    }

    public class DslBuilder {
        public var name: String? = null
        public var packageName: String? = null
        public var enclosing: ClassName? = null

        public fun build(): ClassName {
            requireNotNull(name) { "Property 'name' must be initialized!" }
            requireNotNull(packageName) { "Property 'packageName' must be initialized!" }

            return ClassName(Name(name!!), PackageName(packageName!!), enclosing)
        }
    }

    public companion object {
        public operator fun invoke(fullClassName: String): ClassName {

        }
    }
}
