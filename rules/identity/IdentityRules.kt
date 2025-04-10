package com.qut.webservices.igalogicservices.rules.identity

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.*
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Version 1 rules for the IAM Foundation project.
 * Rules are injected into this "library" using Spring Dependency Injection
 * Filters should be used to create/manage versions of rules
 */
@Component
class IdentityRules @Autowired constructor(allRules: Array<Rule<Identity, Calculated>>, ruleLogger: RuleLogger) : RulesLibrary<Identity, Calculated>(allRules, ruleLogger) {

    override fun protect(source: Identity, initialisedEntities: List<Attribute>): List<String> {
        val required = mutableListOf<String>()

        if ("Person" !in initialisedEntities.map { it.className })
        {
            required.add("Person")
        }

        return required
    }

    override fun createCalculationsObject(): Calculated {
        return Calculated()
    }

    override fun createSourceObject(): Identity {
        return mock<Identity>{}
    }

    override fun executeRules(changedAttributes: List<Attribute>, source: Identity, correlationId:String?): Calculated {
        val result = super.executeRules(changedAttributes, source, correlationId)
        ruleLogger.flushRules()

//        write all calculations to the log
        ruleLogger.writeObject(result)
        ruleLogger.flushLog(source.igaUserId.toString(), correlationId.toString())

        return result
    }


}