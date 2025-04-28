package ru.it_arch.clean_ddd.ksp_model

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
import ru.it_arch.clean_ddd.ksp_model.model.KDType
import ru.it_arch.clean_ddd.ksp_model.utils.KDLogger

internal sealed interface DSLType {
    val kddd: TypeName

    @ConsistentCopyVisibility
    data class Element private constructor(
        override val kddd: TypeName,
        val kdType: KDType,
        val isInner: Boolean
    ) : DSLType {

        companion object {
            operator fun invoke(kdTypeSearchResult: KDTypeSearchResult, isNullable: Boolean) =
                Element(
                    (if (kdTypeSearchResult.first is KDType.Boxed) (kdTypeSearchResult.first as KDType.Boxed).rawType
                    else kdTypeSearchResult.first.kddd).toNullable(isNullable),
                    kdTypeSearchResult.first,
                    kdTypeSearchResult.second,
                )
        }
    }

    @ConsistentCopyVisibility
    data class Collection private constructor(
        val parameterizedName: ParameterizedTypeName,
        val logger: KDLogger
    ) : DSLType {

        private lateinit var args: List<DSLType>
        private var isSubstituted: Boolean = false

        private val collectionType = parameterizedName.toCollectionType()

        lateinit var fromDslMapper: String
        lateinit var toDslMapper: String

        override val kddd = parameterizedName

        fun substituteOrNull(): Collection? =
            if (isSubstituted) substitute().also { isSubstituted = true } else null

        fun substituteArgs(transform: (TypeName) -> DSLType) {
            val fromDslArgs = mutableListOf<String>()
            val toDslArgs = mutableListOf<String>()
            args = parameterizedName.typeArguments.mapIndexed { i, arg ->
                transform(arg).also { newArg ->
                    val localIt = collectionType getItArgNameForIndex i
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
                                fromDslArgs += localIt.boxed
                                toDslArgs += localIt.boxed
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
            parameterizedName.rawType.toMutableCollection()
                .let { newType -> copy(parameterizedName = newType).also {
                    it.fromDslMapper = fromDslMapper
                    it.toDslMapper = toDslMapper
                } }

        private fun ClassName.toMutableCollection() = when(this) {
            LIST -> MUTABLE_LIST
            SET -> MUTABLE_SET
            MAP -> MUTABLE_MAP
            else -> error("Unsupported collection for mutable: $this")
        }.parameterizedBy(args.map { it.kddd })

        companion object {
            operator fun invoke(name: ParameterizedTypeName, logger: KDLogger): Collection =
                Collection(name, logger)
        }
    }
}
