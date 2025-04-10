package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsCurrentStaff: Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Boolean? {
        val currentOccupancies = changedAttribute(calculations.currentOccupancies, calculations::currentOccupancies)
        val currentSessional = changedAttribute(calculations.currentSessionalStaff, calculations::currentSessionalStaff)

        returns(calculations::currentStaff)

        return currentSessional || currentOccupancies.any()
    }
}