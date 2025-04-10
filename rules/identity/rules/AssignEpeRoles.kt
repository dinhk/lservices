package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.EpeRole
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

//for now, don't provide a specific course name for the role
private const val EpeStudent: String = "EPEStudent"

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class AssignEpeRoles() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Array<EpeRole>? {
        val epeCourses = changedAttribute(source.epeCourses, source::epeCourses)
        returns(calculations::epeRoles)

        val timeNow: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val roles = mutableListOf<EpeRole>()

        for (course in epeCourses) {
            if (
            //            legacy IAM allows 7 days grace on the start date
            //            course.startDate > sevenDayaAgo &&
                    ((course.endDate == null || course.endDate >= timeNow)
                            && (course.hedStatus == null || course.hedStatus.uppercase() != "CANCELLED")) ) {
                val oneYearFromStartDate: LocalDate = course.startDate.plus(1, unit = DateTimeUnit.YEAR)
                roles.add(
                        EpeRole(
                                EpeStudent,
                                course.startDate.minus(7, unit = DateTimeUnit.DAY).toJavaLocalDate(),
                                (if ((course.endDate ?: oneYearFromStartDate) > oneYearFromStartDate) oneYearFromStartDate else course.endDate ?: oneYearFromStartDate).toJavaLocalDate() ,
                                deleted = false
                    )
                )
            }
        }
//        if the student is studying multiple course the role merge logic in the RulesExecutor will take care of the dates
        return roles.toTypedArray()
    }

}