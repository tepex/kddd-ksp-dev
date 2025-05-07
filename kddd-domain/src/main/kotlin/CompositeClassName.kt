package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

public data class CompositeClassName(
    public val packageName: PackageName,
    public val className: ClassName
) : ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    @JvmInline
    public value class PackageName private constructor(override val boxed: String) : ValueObject.Boxed<String> {

        override fun validate() {}

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun toString(): String =
            boxed

        public companion object {
            public operator fun invoke(value: String): PackageName =
                PackageName(value)
        }
    }

    @JvmInline
    public value class ClassName private constructor(override val boxed: String) : ValueObject.Boxed<String> {

        override fun validate() {}

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun toString(): String =
            boxed

        public companion object {
            public operator fun invoke(value: String): ClassName =
                ClassName(value)
        }
    }

    @JvmInline
    public value class FullClassName private constructor(override val boxed: String) : ValueObject.Boxed<String> {
        override fun validate() {}

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun toString(): String = boxed

        public companion object {
            public operator fun invoke(value: String): FullClassName =
                FullClassName(value)
        }
    }

    public class Builder {
        public var packageName: String? = null
        public var className: String? = null

        public fun build(): CompositeClassName {
            checkNotNull(packageName) { "Property 'packageName' must be initialized!" }
            checkNotNull(className) { "Property 'className' must be initialized!" }
            return CompositeClassName(PackageName(packageName!!), ClassName(className!!))
        }
    }

    public companion object {
        public operator fun invoke(fullClassName: String, packageName: String): CompositeClassName =
            CompositeClassName(
                PackageName(packageName),
                ClassName(fullClassName.substringAfterLast("$packageName."))
            )
    }
}
