package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.models.UnitRole
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.toJavaLocalDate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component


//Generate_CurrUnit_for_Stu
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class AssignUnitRoles() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Array<UnitRole>? {
        val currentUnits = changedAttribute(calculations.currentUnitEnrolments, calculations::currentUnitEnrolments)
        returns(calculations::unitRoles)

        val roles = mutableListOf<UnitRole>()

        for (unit in currentUnits) {
            if (unit.isCurrent && unit.unitStatusCode in listOf("ENROLLED")) {
                roles.add(
                        UnitRole(
                        unit.unitCode,
                        unit.courseCommencementDate!!.toJavaLocalDate(),
                                null,
                                deleted = false
                    )
                )
            }
        }
        return roles.toTypedArray()
    }
}