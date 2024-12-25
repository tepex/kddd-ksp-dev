package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName

public sealed interface KDReference {
    public val typeName: TypeName

    public class Element private constructor(
        override val typeName: TypeName,
        private val _kdType: KDType? = null
    ) : KDReference {

        public val kdType: KDType
            get() = _kdType ?: error("KDType is not resolved for $typeName")

        override fun toString(): String = typeName.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Element) return false
            if (typeName != other.typeName) return false
            return true
        }

        override fun hashCode(): Int = typeName.hashCode()

        public companion object {
            public fun create(typeName: TypeName): Element = Element(typeName)

            public fun create(kdType: KDType, isNullable: Boolean): Element = Element(
                (if (kdType is KDType.Boxed) kdType.rawTypeName else kdType.sourceTypeName).toNullable(isNullable),
                kdType
            )
        }
    }

    public interface Parameterized : KDReference {
        public val parameterizedTypeName: ParameterizedTypeName
        public val fromDslMapper: String
        public val toDslMapper: String

        override val typeName: TypeName
            get() = parameterizedTypeName

        public fun getLambdaArgName(i: Int): String

        public fun substituteArgs(transform: (TypeName) -> KDReference)
        public fun substituteOrNull(): Parameterized?
    }

    public abstract class AbstractParameterized() : Parameterized {
        protected lateinit var args: List<KDReference>
        // TODO: make private
        override lateinit var fromDslMapper: String
        override lateinit var toDslMapper: String

        private var isSubstituted: Boolean = false

        protected abstract fun createFromDslMapper(lambdaArgs: List<String>): String
        protected abstract fun createToDslMapper(lambdaArgs: List<String>): String
        protected abstract fun substitute(): Parameterized // copy

        override fun substituteOrNull(): Parameterized? =
            if (isSubstituted) substitute().also { isSubstituted = true } else null

        override fun substituteArgs(transform: (TypeName) -> KDReference) {
            val fromDslArgs = mutableListOf<String>()
            val toDslArgs = mutableListOf<String>()
            args = parameterizedTypeName.typeArguments.mapIndexed { i, arg ->
                transform(arg).also { newArg ->
                    val localIt = getLambdaArgName(i)
                    when(newArg) {
                        is Collection -> {
                            fromDslArgs += "$localIt${newArg.fromDslMapper}"
                            toDslArgs += "$localIt${newArg.toDslMapper}"
                        }
                        is Element -> {
                            if (newArg.kdType is KDType.Boxed) {
                                val dslBuilderFunName = (newArg.kdType as KDType.Boxed).dslBuilderFunName
                                if (arg.isNullable) {
                                    fromDslArgs += "$localIt?.let(::$dslBuilderFunName)"
                                    toDslArgs += "$localIt?.${KDType.Boxed.PARAM_NAME}"
                                } else {
                                    fromDslArgs += "$dslBuilderFunName($localIt)"
                                    toDslArgs += "$localIt.${KDType.Boxed.PARAM_NAME}"
                                }
                            } else {
                                fromDslArgs += localIt
                                toDslArgs += localIt
                            }
                        }
                        else -> error("Unsupported KDReference type: $this")
                    }
                }
            }
            fromDslMapper = createFromDslMapper(fromDslArgs)
            toDslMapper = createToDslMapper(toDslArgs)
            isSubstituted = true
        }
    }

    public data class Collection private constructor(
        override val parameterizedTypeName: ParameterizedTypeName,
        val collectionType: CollectionType,
    ) : AbstractParameterized() {

        override fun getLambdaArgName(i: Int): String = when(collectionType) {
            CollectionType.MAP -> "it.key".takeIf { i == 0 } ?: "it.value"
            else               -> "it"
        }

        override fun createFromDslMapper(lambdaArgs: List<String>): String = when(collectionType) {
            CollectionType.MAP -> ".entries.associate { ${lambdaArgs[0]} to ${lambdaArgs[1]} }"
            else -> StringBuilder().apply {
                val arg = args.first()
                ".map { ${lambdaArgs.first()} }".takeUnless { arg is Element && arg.kdType !is KDType.Boxed }?.also(::append)
                append(".toList()".takeIf { collectionType == CollectionType.LIST } ?: ".toSet()")
            }.toString()
        }

        override fun createToDslMapper(lambdaArgs: List<String>): String = when(collectionType) {
            CollectionType.MAP -> ".entries.associate { ${lambdaArgs[0]} to ${lambdaArgs[1]} }"
            else -> StringBuilder().apply {
                val arg = args.first()
                ".map { ${lambdaArgs.first()} }".takeUnless { arg is Element && arg.kdType !is KDType.Boxed }?.also(::append)
                append(".toMutableList()".takeIf { collectionType == CollectionType.LIST } ?: ".toMutableSet()")
            }.toString()
        }

        /*
                                                                                          | init                       | arg[0]                     | arg[1]                                                | finish
list:        List        <Name?>
             MutableList <String?>                                                     -> | <name>.map {                 it?.let(::name)                                                                    | }.toList(),
                                                                                       -> | <name>.map {                 it?.boxed                                                                          | }.toMutableList()
_set:        Set         <Name>
             MutableSet  <String>                                                      -> | <name>.map {                 name(it)                                                                           | }.toSet(),
                                                                                       -> | <name>.map {                 it.boxed                                                                           | }.toMutableSet()
listInner:   List        <Inner?>
             MutableList <Inner?>                                                      -> | <name>                       empty                                                                              | .toList()
                                                                                       -> | <name>                       empty                                                                              | .toMutableList()
nestedList:  Set         <List<Inner?>>
             MutableSet  <MutableList<Inner?>>                                         -> | <name>.map {                 it                      .toList()                                                  | }.toSet(),
                                                                                       -> | <name>.map {                 it                      .toMutableList()                                           | }.toMutableSet()
nestedList1: Set         <List<Name>>
             MutableSet  <MutableList<String>>                                         -> | <name>.map {                 it     .map { name(it) }.toList()                                                  | }.toSet(),
                                                                                       -> | <name>.map {                 it     .map { it.boxed }.toMutableList()                                           | }.toMutableSet()
                                                                                          | init                       | arg[0]                     | arg[1]                                                     | finish
simpleMap:   Map         <Name, Inner>
             MutableMap  <String, Inner>                                               -> | <name>.entries.associate {   name(it.key)          to       it.value                                                    },
                                                                                       -> | <name>.entries.associate {   it.key.boxed          to       it.value                                                    }.toMutableMap()
simpleMap1:  Map         <Name, Name?>
             MutableMap  <String, String?>                                             -> | <name>.entries.associate {   name(it.key)          to       it.value?.let(::name)                                                      },
                                                                                       -> | <name>.entries.associate {   it.key.boxed          to       it.value?.boxed                                                            }.toMutableMap()
nestedMap:   Map         <Name?, List<Name>>                                                                                                                          | arg[1][0]
             MutableMap  <String?, MutableList<String>>                                -> | <name>.entries.associate {   it.key?.let(::name)   to       it.value   .map { name(it) }                                               },
                                                                                       -> | <name>.entries.associate {   it.key?.boxed         to       it.value   .map { it.boxed }.toMutableList()                               }.toMutableMap()
nestedMaps:  Map         <Map<Inner, Inner?>, List<List<Inner?>>>
             MutableMap  <MutableMap<Inner, Inner?>, MutableList<MutableList<Inner?>>> -> | <name>.entries.associate {   it.key.toMap()        to       it.value   .map { it.toList() }                                            },
                                                                                       -> | <name>.entries.associate {   it.key.toMutableMap() to       it.value   .map { it.toMutableList() }.toMutableList()                     }.toMutableMap()
nestedMaps1: Map         <Name, Map<Name, Inner>>                                                                                                                                    | arg[1][0]       arg[1][1]
             MutableMap  <String, MutableMap<String, Inner>>                           -> | <name>.entries.associate {   name(it.key)          to       it.value   .entries.associate { name(it.key) to it.value }                 },
                                                                                       -> | <name>.entries.associate {   it.key.boxed          to       it.value   .entries.associate { it.key.boxed to it.value }.toMutableMap()  }.toMutableMap()

    ret.list = list.map { it?.boxed }.toMutableList()
    ret._set = _set.map { it.boxed }.toMutableSet()
    ret.listInner = listInner.toMutableList()

    ret.nestedList = nestedList.map { it.toMutableList() }.toMutableSet()
    ret.nestedList1 = nestedList1.map { it.map { it.boxed }.toMutableList() }.toMutableSet()

    ret.simpleMap = simpleMap.entries.associate { it.key.boxed to it.value }.toMutableMap()
    ret.simpleMap1 = simpleMap1.entries.associate { it.key.boxed to it.value?.boxed }.toMutableMap()
    ret.nestedMap = nestedMap.entries.associate { it.key?.boxed to it.value.map { it.boxed }.toMutableList() }.toMutableMap()
    ret.nestedMaps = nestedMaps.entries.associate { it.key.toMutableMap() to it.value.map { it.toMutableList() }.toMutableList() }.toMutableMap()
    ret.nestedMaps1 = nestedMaps1.entries.associate { it.key.boxed to it.value.entries.associate { it.key.boxed to it.value }.toMutableMap() }.toMutableMap()

         */

        override fun substitute(): Parameterized =
            parameterizedTypeName.rawType.toMutableCollection()
                .let { newType -> copy(parameterizedTypeName = newType).also {
                    it.fromDslMapper = fromDslMapper
                    it.toDslMapper = toDslMapper
                } }

        private fun ClassName.toMutableCollection() = when(this) {
            LIST -> MUTABLE_LIST
            SET -> MUTABLE_SET
            MAP -> MUTABLE_MAP
            else -> error("Unsupported collection for mutable: $this")
        }.parameterizedBy(args.map { it.typeName })

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
        }
    }

    public companion object {
        public fun create(typeName: TypeName): KDReference = when(typeName) {
            is ParameterizedTypeName -> Collection.create(typeName)
            else                     -> Element.create(typeName)
        }
    }
}
