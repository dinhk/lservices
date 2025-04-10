package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import com.qut.webservices.igalogicservices.models.Occupancy
import kotlinx.datetime.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Provisional rule - waiting for a business requirement.
//We will need to exclude former employees with no occupancies within the last x years
class ShouldCreateIgaUserForEmployee() : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Any? {
        val occupancies = changedAttribute(source.occupancies, source::occupancies)
        val timeNow: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        returns(calculations::shouldCreateIgaUser)

        return occupancies.any {
            !it.deleted
                    && (it.endDate == null || it.endDate >= timeNow.minus(5, DateTimeUnit.YEAR))
        }

    }
}