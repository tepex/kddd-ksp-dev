package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public class KDTypeJsonBuilder private constructor(
    private val holder: KDType.Model,
    private val logger: KDLogger
) {

    private val companionBuilder = TypeSpec.companionObjectBuilder().addSuperinterface(KSerializer::class.asTypeName().parameterizedBy(holder.className))

    private val descriptorFun = DescriptorFun(holder.className)
    private val serializeFun = SerializeFun(holder.className)
    private val deserializeFun = DeserializeFun(holder.className)

    init {
        holder.propertyHolders.forEachIndexed { index, property ->
            if (property.typeName is ParameterizedTypeName) {
                processCollection(property.name, JsonType.Collection.create(property.typeName), false).also { jsonType ->
                    descriptorFun.addCollection(property.serialName, jsonType)
                    serializeFun.addElement(property.serialName, jsonType)
                    deserializeFun.addElement(property.serialName, jsonType)
                }
            } else {
                val result = holder.getKDType(property.typeName)
                if (result.first is KDType.Generatable) {
                    descriptorFun.addElement(property.serialName, result.first as KDType.Generatable, property.typeName.isNullable)
                    serializeFun.addElement(property.name.simpleName, JsonType.Element.create(result, property.typeName.isNullable))
                    deserializeFun.addElement(property.name.simpleName, JsonType.Element.create(result, property.typeName.isNullable))
                } else {
                    TODO()
                }
            }
        }

        val descriptor = descriptorFun.build()
        companionBuilder.addProperty(descriptor)
        companionBuilder.addFunction(serializeFun.build(descriptor))
        companionBuilder.addFunction(deserializeFun.build(descriptor))
    }

    /**
     * 1. IN: ParametrizedTypeName ->
     * 2. For `descriptorFun`: simple names: List<Set<String>> with null
     * 3. For `funSerialize`: List/Set/Map Serializer + mapper type -> String :
     *     encodeSerializableElement(descriptor, 9, ListSerializer(SetSerializer(String.serializer())),
     *                     value.nestedList1.map { it.map { it.boxed }.toSet() })
     * 4. For `funDeserialize`:
     * */
    // Создаем свою тяжелую иерархию для разных кейсов
    private tailrec fun processCollection(name: MemberName, collection: JsonType.Collection, isFinish: Boolean): JsonType {
        return if (isFinish) {
            // закрываем скобочки?
            collection
        }
        else {
            collection.parameterizedTypeName.typeArguments.forEachIndexed { index, arg ->
                @Suppress("NON_TAIL_RECURSIVE_CALL")
                (if (arg is ParameterizedTypeName) processCollection(name, JsonType.Collection.create(arg), false)
                else holder.getKDType(arg).let { JsonType.Element.create(it, arg.isNullable) })
                    .also { collection.addArg(index, it) }
            }
            collection.finish()
            processCollection(name, collection, true)
        }
    }

    public fun build(): TypeSpec {
        return companionBuilder.build()
    }

    private class DescriptorFun(
        className: ClassName
    ) {
        val descriptorProperty = PropertySpec.builder("descriptor", SerialDescriptor::class).addModifiers(KModifier.OVERRIDE)
        val sb = StringBuilder("%M(%T::class.java.name) {«⇥\n")
        private val args = mutableListOf(MemberName("kotlinx.serialization.descriptors", "buildClassSerialDescriptor"), className)

        fun addElement(name: String, kdType: KDType.Generatable, isNullable: Boolean) {
            // element<String?>("uuid", isOptional = true)
            // element<InnerImpl>("inner")
            sb.append("%M<%T")
            if (isNullable) sb.append('?')
            sb.append(">(\"$name\"")
            if (isNullable) sb.append(", isOptional = true")
            sb.append(")\n")
            args += MemberName("kotlinx.serialization.descriptors", "element")
            val param = if (kdType is KDType.Boxed) {
                if (kdType.isPrimitive) kdType.boxedType
                else if (kdType.isParsable) STRING
                else error("Property `$name` type must have parameterized type primitive or parsable")
            } else kdType.className
            args += param
        }

        fun addCollection(name: String, jsonType: JsonType) {
            sb.append("%M<${jsonType.asString()}>")
            sb.append("(\"$name\")\n")
            args += MemberName("kotlinx.serialization.descriptors", "element")
        }

        fun build() =
            descriptorProperty
                .initializer("$sb»}", *args.toTypedArray())
                .build()
    }

    private class SerializeFun(
        private val className: ClassName,
    ) {
        private val funSerialise = FunSpec.builder("serialize")
            .addModifiers(KModifier.OVERRIDE)

        private val elements = mutableListOf<Pair<String, JsonType>>()

        fun addElement(propertyName: String, type: JsonType) {
            elements += propertyName to type
        }

        fun build(descriptor: PropertySpec): FunSpec {
            val encoderParam = ParameterSpec.builder("encoder", Encoder::class).build()
            val valueParam = ParameterSpec.builder("value", className).build()
            val encoderStructure = MemberName("kotlinx.serialization.encoding", "encodeStructure")
            return funSerialise.addParameter(encoderParam).apply {
                addParameter(valueParam)
                addStatement("%N.%M(%N) {⇥", encoderParam, encoderStructure, descriptor)
                elements.forEachIndexed { i, el -> when(val jsonType = el.second) {
                    is JsonType.Element -> {
                        if (jsonType.typeName.isNullable)
                            addStatement("encodeNullableSerializableElement(%N, $i, String.%M(), %N.${el.first}?.${KDType.Boxed.PARAM_NAME}?.toString())",
                                descriptor,
                                MemberName("kotlinx.serialization.builtins", "serializer"),
                                valueParam
                            )
                        else if (jsonType.kdType is KDType.Boxed) {
                            // неявно полагается, что тип `Parsable`
                            addStatement("${jsonType.encodePrimitiveElement()}(%N, $i, %N.${jsonType.kdType.asIsOrSerialize(el.first, false)})",
                                descriptor,
                                valueParam
                            )
                        } else { /*addStatement("TODO()")*/ }
                    }
                    is JsonType.Collection ->
                        addStatement("encodeSerializableElement(%N, $i, ${jsonType.serializerTemplate}, %N.${el.first}${jsonType.serializationMapper})",
                            *(listOf<Any>(descriptor) + jsonType.serializerVarargs + valueParam).toTypedArray()
                        )
                } }
                addStatement("}")
            }.build()
        }
    }

    private class DeserializeFun(
        className: ClassName,
    ) {

        private val elements = mutableListOf<Pair<String, JsonType>>()

        private val funDeserialize = FunSpec.builder("deserialize")
            .addModifiers(KModifier.OVERRIDE)
            .returns(className)

        fun addElement(propertyName: String, type: JsonType) {
            elements += propertyName to type
        }

        fun build(descriptor: PropertySpec): FunSpec {
            val decoderParam = ParameterSpec.builder("decoder", Decoder::class).build()
            val decoderStructure = MemberName("kotlinx.serialization.encoding", "decodeStructure")
            return funDeserialize.addParameter(decoderParam).apply {
                addStatement("val ret = %N.%M(%N) {⇥", decoderParam, decoderStructure, descriptor)
                addStatement("val builder = Builder()")
                addStatement("loop@ while (true) {⇥")
                addStatement("when (val i = decodeElementIndex(%N)) {⇥", descriptor)
                elements.forEachIndexed { index, el ->
                    val prefix = "$index -> builder.${el.first} ="
                    when(val jsonType = el.second) {
                        is JsonType.Element -> {
                            if (jsonType.typeName.isNullable) {
                                if (jsonType.kdType is KDType.Boxed) {
                                    var addNullable = true
                                    ("$prefix decodeNullableSerializableElement(%N, $index, String.%M().nullable)?${jsonType.kdType.asDeserialize(jsonType.isInner)}"
                                        .takeUnless { jsonType.kdType.isPrimitive }
                                        ?: run {
                                            val nullableProperty = if (jsonType.kdType.isString) {
                                                ".%M"
                                            } else {
                                                addNullable = false
                                                ""
                                            }
                                            "$prefix decodeNullableSerializableElement(%N, $index, ${jsonType.kdType.asSimplePrimitive()}.%M()$nullableProperty)?${jsonType.kdType.asDeserialize(jsonType.isInner)}"
                                        }
                                    ).also {
                                        // TODO: refactor copy-paste
                                        if (addNullable) {
                                            addStatement(
                                                it,
                                                descriptor,
                                                MemberName("kotlinx.serialization.builtins", "serializer"),
                                                MemberName("kotlinx.serialization.builtins", "nullable")
                                            )
                                        } else {
                                            addStatement(
                                                it,
                                                descriptor,
                                                MemberName("kotlinx.serialization.builtins", "serializer"),
                                            )
                                        }
                                    }
                                } else { TODO() }
                            } else if (jsonType.kdType is KDType.Boxed) addStatement(
                                "$prefix ${jsonType.decodePrimitiveElement()}(%N, $index)${jsonType.kdType.asDeserialize(jsonType.isInner)}",
                                descriptor
                            )
                            else { addStatement("TODO()") }
                            //0 -> builder.nameName = decodeStringElement(descriptor, 0).let(NameImpl::create)
                        }
                        is JsonType.Collection -> {
                            /*
                            addStatement("encodeSerializableElement(%N, $i, ${jsonType.serializerTemplate}, %N.${el.first}${jsonType.serializationMapper})",
                                *(listOf<Any>(descriptor) + jsonType.serializerVarargs + valueParam).toTypedArray()
                            )*/
                            addStatement("$prefix decodeSerializableElement(%N, $index, ${jsonType.serializerTemplate})${jsonType.deserializationMapper}",
                                *(listOf<Any>(descriptor) + jsonType.serializerVarargs).toTypedArray()
                                )

                        }
                    }
                }
                addStatement("%M -> break@loop", MemberName("kotlinx.serialization.encoding.CompositeDecoder.Companion", "DECODE_DONE"))
                addStatement("else -> throw %T(\"Unexpected index \$i\")", SerializationException::class.asTypeName())

                addStatement("⇤}")
                addStatement("⇤}")
                addStatement("builder.build()")
                addStatement("⇤}\n⇤⇤return ret")
            }.build()
        }
    }

    public companion object {
        context(KDOptions)
        public fun create(holder: KDType.Model, logger: KDLogger): KDTypeJsonBuilder =
            KDTypeJsonBuilder(holder, logger)
    }
}
