package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_FINANCE_UTIL.GENERATE_MGR_POSITION_NUMBER
class BestOccupancyManagerPositionNumber : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): String? {
        val bestOccupancy = changedNullableAttribute(calculations.bestOccupancy, calculations::bestOccupancy)

        returns(calculations::bestOccupancyManagerPositionNumber)

        return bestOccupancy?.managerPositionId
    }
}