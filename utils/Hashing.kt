package com.qut.webservices.igalogicservices.utils
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.qut.webservices.igalogicservices.rules.core.Attribute
import com.qut.webservices.igalogicservices.rules.core.Reversible
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Helper class responsible for serialising and objects and computing entity and field level hashes
 */
class Hashing constructor()
{
    private val logger: Logger = LoggerFactory.getLogger(Hashing::class.java)

    /**
     * Calculate field and entity level hashes for the given entity.
     * Returns the set of non-null entities received and a map of properties/hashes
     */
    fun <E> calculateHashes(entity: E, path: String? = null) : Pair<List<Attribute>, Map<Attribute,String?>> where E : Any
    {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val json = mapper.writeValueAsString(entity)
        val hash = this.sha2(json)

        val hashes = mutableMapOf<Attribute,String?>()
        val nonNullObjects = mutableListOf<Attribute>()

        var fullPath: String?
        val className = entity::class.simpleName!!
        fullPath = if (path == null) {
            className
        } else {
            "$path.$className"
        }

        hashes[Attribute(className, fullPath)] = hash

        for (property in entity::class.memberProperties) {
            val value = property.getter.call(entity)

            if (value == null)
            {
                hashes[Attribute(property)] = null
            }
            else
            {
//                logger.trace("property return type is ${property.returnType}")
//                recursively call this function if the type is not a kotlin/kotlinx type

                val arrayOfNonKotlinObjects = property.returnType.arguments.size == 1 && !property.returnType.arguments.first().type!!.isKotlinType()
                val nonKotlinObject = !property.returnType.isKotlinType()

//                for arrays, just hash the collection and go no further
                if (arrayOfNonKotlinObjects) {
                    nonNullObjects.add(Attribute(property))
                    hashes[Attribute(property)] = this.sha2(mapper.writeValueAsString(value))
                }
                else if (nonKotlinObject) {
                    nonNullObjects.add(Attribute(property))
                    val result = this.calculateHashes(value)
                    hashes.putAll(result.second)
                    nonNullObjects.addAll(result.first)
                }
                else {
//                    The line immediately below is a late change: Don't hash attributes like date-of-birth, which can theoretically be reversed
                    if (property.findAnnotation<Reversible>() == null)
                    {
                        hashes[Attribute(property)] = this.sha2(value.toString())
                        if (path == null) {
                            nonNullObjects.add(Attribute(property))
                        }
                    }
                }
            }
        }
        return Pair(nonNullObjects, hashes)
    }

    /**
     * Compare the set of hashes for a stored set (from database) against the hashes for an incoming data change
     */
    fun compareHashes(storedHashes: Map<Attribute,String?>, incomingHashes: Map<Attribute,String?>, scope: Attribute? = null) : MutableList<Attribute>
    {
        val changedSet = mutableListOf<Attribute>()
        for (key in incomingHashes.keys.filter {
            scope == null|| (
            it.parent == scope.className
                    || (scope.isArray!! && it.className == scope.className)
                    || (scope.isKotlinType!! && it == scope))
        })
        {
//            logger.trace("key=$key")
            if (storedHashes.containsKey(key))
            {
                if (incomingHashes[key] != storedHashes[key])
                {
                    logger.trace("{}!={}", key, key)
                    changedSet.add(key)
                }
            }
            else
            {
                logger.trace("new key={}", key)
                changedSet.add(key)
            }
        }
        return changedSet
    }

    /*
    Remove any attribute from the entity which has not changed
     */
    fun <E> whiteOutUnchanged(
            changedSet: List<Attribute>,
            entity: E,
            deepProbeList: List<Attribute>? = null,
            alwaysPreserveList: List<Attribute>? = null): E where E : Any {
        for (property in entity::class.memberProperties) {
            val value = property.getter.call(entity)

            if (value != null) {
                val arrayOfNonKotlinObjects =
                        property.returnType.arguments.size == 1 && !property.returnType.arguments.first().type!!.isKotlinType()
                val nonKotlinObject = !property.returnType.isKotlinType()
                val hasChanged = Attribute(property) in changedSet
                if (alwaysPreserveList != null && Attribute(property) in alwaysPreserveList)
                {
                    continue
                }

//                for arrays, if there's any change, anywhere in the collection, retain the whole collection
                if (arrayOfNonKotlinObjects) {
                    if (!hasChanged)
                    {
                        (property as KMutableProperty<*>).setter.call(entity, null)
                    }
                }
                else if (nonKotlinObject
                        && (deepProbeList == null || Attribute(property) in deepProbeList)
                        ) {
                    whiteOutUnchanged(changedSet, value)
                } else {
                    if (nonKotlinObject) {
                        val isMutableProperty = (property is KMutableProperty<*>)
                        if (isMutableProperty && !hasChanged) {
                            (property as KMutableProperty<*>).setter.call(entity, null)
                        }
                    }
                }
            }
        }
        return entity
    }


    private fun sha1(input: String) = hashString("SHA-1", input)
    private fun sha2(input: String) = hashString("SHA-256", input)
    private fun md5(input: String) = hashString("MD5", input)

    private fun hashString(type: String, input: String): String {
        val bytes = MessageDigest
            .getInstance(type)
            .digest(input.toByteArray())
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

}