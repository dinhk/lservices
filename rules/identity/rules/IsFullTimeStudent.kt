package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsFullTimeStudent() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Boolean? {
        val currentCourseAttempts = changedAttribute(calculations.currentCourseAttempts, calculations::currentCourseAttempts)

        returns(calculations::fullTimeStudent)

        if (currentCourseAttempts.isEmpty())
        {
            return false
        }

        return currentCourseAttempts.any { it.loadCategoryCode == "FT"}

    }


}