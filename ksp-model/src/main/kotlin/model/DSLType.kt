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
import java.lang.System.Logger

internal sealed interface DSLType {
    val typeName: TypeName

    class Element private constructor(
        override val typeName: TypeName,
        val kdType: KDType
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
            fun create(kdType: KDType, isNullable: Boolean): Element = Element(
                (if (kdType is KDType.Boxed) kdType.rawTypeName else kdType.sourceTypeName).toNullable(isNullable),
                kdType
            )
        }
    }

    interface Parameterized : DSLType {
        val parameterizedTypeName: ParameterizedTypeName
        val fromDslMapper: String
        val toDslMapper: String

        override val typeName: TypeName
            get() = parameterizedTypeName

        fun getLambdaArgName(i: Int): String

        fun substituteArgs(transform: (TypeName) -> DSLType)
        fun substituteOrNull(): Parameterized?
    }

    abstract class AbstractParameterized : Parameterized {
        protected lateinit var args: List<DSLType>
        // TODO: make private
        override lateinit var fromDslMapper: String
        override lateinit var toDslMapper: String

        private var isSubstituted: Boolean = false

        protected abstract fun createFromDslToNormalTypeMapper(lambdaArgs: List<String>): String
        protected abstract fun createFromNormalTypeToDslMapper(lambdaArgs: List<String>): String
        protected abstract fun substitute(): Parameterized // copy

        override fun substituteOrNull(): Parameterized? =
            if (isSubstituted) substitute().also { isSubstituted = true } else null

        override fun substituteArgs(transform: (TypeName) -> DSLType) {
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
                                fromDslArgs += (newArg.kdType as KDType.Boxed).fromString(localIt, arg.isNullable)
                                toDslArgs += (newArg.kdType as KDType.Boxed).asString(localIt, arg.isNullable)
                            } else {
                                fromDslArgs += localIt
                                toDslArgs += localIt
                            }
                        }
                        else -> error("Unsupported KDReference type: $this")
                    }
                }
            }
            fromDslMapper = createFromDslToNormalTypeMapper(fromDslArgs)
            toDslMapper = createFromNormalTypeToDslMapper(toDslArgs)
            isSubstituted = true
        }
    }

    data class Collection private constructor(
        override val parameterizedTypeName: ParameterizedTypeName,
        val logger: KDLogger,
        val testFlag: Boolean
    ) : AbstractParameterized() {

        private val collectionType = parameterizedTypeName.toCollectionType()

        override fun getLambdaArgName(i: Int): String =
            collectionType.getLambdaArgName(i)

        override fun createFromDslToNormalTypeMapper(lambdaArgs: List<String>): String =
            createDslMapper(lambdaArgs, false)

        override fun createFromNormalTypeToDslMapper(lambdaArgs: List<String>): String =
            createDslMapper(lambdaArgs, true)

        private fun createDslMapper(lambdaArgs: List<String>, isMutable: Boolean): String {
            val isNoMap = args.all { it is Element && it.kdType !is KDType.Boxed }
            val isNoTerminal = if (isNoMap) false
            else if (collectionType == CollectionType.MAP) true else collectionType != CollectionType.SET
            return collectionType.mapperAsString(lambdaArgs, isMutable, isNoMap, isNoTerminal)
        }

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

        companion object {
            fun create(typeName: ParameterizedTypeName, logger: KDLogger, testFlag: Boolean): Collection =
                Collection(typeName, logger, testFlag)
        }
    }
}
