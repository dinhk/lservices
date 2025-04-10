package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_IDENTITY_UTIL.Generate_Display_Name
class GenerateDisplayName : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): String? {
        val person = changedAttribute(source.person, source::person)
        val preferredGivenName = changedNullableAttribute(person.preferredGivenName, person::preferredGivenName)
        val givenName = changedNullableAttribute(person.givenName, person::givenName)
        val familyName = changedAttribute(person.familyName, person::familyName)

        returns(calculations::displayName)
        val displayName = generateDisplayName(preferredGivenName, givenName, familyName)
        return displayName.ifBlank { "" }
    }

    private fun generateDisplayName(preferredGivenName: String?, givenName: String?, familyName: String): String {
        val displayName = when {
            !preferredGivenName.isNullOrBlank() -> "$preferredGivenName $familyName"
            !givenName.isNullOrBlank() -> "$givenName $familyName"
            else -> familyName
        }
        return displayName.trim()
    }
}