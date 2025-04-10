package com.qut.webservices.igalogicservices.rules.core

import com.qut.webservices.igalogicservices.utils.getClassName
import com.qut.webservices.igalogicservices.utils.isKotlinType
import kotlin.reflect.KProperty
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaField

/**
 * Used to store path/class information for an attribute
 */
open class Attribute {

    private val pathString: String
    private val path: MutableList<String>
    val attribute: String
    val className: String
    val parent: String
    val isKotlinType: Boolean?
    val isArray: Boolean?
    val isSensitive: Boolean?

    constructor(className:String, pathString: String) {
        this.path = pathString.split('.') as MutableList<String>
        this.attribute = this.path.last()
        this.pathString = pathString
        this.className = className
        this.parent = path.last()
        this.isKotlinType = false
        this.isArray = false
        this.isSensitive = null
    }

    constructor(classNameAndPathString:String) {
        val parts = classNameAndPathString.split(",")
        this.className = parts[0]
        this.path = parts[1].split('.') as MutableList<String>
        this.attribute = path.last()
        this.pathString = parts[1]
        this.parent = path.first()
        this.isKotlinType = null
        this.isArray = null
        this.isSensitive = null
    }

    constructor(property: KProperty<*>, pathString: String? = null)
    {
        val propertyName = property.name
        if (pathString == null) {
            parent = property.javaField?.declaringClass?.name!!.split(".").last()
        }
        else
        {
            parent = pathString
        }
        this.pathString = "$parent.$propertyName"
        this.path = mutableListOf(parent, propertyName)
        this.attribute = propertyName
        val arrayOfNonKotlinObjects = property.returnType.arguments.size == 1 && !property.returnType.arguments.first().type!!.isKotlinType()
//        val nonKotlinObject = !property.returnType.isKotlinType()

        if (arrayOfNonKotlinObjects) {
            this.className = property.returnType.arguments.first().type!!.getClassName()
            this.isKotlinType = false
            this.isArray = true
            this.isSensitive = false
        }
        else {
            this.isSensitive = property.hasAnnotation<Sensitive>()
            this.className = property.returnType.getClassName()
            this.isKotlinType = property.returnType.isKotlinType()
            this.isArray = false
        }

    }

    override fun toString(): String {
        return "${this.className},${this.pathString}"
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as Attribute
        return (other.toString() == this.toString())
    }

    override fun hashCode(): Int {
        return pathString.hashCode()
    }
}