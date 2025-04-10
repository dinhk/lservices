package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//See https://qut.atlassian.net/browse/IFP-517
class FirstCommenceDate : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Any? {
        val occupancies = changedAttribute(source.occupancies, source::occupancies)

        returns(calculations::firstCommencementDate)

        if(occupancies.isNotEmpty()) {
            return occupancies.filter { !it.deleted }.sortedBy { it.startDate }.firstOrNull()?.startDate
        }
        return null
    }
}