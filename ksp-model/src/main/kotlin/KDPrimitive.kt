package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName

internal enum class KDPrimitive(
    val typeName: ClassName,
    val encoderFun: String,
    val decoderFun: String
) {
    KD_BOOLEAN(BOOLEAN, "encodeBooleanElement", "decodeBooleanElement"),/*
    KD_BYTE,
    KD_CHAR,
    KD_DOUBLE,
    KD_FLOAT,
    KD_INT,
    KD_LONG,
    KD_SHORT,
    KD_STRING,*/
}
