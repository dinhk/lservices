package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_IAS_UTIL.GENERATE_IAS_FIRST_NAME AND SVC_IDENTITY_UTIL.Select_Preferred_Or_First_Name AND SVC_QUALTRICS_UTIL.Select_Qualtrics_First_Name
class CleanGivenName : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): String? {
        val person = changedAttribute(source.person, source::person)
        val preferredGivenName = changedNullableAttribute(person.preferredGivenName, person::preferredGivenName)
        val givenName = changedNullableAttribute(person.givenName, person::givenName)
        val familyName = changedNullableAttribute(person.familyName, person::familyName)

        returns(calculations::cleanGivenName)

        //Return in order of priority: preferredGivenName, givenName and if both are null then return family name
        //Returning family name logic has been implemented as per the logic contained in SVC_QUALTRICS_UTIL.Select_Qualtrics_First_Name
        return (preferredGivenName ?: givenName) ?: familyName ?: ""
    }
}