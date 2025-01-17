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

internal sealed interface DSLType {
    val typeName: TypeName

    class Element private constructor(
        override val typeName: TypeName,
        val kdType: KDType,
        val isInner: Boolean
    ) : DSLType {

        override fun toString(): String = typeName.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Element) return false
            if (typeName != other.typeName) return false
            return true
        }

        override fun hashCode(): Int = typeName.hashCode()

        companion object {
            fun create(kdTypeSearchResult: KDTypeSearchResult, typeName: TypeName): Element =
                Element(
                    (if (kdTypeSearchResult.first is KDType.Boxed) (kdTypeSearchResult.first as KDType.Boxed).rawTypeName
                    else kdTypeSearchResult.first.sourceTypeName).toNullable(typeName.isNullable),
                    kdTypeSearchResult.first,
                    kdTypeSearchResult.second,
                )
        }
    }

    data class Collection private constructor(
        val parameterizedTypeName: ParameterizedTypeName,
        val logger: KDLogger
    ) : DSLType {

        private lateinit var args: List<DSLType>
        private val collectionType = parameterizedTypeName.toCollectionType()
        private var isSubstituted: Boolean = false

        lateinit var fromDslMapper: String
        lateinit var toDslMapper: String

        override val typeName: TypeName
            get() = parameterizedTypeName

        fun getLambdaArgName(i: Int): String =
            collectionType.getLambdaArgName(i)

        fun createFromDslToNormalTypeMapper(lambdaArgs: List<String>): String =
            createDslMapper(lambdaArgs, false)

        fun createFromNormalTypeToDslMapper(lambdaArgs: List<String>): String =
            createDslMapper(lambdaArgs, true)

        fun substituteOrNull(): Collection? =
            if (isSubstituted) substitute().also { isSubstituted = true } else null

        fun substituteArgs(transform: (TypeName) -> DSLType) {
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
                        is Element ->
                            if (newArg.kdType is KDType.Boxed) {
                                fromDslArgs += newArg.kdType.fromString(localIt, newArg.isInner, arg.isNullable)
                                toDslArgs += newArg.kdType.asString(localIt, arg.isNullable)
                            } else {
                                fromDslArgs += localIt
                                toDslArgs += localIt
                            }
                    }
                }
            }
            fromDslMapper = createFromDslToNormalTypeMapper(fromDslArgs)
            toDslMapper = createFromNormalTypeToDslMapper(toDslArgs)
            isSubstituted = true
        }

        private fun createDslMapper(lambdaArgs: List<String>, isMutable: Boolean): String {
            val isNoMap = args.all { it is Element && it.kdType !is KDType.Boxed }
            val isNoTerminal = if (isNoMap) false
            else if (collectionType == CollectionType.MAP) true else collectionType != CollectionType.SET
            return collectionType.mapperAsString(lambdaArgs, isMutable, isNoMap, isNoTerminal)
        }

        fun substitute(): Collection =
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

        companion object {
            fun create(typeName: ParameterizedTypeName, logger: KDLogger): Collection =
                Collection(typeName, logger)
        }
    }
}
