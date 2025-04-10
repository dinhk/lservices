package com.qut.webservices.igalogicservices.utils

import kotlin.reflect.KType
import kotlin.reflect.full.createType

/**
Returns a default/stub value for the given type.
If the type is not a basic type the functions returns null
 */
fun getBasicTypeDefaultValue(kType: KType): Any? {
    return when (kType) {
        Double::class.createType() -> 0.0
        Float::class.createType() -> 0F
        Long::class.createType() -> 0L
        Int::class.createType() -> 1
        Short::class.createType() -> 0.toShort()
        Byte::class.createType() -> 0.toByte()
        Boolean::class.createType() -> false
        Char::class.createType() -> 'a'
        String::class.createType() -> "String"
        kotlinx.datetime.LocalDate::class.createType() -> kotlinx.datetime.LocalDate(2023, 1,1)
        Double::class.createType(nullable = true) -> 0.0
        Float::class.createType(nullable = true) -> 0F
        Long::class.createType(nullable = true) -> 0L
        Int::class.createType(nullable = true) -> 1
        Short::class.createType(nullable = true) -> 0.toShort()
        Byte::class.createType(nullable = true) -> 0.toByte()
        Boolean::class.createType(nullable = true) -> false
        Char::class.createType(nullable = true) -> 'a'
        String::class.createType(nullable = true) -> "string?"
        kotlinx.datetime.LocalDate::class.createType(nullable = true) -> kotlinx.datetime.LocalDate(2023, 1,1)
        else -> null
    }
}

/**
Returns the class name of the given type
This implementation is provisional
 */
fun KType.getClassName(): String {
    return this.toString().split(".").last().replace("?","")
}

/**
Returns true if the type is a Kotlin/KotlinX type
This implementation is provisional
 */
fun KType.isKotlinType(): Boolean {
    return this.toString().startsWith("kotlin")
}