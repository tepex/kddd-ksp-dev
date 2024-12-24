package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName

@ConsistentCopyVisibility
public data class KDPropertyHolder private constructor(
    val name: MemberName,
    val typeReference: KDReference
) {

    public sealed interface KDReference {
        public val typeName: TypeName

        @JvmInline
        public value class Element private constructor(override val typeName: TypeName) : KDReference {

            override fun toString(): String =
                typeName.toString()

            public companion object {
                public fun create(typeName: TypeName): Element =
                    Element(typeName)
            }
        }

        @ConsistentCopyVisibility
        public data class Collection private constructor(
            val parameterizedTypeName: ParameterizedTypeName,
            val collectionType: CollectionType
        ) : KDReference {
            override val typeName: TypeName = parameterizedTypeName

            private var args = parameterizedTypeName.typeArguments
            private val unDslArgs = mutableListOf<String>()
            private lateinit var unDslMapper: String

            private var _isSubstituted = false
            internal val isSubstituted: Boolean
                get() = _isSubstituted

            /*
                                                                                                        | init                       | arg[0]                     | arg[1]                                                | finish
              list:        List        <Name?>
                           MutableList <String?>                                                     -> | <name>.map {                 it?.let(::name)                                                                    | }.toList(),
              _set:        Set         <Name>
                           MutableSet  <String>                                                      -> | <name>.map(                  ::name                                                                              | ).toSet(),
              listInner:   List        <Inner?>
                           MutableList <Inner?>                                                      -> | <name>                                                                                                           | .toList()
                                                                                                        | init                       | arg[0]                     | arg[1]                                                 | finish
              simpleMap:   Map         <Name, Inner>
                           MutableMap  <String, Inner>                                               -> | <name>.entries.associate {   name(it.key)        to       it.value                                                    },
              simpleMap1:  Map         <Name, Name?>
                           MutableMap  <String, String?>                                             -> | <name>.entries.associate {   name(it.key)        to       it.value?.let(::name)                                       },

              nestedMap:   Map         <Name?, List<Name>>                                                                                                                          | arg[1][0]
                           MutableMap  <String?, MutableList<String>>                                -> | <name>.entries.associate {   it.key?.let(::name) to       it.value   .map { name(it) }                                },
              nestedMaps:  Map         <Map<Inner, Inner?>, List<List<Inner?>>>
                           MutableMap  <MutableMap<Inner, Inner?>, MutableList<MutableList<Inner?>>> -> | <name>.entries.associate {   it.key.toMap()      to       it.value   .map { it.toList() }                             },
              nestedMaps1: Map         <Name, Map<Name, Inner>>                                                                                                                                    | arg[1][0]       arg[1][1]
                           MutableMap  <String, MutableMap<String, Inner>>                           -> | <name>.entries.associate {   name(it.key)        to       it.value   .entries.associate { name(it.key) to it.value }  },

             */
            internal fun substitute(transform: (TypeName) -> TypeName) {
                args = args.map { arg ->
                    /*
                    if (arg is ParameterizedTypeName) {

                    } else {
                        val dslBuilderFunName = arg.dslBuilderFunName
                    }
        */
                    transform(arg)
                }

                _isSubstituted = true
            }

            internal fun terminate(): Collection =
                parameterizedTypeName.rawType.toMutableCollection().parameterizedBy(args)
                    .let { copy(parameterizedTypeName = it) }
                    .also { _isSubstituted = true }

            public enum class CollectionType(
                public val className: ClassName,
                public val initializer: String,
                public val mutableInitializer: String
            ) {
                SET(com.squareup.kotlinpoet.SET, "emptySet()", "mutableSetOf()"),
                LIST(com.squareup.kotlinpoet.LIST, "emptyList()", "mutableListOf()"),
                MAP(com.squareup.kotlinpoet.MAP, "emptyMap()", "mutableMapOf()");
            }

            public companion object {
                public fun create(typeName: ParameterizedTypeName): Collection = CollectionType.entries
                    .find { it.className == typeName.rawType }
                    ?.let { Collection(typeName, it) }
                    ?: error("Not supported collection type $typeName")

                private fun ClassName.toMutableCollection() = when (this) {
                    LIST -> MUTABLE_LIST
                    SET -> MUTABLE_SET
                    MAP -> MUTABLE_MAP
                    else -> error("Unsupported collection for mutable: $this")
                }
            }
        }

        public companion object {
            public fun create(typeName: TypeName): KDReference = when(typeName) {
                is ParameterizedTypeName -> Collection.create(typeName)
                else                     -> Element.create(typeName)
            }
        }
    }

    public companion object {
        public fun create(name: MemberName, propertyTypeName: TypeName): KDPropertyHolder =
            KDPropertyHolder(name, KDReference.create(propertyTypeName))

        public fun create(property: KDTypeHelper.Property): KDPropertyHolder =
            KDPropertyHolder(property.name, KDReference.create(property.typeName))
    }
}
