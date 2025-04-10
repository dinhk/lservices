package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)

class SelectPersonalEmail : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): String? {
        val person = changedAttribute(source.person, source::person)
        val emails = changedNullableAttribute(person.emails, person::emails)

        returns(calculations::personalEmail)

        if (emails == null)
        {
            return null
        }

        val entry = emails.firstOrNull { it.contactType == "Personal" && it.contactPreference == "Transactional" }
                ?: return null

        return entry.address
    }
}