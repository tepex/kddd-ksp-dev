package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

internal fun KSClassDeclaration.toClassNameImpl(): ClassName =
    ClassName.bestGuess("${simpleName.asString()}Impl")

internal fun TypeName.toNullable(nullable: Boolean = true) =
    if (isNullable != nullable) copy(nullable = nullable) else this

internal fun FunSpec.Builder.addUncheckedCast(): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())

internal fun KDType.IEntity.build() {

    val paramId = parameters.find { it.name.simpleName == KDType.IEntity.ID_NAME }
        ?: error("ID parameter not found for Entity $className")

    FunSpec.builder("hashCode").apply {
        addModifiers(KModifier.OVERRIDE)
        addStatement("return %N.hashCode()", paramId.name)
        returns(Int::class)
    }.build().also(builder::addFunction)

    /*        if (this === other) return true
        if (other !is MyEntityImpl) return false
        if (id != other.id) return false
        return true
*/
    val paramOther = ParameterSpec.builder("other", ANY.toNullable()).build()
    FunSpec.builder("equals").apply {
        addModifiers(KModifier.OVERRIDE)
        addParameter(paramOther)
        addStatement("if (this === other) return true")
        addStatement("if (%N !is %T) return false", paramOther, className)
        addStatement("if (%N != %N.%N) return false", paramId.name, paramOther, paramId.name)
        addStatement("return true")
        returns(Boolean::class)
    }.build().also(builder::addFunction)


    parameters.filter { it.name.simpleName != KDType.IEntity.ID_NAME }
        .fold(mutableListOf<Pair<String, MemberName>>()) { acc, param -> acc.apply { add("%N: $%N" to param.name) } }
        .let { it.joinToString() { pair -> pair.first } to it.fold(mutableListOf<MemberName>(paramId.name)) { acc, pair ->
            acc.apply {
                add(pair.second)
                add(pair.second)
            }
        } }.also { pair ->
            FunSpec.builder("toString").apply {
                addModifiers(KModifier.OVERRIDE)
                returns(String::class)
                addStatement("return \"[ID: $%N, ${pair.first}]\"", *pair.second.toTypedArray())
            }.build().also(builder::addFunction)
        }
}

internal fun String.kdTypeOrNull(helper: KDTypeHelper, parentTypeName: TypeName) = when(this) {
    KDType.Sealed::class.java.simpleName -> KDType.Sealed.create(helper.typeName)
    KDType.Data::class.java.simpleName -> KDType.Data.create(helper, false)
    KDType.IEntity::class.java.simpleName -> KDType.IEntity.create(helper)
    KDType.Boxed::class.java.simpleName -> {
        runCatching { KDType.Boxed.create(helper, parentTypeName) }.getOrElse {
            //logger.log(it.message ?: "Cant parse parent type $parentTypeName")
            null
        }
    }
    else -> null
}
