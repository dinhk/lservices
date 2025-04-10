package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.CLevelRole
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.toJavaLocalDate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component


@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class AssignCLevelRoles() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Array<CLevelRole>? {
        val currentOccupancies = changedAttribute(calculations.currentOccupancies, calculations::currentOccupancies)
        returns(calculations::cLevelRoles)

        val roles = mutableListOf<CLevelRole>()

        for (occupancy in currentOccupancies) {
            roles.add(
                CLevelRole(
                        occupancy.orgUnitCode,
                        occupancy.startDate.toJavaLocalDate(),
                        occupancy.endDate?.toJavaLocalDate(),
                        false
                )
            )
        }
        return roles.distinctBy { Triple(it.role, it.effectiveFrom, it.effectiveTo)  }.toTypedArray()
    }
}