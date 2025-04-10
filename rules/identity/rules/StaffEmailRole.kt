package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

private const val STAFF_EMAIL_ROLE = "STAFF EMAIL"
private const val STAFF_CASUAL_EMAIL_ROLE = "STAFF CASUAL EMAIL"

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_O365_EMAIL_UTIL.GENERATE_STAFF_EMAIL_ROLE
class StaffEmailRole : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): String? {
        val currentOccupancies = changedAttribute(calculations.currentOccupancies, calculations::currentOccupancies)

        returns(calculations::staffEmailRole)

        if (currentOccupancies.isEmpty())
        {
            return null
        }

        for(occupancy in currentOccupancies) {
            if(isCasualStaff(occupancy.employmentStatus, occupancy.inoperativeCode)) {
                return STAFF_CASUAL_EMAIL_ROLE
            }
        }
        return STAFF_EMAIL_ROLE
    }

    fun isCasualStaff(employmentStatus: String, inoperativeCode: String?):Boolean {
        return if(employmentStatus.uppercase() in arrayOf("OFT", "OPT", "FFT", "FPT") && isStringNullOrEmpty(inoperativeCode)) {
            false
        } else if(employmentStatus.uppercase() in arrayOf("VISIT") && inoperativeCode?.uppercase() in arrayOf("AGENC", "CONSU", "SPAC", "PRCO")) {
            false
        } else if(inoperativeCode?.uppercase() == "CHANC") {
            false
        } else {
            true
        }
    }

    fun isStringNullOrEmpty(string: String?): Boolean {
        return string == null || string == ""
    }
}