package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName

internal fun TypeName.toNullable(nullable: Boolean = true) =
    if (isNullable != nullable) copy(nullable = nullable) else this

internal fun FunSpec.Builder.addUncheckedCast(): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())

internal val TypeName.isMutableCollection: Boolean
    get() = if (this !is ParameterizedTypeName) false
    else MUTABLE_MAP == rawType || MUTABLE_SET == rawType || MUTABLE_LIST == rawType

internal fun ClassName.toMutableCollection() = when (this) {
    LIST -> MUTABLE_LIST
    SET -> MUTABLE_SET
    MAP -> MUTABLE_MAP
    else -> error("Unsupported collection for mutable: $this")
}

internal fun TypeName.substituteArg(holder: KDType.Model): TypeName =
    if (isMutableCollection) this else
        holder.getKDType(this).let { if (it is KDType.Boxed) it.rawTypeName.toNullable(isNullable) else this }

