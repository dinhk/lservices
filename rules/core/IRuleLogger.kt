package com.qut.webservices.igalogicservices.rules.core

import com.fasterxml.jackson.databind.ObjectMapper

interface IRuleLogger {
    fun prepareRule(rule: Rule<*, *>)

    fun addTrigger(attribute: Attribute, value: Any? = null)

    fun addOutput(attribute: Attribute)

    fun addOutputValue(value: Any?)

    fun flushRule()

    fun flushRules()

    fun flushLog(igaUserId: String, correlationId: String)

    fun flushLog() : List<String>

    fun flushLogToLogger()
    fun writeObject(objectToWrite: Any)

    val messages: MutableList<String>

    val mapper: ObjectMapper
}