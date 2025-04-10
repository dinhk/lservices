package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.CourseRole
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.toJavaLocalDate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component


@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class AssignCourseRoles() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Array<CourseRole>? {
        val currentCourses = changedAttribute(calculations.currentCourseEnrolments, calculations::currentCourseEnrolments)
        returns(calculations::courseRoles)

        val roles = mutableListOf<CourseRole>()

        for (course in currentCourses) {
            if (course.isCurrent == true) {
                roles.add(
                    CourseRole(
                        course.courseCode,
                        course.commencementDate.toJavaLocalDate(),
                        course.completionDate?.toJavaLocalDate(),
                            false
                    )
                )
            }
        }

        return roles.distinctBy { Triple(it.role, it.effectiveFrom, it.effectiveTo)  }.toTypedArray()
    }
}