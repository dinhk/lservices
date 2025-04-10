package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.FacultyRole
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.models.SamsOrgUnitAssignment
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.toJavaLocalDate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Partially implements Generate_CurrFaculty_for_Stu
//Faculty groups are also assigned on the basis of the courses being studied
//The rule AssignFacultyGroupsForCourseEnrolments is a companion of this rule
//In this current implementation the rule "AssignFacultyGroupsForUnitEnrolments" maps a set of SamsOrgUnitAssignments to a FacultyGroup type Role
//The RoleAssignmentState class then attempts to ensure that the set of FacultyGroup roles to be assigned to a user
//is a logical union of FacultyGroup roles derived from CourseEnrolments and UnitEnrolments
//A better alternative would be to:
//Create a unique set of Saviynt Entitlement values (one for each Faculty Code) and share that set of Entitlement values across two sets of Enterprise Roles:
//1. CourseFaculty
//2. UnitFaculty
//Based on our produce knowledge of Saviynt, the entitlement management logic in Saviynt will ensure that the entitlement is assigned/un-assigned based
//on the union of the two sets of Enterprise Roles for the user
class AssignFacultyGroupsForUnitEnrolments : Rule<Identity, Calculated>() {

    val facultyGroupMap = FacultyGroupMap().facultyGroupMap

    override fun execute(source: Identity, calculations: Calculated): Array<FacultyRole>? {
        val currentUnits = changedAttribute(calculations.currentUnitEnrolments, calculations::currentUnitEnrolments)
        returns(calculations::facultyRoles)

        val samsOrgUnits = mutableListOf<SamsOrgUnitAssignment>()

        for (unit in currentUnits) {
            if (unit.isCurrent || unit.isNotYetStarted ) {
//                    Obtain owning org unit of the unit
                    val owningOrgUnit = unit.owningOrgUnits?.maxByOrNull { it.responsibilityPercentage }
                    if (owningOrgUnit != null && owningOrgUnit.facultyUnitCode != "960000") {
                        samsOrgUnits.add(SamsOrgUnitAssignment(
                                owningOrgUnit.facultyUnitCode,
                                "UnitEnrolment",
                                unit.courseCommencementDate!!.toJavaLocalDate(),
                                null))
                    }
                }
            }

//        return samsOrgUnits.toTypedArray()

        val roles = mutableListOf<FacultyRole>()

        for (unit in samsOrgUnits) {
            val facultyGroupCode = facultyGroupMap.getOrDefault(unit.orgUnit,null)
            if (facultyGroupCode != null) {
                roles.add(
                        FacultyRole(
                                facultyGroupCode,
                                unit.effectiveFrom,
                                unit.effectiveTo,
                                false,
                                unit.derivedFrom
                        )
                )
            }
        }
        return roles.toTypedArray()
    }
}


