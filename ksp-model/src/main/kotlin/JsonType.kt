package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

internal sealed interface JsonType {

    fun asString(): String

    data class Element private constructor(
        val typeName: TypeName,
        val kdType: KDType
    ): JsonType {

        override fun asString(): String =
            if (typeName.isNullable) "String?" else "String"

        companion object {
            fun create(kdTypeSearchResult: KDTypeSearchResult, isNullable: Boolean): Element =
                Element(
                    (if (kdTypeSearchResult.first is KDType.Boxed) (kdTypeSearchResult.first as KDType.Boxed).rawTypeName
                    else kdTypeSearchResult.first.sourceTypeName).toNullable(isNullable),
                    kdTypeSearchResult.first
                )
        }
    }

    data class Collection(
        val parameterizedTypeName: ParameterizedTypeName
    ) : JsonType {

        private val args = mutableListOf<JsonType>()

        fun addForDescriptor(node: JsonType) {
            args += node
        }

        override fun asString(): String = when(val type = parameterizedTypeName.toCollectionType()) {
            CollectionType.MAP ->
                "${type.originName}<${args[0].asString()}, ${args[1].asString()}>"
            else -> "${type.originName}<${args.first().asString()}>"
        }

        companion object {
            fun create(typeName: ParameterizedTypeName) = Collection(typeName)
        }
    }
}
