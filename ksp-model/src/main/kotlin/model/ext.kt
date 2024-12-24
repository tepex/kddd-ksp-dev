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
import ru.it_arch.clean_ddd.ksp.model.KDReference.Collection

internal fun TypeName.toNullable(nullable: Boolean = true) =
    if (isNullable != nullable) copy(nullable = nullable) else this

internal fun FunSpec.Builder.addUncheckedCast(): FunSpec.Builder =
    addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"UNCHECKED_CAST\"").build())

internal val TypeName.dslBuilderFunName: String
    get() = toString().substringAfterLast('.').replaceFirstChar { it.lowercaseChar() }

internal fun TypeName.toKDReference(holder: KDType.Model): KDReference = when(this) {
    is ParameterizedTypeName -> Collection.create(this)
    else -> holder.getKDType(this).let(KDReference.Element::create)
}
