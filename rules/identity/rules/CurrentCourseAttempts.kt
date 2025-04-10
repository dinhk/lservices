package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.CalculatedCourseEnrolment
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class CurrentCourseAttempts() : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Array<CalculatedCourseEnrolment>? {
        val currentCourses = changedAttribute(calculations.currentCourseEnrolments, calculations::currentCourseEnrolments)
        returns(calculations::currentCourseAttempts)

        return currentCourses.filter { it.isCurrent == true }.toTypedArray()
    }
}