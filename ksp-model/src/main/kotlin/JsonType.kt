package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

internal sealed interface JsonType {

    val typeName: TypeName

    fun asString(): String
    fun encodePrimitiveElement(): String
    fun decodePrimitiveElement(): String

    data class Element private constructor(
        override val typeName: TypeName,
        val kdType: KDType,
        val isInner: Boolean
    ): JsonType {

        override fun asString(): String =
            "String?".takeIf { typeName.isNullable } ?: "String"

        override fun encodePrimitiveElement(): String =
            "encode${if (kdType is KDType.Boxed && kdType.isPrimitive) kdType.asSimplePrimitive() else "String"}Element"

        override fun decodePrimitiveElement(): String =
            "decode${if (kdType is KDType.Boxed && kdType.isPrimitive) kdType.asSimplePrimitive() else "String"}Element"

        companion object {
            fun create(kdTypeSearchResult: KDTypeSearchResult, isNullable: Boolean): Element =
                Element(
                    (if (kdTypeSearchResult.first is KDType.Boxed) (kdTypeSearchResult.first as KDType.Boxed).rawTypeName
                    else kdTypeSearchResult.first.sourceTypeName).toNullable(isNullable),
                    kdTypeSearchResult.first,
                    kdTypeSearchResult.second
                )
        }
    }

    data class Collection(
        val parameterizedTypeName: ParameterizedTypeName
    ) : JsonType {

        private val type = parameterizedTypeName.toCollectionType()
        private val args = mutableListOf<JsonType>()
        var serializerTemplate = if (type == CollectionType.MAP) "%M(${TMPL_SIGN}0, ${TMPL_SIGN}1)" else "%M($TMPL_SIGN)"
            private set
        // for addStatement("", *varargs)
        private val _serializerVarargs = mutableListOf<Any>(
            MemberName("kotlinx.serialization.builtins", "${type.originName}Serializer")
        )
        val serializerVarargs: List<Any>
            get() = _serializerVarargs.toList()

        private val serializationMappers = mutableListOf<String>()
        lateinit var serializationMapper: String
            private set

        private val deserializationMappers = mutableListOf<String>()
        lateinit var deserializationMapper: String
            private set

        override val typeName: TypeName = parameterizedTypeName

        fun addArg(i: Int, node: JsonType) {
            // for descriptor
            args += node
            // for serializer
            serializerTemplate = if (type == CollectionType.MAP) serializerTemplate.replace("${TMPL_SIGN}$i", serializerReplacement(node))
            else serializerTemplate.replace(TMPL_SIGN, serializerReplacement(node))
            val localIt = type.getItArgName(i)
            // .map { it.map { it.boxed }.toSet() })
            serializationMappers += when(node) {
                is Element -> if (node.kdType is KDType.Boxed) node.kdType.asSerialize(localIt, node.typeName.isNullable) else localIt
                is Collection -> "$localIt${node.serializationMapper}"
            }
            // .map { it.map { it.let(NameImpl::create) }.toSet() }
            // for deserializer
            deserializationMappers += when(node) {
                is Element ->
                    if (node.kdType is KDType.Boxed) "${localIt}${"?".takeIf { node.typeName.isNullable } ?: ""}${node.kdType.asDeserialize(node.isInner)}" else "XXXImpl.serializer()"
                is Collection -> "$localIt${node.deserializationMapper}"
            }
        }

        fun finish() {
            val hasNotContainsBoxed = args.all { it is Element && it.kdType !is KDType.Boxed }
            serializationMapper = serializationMappers.toList().createMapper(type, false, hasNotContainsBoxed)
            deserializationMapper = deserializationMappers.toList().createMapper(type, false, hasNotContainsBoxed)
        }

        private fun serializerReplacement(node: JsonType): String = when(node) {
            is Element -> {
                _serializerVarargs += MemberName("kotlinx.serialization.builtins", "serializer")
                "String.%M().%M".takeIf { node.typeName.isNullable }
                    ?.also { _serializerVarargs += MemberName("kotlinx.serialization.builtins", "nullable") }
                    ?: "String.%M()"
            }
            is Collection -> node.serializerTemplate.also { _serializerVarargs.addAll(node.serializerVarargs) }
        }

        override fun encodePrimitiveElement(): String =
            "encodeSerializableElement"

        override fun decodePrimitiveElement(): String =
            "decodeSerializableElement"

        override fun asString(): String = when(val type = parameterizedTypeName.toCollectionType()) {
            CollectionType.MAP -> "${type.originName}<${args[0].asString()}, ${args[1].asString()}>"
            else -> "${type.originName}<${args.first().asString()}>"
        }

        companion object {
            private const val TMPL_SIGN = "#"
            fun create(typeName: ParameterizedTypeName) = Collection(typeName)
        }
    }
}
