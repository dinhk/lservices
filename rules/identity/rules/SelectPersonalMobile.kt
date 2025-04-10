package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)

class SelectPersonalMobile : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Any? {
        val person = changedAttribute(source.person, source::person)
        val mobiles = changedNullableAttribute(person.mobilePhones, person::mobilePhones)

        returns(calculations::personalMobile)

        if (mobiles == null)
        {
            return null
        }

        val entry = mobiles.firstOrNull { it.contactType == "Personal" && it.contactPreference == "Transactional" }
                ?: return null

        return entry.phoneNumber

    }
}