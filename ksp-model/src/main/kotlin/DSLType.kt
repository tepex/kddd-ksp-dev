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

    data class Element private constructor(
        override val typeName: TypeName,
        val kdType: KDType,
        val isInner: Boolean
    ) : DSLType {

        companion object {
            operator fun invoke(kdTypeSearchResult: KDTypeSearchResult, isNullable: Boolean): Element =
                Element(
                    (if (kdTypeSearchResult.first is KDType.Boxed) (kdTypeSearchResult.first as KDType.Boxed).rawTypeName
                    else kdTypeSearchResult.first.kDddTypeName).toNullable(isNullable),
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
        private var isSubstituted: Boolean = false

        private val collectionType = parameterizedTypeName.toCollectionType()

        lateinit var fromDslMapper: String
        lateinit var toDslMapper: String

        override val typeName = parameterizedTypeName

        fun substituteOrNull(): Collection? =
            if (isSubstituted) substitute().also { isSubstituted = true } else null

        fun substituteArgs(transform: (TypeName) -> DSLType) {
            val fromDslArgs = mutableListOf<String>()
            val toDslArgs = mutableListOf<String>()
            args = parameterizedTypeName.typeArguments.mapIndexed { i, arg ->
                transform(arg).also { newArg ->
                    val localIt = collectionType.getItArgName(i)
                    when(newArg) {
                        is Collection -> {
                            fromDslArgs += "$localIt${newArg.fromDslMapper}"
                            toDslArgs += "$localIt${newArg.toDslMapper}"
                        }
                        is Element ->
                            if (newArg.kdType is KDType.Boxed) {
                                fromDslArgs += newArg.kdType.asDeserialize(localIt, arg.isNullable)
                                toDslArgs += newArg.kdType.asIsOrSerialize(localIt, arg.isNullable)
                            } else {
                                fromDslArgs += localIt
                                toDslArgs += localIt
                            }
                    }
                }
            }

            val hasNotContainsBoxed = args.all { it is Element && it.kdType !is KDType.Boxed }
            fromDslMapper = fromDslArgs.createMapper(collectionType, false, hasNotContainsBoxed)
            toDslMapper = toDslArgs.createMapper(collectionType, true, hasNotContainsBoxed)
            isSubstituted = true
        }

        private fun substitute(): Collection =
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
            operator fun invoke(typeName: ParameterizedTypeName, logger: KDLogger): Collection =
                Collection(typeName, logger)
        }
    }
}
