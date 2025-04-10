package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
/*
This rule instructs Boomi to load/reload occupancies, location data and cards when a student is added or changes
 */
class TriggerStudentLoad() : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Boolean? {
        val studentId = changedNullableAttribute(source.studentId, source::studentId)
        val epeCourse = requiredAttribute(source.epeCourses, source::epeCourses)
        val courseEnrolments = requiredAttribute(source.courseEnrolments, source::courseEnrolments)
        val unitEnrolments = requiredAttribute(source.unitEnrolments, source::unitEnrolments)
        val cards = requiredAttribute(source.cards, source::cards)

        returns(calculations::studentReloaded)

        return true
    }
}