package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements Generate_IAS_Staff_Term_Date
class FinalTermDate : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Any? {
        val occupancies = changedAttribute(source.occupancies, source::occupancies)

        returns(calculations::finalTerminationDate)

        if(occupancies.isNotEmpty()) {
            return occupancies.filter { !it.deleted }.sortedWith(compareByDescending(nullsFirst()) { it.endDate}).firstOrNull()?.endDate
        }
        return null
    }
}