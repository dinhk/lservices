package com.qut.webservices.igalogicservices.rules.core

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.qut.webservices.igalogicservices.dataaccess.ILogDataAccess
import kotlinx.datetime.LocalDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope


@RequestScope
@Component
class RuleLogger @Autowired constructor(val writer: ILogDataAccess) : IRuleLogger {

    private val logger: Logger = LoggerFactory.getLogger(RuleDependencyAnalysis::class.java)
    private var currentTriggers = mutableListOf<Pair<Attribute, Any?>>()
    private var currentOutputAttribute: Attribute? = null
    private var currentOutput: Any? = null
    private var currentRule: Rule<*,*>? = null
    private var currentRules = mutableListOf<RuleLog>()
    override var messages = mutableListOf<String>()
    final override val mapper = jacksonObjectMapper()
    init {
        mapper.registerModule(JavaTimeModule())
                .registerModule(SimpleModule().addSerializer(LocalDate::class.javaObjectType , ToStringSerializer.instance))
    }

    override fun prepareRule(rule: Rule<*,*>)
    {
        currentRule = rule
        currentTriggers = mutableListOf()
        currentOutputAttribute = null
    }

    override fun addTrigger(attribute: Attribute, value: Any?)
    {
        if (attribute.isSensitive == true)
        {
            currentTriggers.add(Pair(attribute, "***"))
        }
//        TODO: pass-down class name of calculations class
        else if (attribute.isKotlinType == false && attribute.parent == "Calculated")
        {
//            TODO add settings for verbose logging
            currentTriggers.add(Pair(attribute, "(calculated object, see output of the Rule responsible for the value)"))
        }
        else
        {
            currentTriggers.add(Pair(attribute, value))
        }
    }

    override fun addOutput(attribute: Attribute)
    {
        currentOutputAttribute = attribute
    }

    override fun addOutputValue(value: Any?)
    {
        currentOutput = if (currentOutputAttribute != null && currentOutputAttribute!!.isSensitive == true) {
            "***"
        } else {
            value
        }
    }

    override fun flushRule()
    {
        if (currentRule != null) {
            val ruleLog = RuleLog(
                currentRule!!::class.simpleName!!,
                currentTriggers.associateBy({ it.first.toString() }, { it.second }),
                currentOutputAttribute.toString(),
                currentOutput
            )
            currentRules.add(ruleLog)
        }
        currentTriggers = mutableListOf<Pair<Attribute, Any?>>()
        currentOutputAttribute = null
    }



    override fun flushRules()
    {
        val json = mapper.writerWithView(NotSensitive::class.java).writeValueAsString(currentRules)
        messages.add(json)
        currentRules = mutableListOf<RuleLog>()
    }

    override fun writeObject(objectToWrite: Any) {
        val json = mapper.writerWithView(NotSensitive::class.java).writeValueAsString(objectToWrite)
        messages.add(json)
    }

    override fun flushLog(igaUserId: String, correlationId: String)
    {
        writer.putLogMessages(igaUserId, correlationId, messages)

        messages = mutableListOf<String>()
    }

    override fun flushLog() : List<String>
    {
        val copy = messages.toCollection(mutableListOf())
        messages = mutableListOf<String>()
        return copy
    }

    override fun flushLogToLogger()
    {
        for (message in messages)
            logger.debug(message)
    }
}


data class RuleLog(
    val ruleName: String,
    val triggers: Map<String, Any?>,
    val outputProperty: String,
    val outputValue: Any?
)