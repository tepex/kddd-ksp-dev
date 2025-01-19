package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

internal sealed interface JsonType {

    val typeName: TypeName

    fun asString(): String
    fun encodeXElement(): String

    data class Element private constructor(
        override val typeName: TypeName,
        val kdType: KDType
    ): JsonType {

        override fun asString(): String =
            if (typeName.isNullable) "String?" else "String"

        override fun encodeXElement(): String =
            "encode${if (kdType is KDType.Boxed) kdType.rawTypeName.toString().substringAfterLast('.') else "String"}Element"

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

        private val type = parameterizedTypeName.toCollectionType()
        private val args = mutableListOf<JsonType>()
        private var _serializerTemplate = if (type == CollectionType.MAP) "%M(${TMPL_SIGN}0, ${TMPL_SIGN}1)" else "%M($TMPL_SIGN)"
        val serializerTemplate: String
            get() = _serializerTemplate
        // for addStatement("", *varargs)
        private val _serializerVarargs = mutableListOf<Any>(
            MemberName("kotlinx.serialization.builtins", "${type.originName}Serializer")
        )
        val serializerVarargs: List<Any>
            get() = _serializerVarargs.toList()

        private val _serializationMappers = mutableListOf<String>()
        private lateinit var _serializationMapper: String
        val serializationMapper: String
            get() = _serializationMapper

        override val typeName: TypeName = parameterizedTypeName

        fun addArg(i: Int, node: JsonType) {
            // for descriptor
            args += node
            // for serializer
            _serializerTemplate = if (type == CollectionType.MAP) _serializerTemplate.replace("${TMPL_SIGN}$i", serializerReplacement(node))
            else _serializerTemplate.replace(TMPL_SIGN, serializerReplacement(node))
            val localIt = type.getItArgName(i)
            _serializationMappers += when(node) {
                is Element -> if (node.kdType is KDType.Boxed) node.kdType.asSerialize(localIt, node.typeName.isNullable) else localIt
                is Collection -> "$localIt${node.serializationMapper}"
            }
            // for deserializer
        }

        fun finish() {
            val hasNotContainsBoxed = args.all { it is Element && it.kdType !is KDType.Boxed }
            _serializationMapper = _serializationMappers.toList().createMapper(type, false, hasNotContainsBoxed)
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

        override fun encodeXElement(): String =
            "encodeSerializableElement"

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
