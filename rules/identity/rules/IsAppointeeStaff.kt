package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsAppointeeStaff: Rule<Identity, Calculated>() {
    val INVALID_EMPLOYMENT_STATUS: List<String> = listOf("CASA", "SASA")
    val INVALID_CLASSIFICATION: String = "PRACT"

    override fun execute(source: Identity, calculations: Calculated): Boolean {
        val currentOccupancies = changedAttribute(calculations.currentOccupancies, calculations::currentOccupancies)

        returns(calculations::appointeeStaff)

        for (occupancy in currentOccupancies) {
            if (IsValidEmploymentStatus(occupancy.employmentStatus) && IsValidClassification(occupancy.classificationCode)) {
                return true
            }
        }
        return false
    }

    fun IsValidEmploymentStatus(employmentStatus: String): Boolean {
        if(employmentStatus.uppercase() !in INVALID_EMPLOYMENT_STATUS) {
            return true
        } else {
            return false
        }
    }

    fun IsValidClassification(classification: String): Boolean {
        if(classification !in INVALID_CLASSIFICATION) {
            return true
        } else {
            return false
        }
    }
}