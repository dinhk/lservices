package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

private const val OES_LOCATION_CODE = "QO"
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_QVP_UTIL.Generate_Oes_Stu_Grp
class HasOESCourse: Rule<Identity, Calculated>(){

    override fun execute(source: Identity, calculations: Calculated): Boolean {
        val currentCourseAttempts = changedAttribute(calculations.currentCourseAttempts, calculations::currentCourseAttempts)

        returns(calculations::oesStudent)

        return currentCourseAttempts.any { it.locationCode.equals(OES_LOCATION_CODE, true) }

    }
}