package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

private const val CLEVEL_SUBSTRING_START_INDEX = 0
private const val CLEVEL_SUBSTRING_LENGTH = 5

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_ARCHIBUS_UTIL.Get_Division_Code
class BestOccupancyDivisionCode : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): String? {
        val bestOccupancyClevel = changedNullableAttribute(calculations.bestOccupancyClevel, calculations::bestOccupancyClevel)

        returns(calculations::bestOccupancyDivisionCode)

        if(bestOccupancyClevel == null) {
            return null
        }

        val length = if (bestOccupancyClevel.length >= CLEVEL_SUBSTRING_LENGTH) CLEVEL_SUBSTRING_LENGTH else bestOccupancyClevel.length

        return bestOccupancyClevel.substring(CLEVEL_SUBSTRING_START_INDEX, length)
    }
}