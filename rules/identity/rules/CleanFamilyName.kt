package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_QUALTRICS_UTIL.Select_Qualtrics_Last_Name
class CleanFamilyName : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): String? {
        val person = changedAttribute(source.person, source::person)
        // Assume changedAttribute returns String? to simulate nullable handling
        val familyName: String? = person?.let { changedAttribute(it.familyName, it::familyName) }

        returns(calculations::cleanFamilyName)

        // Ensure familyName is not null, return empty string if it is
        return familyName ?: ""
    }
}
