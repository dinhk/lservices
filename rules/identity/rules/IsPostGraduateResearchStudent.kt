package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsPostGraduateResearchStudent() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Boolean? {
        val currentCourses = changedAttribute(calculations.currentCourseEnrolments, calculations::currentCourseEnrolments)

        returns(calculations::postGraduateResearchStudent)

        if (currentCourses.isEmpty())
        {
            return false
        }

        return currentCourses.any { (it.isPostGraceAttempt == true || it.isCurrent == true) && it.spkCategoryTypeCode in listOf("101","103") }
    }


}