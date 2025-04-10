package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsIntermitStudent() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Boolean? {
        val currentCourses = changedAttribute(calculations.currentCourseAttempts, calculations::currentCourseAttempts)

        returns(calculations::intermitStudent)

        if (currentCourses.isEmpty())
        {
            return false
        }

        return currentCourses.any {
                it.courseStatusCode == "LOA"
        }
    }


}