package com.qut.webservices.igalogicservices.rules.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.qut.webservices.igalogicservices.dataaccess.StateDataAccess
import com.qut.webservices.igalogicservices.models.StateObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import java.security.MessageDigest
import kotlin.reflect.full.memberProperties


@RequestScope
@Component
class StateManager @Autowired constructor(val dataAccess: StateDataAccess)  {

    final val mapper = ObjectMapper()
    private var stateObjects: List<StateObject>? = null
    init {
        mapper.registerModule(JavaTimeModule())
    }

    fun getStateObjects(igaUserId: String) : List<StateObject>
    {
        if (stateObjects == null) {
            stateObjects = dataAccess.getStateObjects(igaUserId)
        }
        return stateObjects!!
    }

    final inline fun <reified T> getStateItemCollection(igaUserId: String, collectionName: String) : List<StateObject>
    {

        val mapper = ObjectMapper()
        mapper.registerModule(JavaTimeModule())
        val stateObjects =  getStateObjects(igaUserId)
        stateObjects.forEach { it.objectValue = mapper.readValue(it.objectJson, T::class.java)}
        return stateObjects.filter { it.collectionName == collectionName && it.objectStateCode != "FAILED"}
    }

    final inline fun <reified T> getCollection(igaUserId: String) : List<T>
    {
        val stateObjects = getStateItemCollection<T>(igaUserId, T::class.simpleName!!)
        return stateObjects.map { it.objectValue as T}
    }

    fun <T> updateCollection(igaUserId: String, collectionName: String, items: List<T>, keys: Array<String>) where T: IHasHashKey
    {

        val genericStateItems = mutableListOf<StateObject>()

        for (item in items)
        {
//            calculate a unique hash for the keys
            val keyMap = mutableMapOf<String,String>()
            for (property in item::class.memberProperties) {
                if (property.name in keys)
                {
                    keyMap[property.name] = property.getter.call(item).toString()
                }
            }
            val hashedKeys = md5(mapper.writeValueAsString(keyMap))
            item.hash = hashedKeys
            val json = this.mapper.writeValueAsString(item)
            genericStateItems.add(StateObject(igaUserId, collectionName, hashedKeys, json, "WRITTEN", null, null))
        }

        dataAccess.putStateObjects(genericStateItems)
    }

    fun updateStateObjectStatus(igaUserId: String, objectKeyHash: String, status:String)
    {
        dataAccess.updateStateObjectStatus(igaUserId, objectKeyHash, status)
    }

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
