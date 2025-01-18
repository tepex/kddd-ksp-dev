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

    private val funDeserialize = FunSpec.builder("deserialize")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("decoder", Decoder::class)
        .returns(holder.className)

    //private val sb

    init {
        holder.propertyHolders.forEachIndexed { index, property ->
            if (property.typeName is ParameterizedTypeName) {
                processCollection(property.name, JsonType.Collection.create(property.typeName), false).also { jsonType ->
                    descriptorFun.addCollection(property.serialName, jsonType)
                }
            } else {
                // direct to statement +Chunk

                // It works for scalar types

                val kdType = holder.getKDType(property.typeName).first
                if (kdType is KDType.Boxed) {
                    logger.log("${property.serialName}: ${kdType.boxedType} ${kdType.isPrimitive} parse: ${kdType.isParsable}")
                } else if (kdType is KDType.Generatable) logger.log(">>> ${property.serialName}: ${kdType.className}")

                if (kdType is KDType.Generatable) {
                    descriptorFun.addElement(property.serialName, kdType, property.typeName.isNullable)
                    //funSerialise
                } else {
                    TODO()
                }
                // funSerialize.add
                // funDeserialize.add
            }
        }

        val descriptor = descriptorFun.build()
        companionBuilder.addProperty(descriptor)

        companionBuilder.addFunction(serializeFun.build(descriptor))

        companionBuilder.addFunction(funDeserialize.build())
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
                    .also(collection::addForDescriptor)
            }
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
        private val className: ClassName
    ) {
        private val funSerialise = FunSpec.builder("serialize")
            .addModifiers(KModifier.OVERRIDE)

        fun build(descriptor: PropertySpec): FunSpec {
            val encoder = ParameterSpec.builder("encoder", Encoder::class).build()
            val encoderStructure = MemberName("kotlinx.serialization.encoding", "encodeStructure")
            funSerialise.addParameter(encoder)
                .addParameter("value", className)
                .addStatement("%N.%M(%N) {}", encoder, encoderStructure, descriptor)
            return funSerialise.build()
        }
    }

    public companion object {
        context(KDOptions)
        public fun create(holder: KDType.Model, logger: KDLogger): KDTypeJsonBuilder =
            KDTypeJsonBuilder(holder, logger)
    }
}
