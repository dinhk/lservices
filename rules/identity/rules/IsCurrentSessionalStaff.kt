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

private val VALID_SESSIONAL_STATUS: Array<String> = arrayOf("CASA", "SASA")

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsCurrentSessionalStaff: Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Boolean? {
        val occupancies = changedAttribute(calculations.singleOccupancyPerJobId, calculations::singleOccupancyPerJobId)

        returns(calculations::currentSessionalStaff)

        for (occupancy in occupancies) {
            if(isSessional(occupancy.employmentStatus) && isCurrent(occupancy.endDate)) {
                return true
            }
        }
        return false
    }

    fun isSessional(employmentStatus: String): Boolean {
        return employmentStatus.uppercase() in VALID_SESSIONAL_STATUS
    }

//    According to the proc Fudge_Sessional_Occ_Dates, we may have to add a year to the occupancy end date
    fun isCurrent(endDate: LocalDate?): Boolean {
        val timeNow: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        return endDate == null || endDate >= timeNow
    }
}