package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component


@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Generate_IAS_Role_Indicator
class GenerateInternetAccessRoleCode() : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Any? {
        val studentId = changedNullableAttribute(source.studentId, source::studentId)
        val employeeId = changedNullableAttribute(source.employeeId, source::employeeId)

        returns(calculations::iasRoleCode)

        if (employeeId != null)
        {
//            S for Staff
            return "S"
        }

        if (studentId != null)
        {
//            N for "No Idea what N stands for"
            return "N"
        }

        return null
    }
}