package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

//private const val FIRST_SEMESTER_GROUP = "First Semester Student"
private val INVALID_UNIT_STATUS = listOf("PASSED", "FAILED")

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_IDENTITY_UTIL.GENERATE_FIRSTSEMFLAG_FOR_STU
class IsFirstSemesterStudent: Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Boolean {
        val currentUnitEnrolments = changedAttribute(calculations.currentUnitEnrolments, calculations::currentUnitEnrolments)

        returns(calculations::firstSemester)

        if (currentUnitEnrolments.isEmpty()) {
            return false
        }

        return currentUnitEnrolments.all { it.isCurrent && it.unitStatusCode !in INVALID_UNIT_STATUS }
    }
}