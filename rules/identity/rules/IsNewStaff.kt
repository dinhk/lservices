package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsNewStaff: Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Boolean {
        val firstCommenceDate = changedNullableAttribute(calculations.firstCommencementDate, calculations::firstCommencementDate)

        returns(calculations::newStaff)

        val timeNow: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (firstCommenceDate == null) {
            return false
        }
        else
        {
            return (firstCommenceDate.plus(90, unit = DateTimeUnit.DAY) > timeNow)
        }
    }


}