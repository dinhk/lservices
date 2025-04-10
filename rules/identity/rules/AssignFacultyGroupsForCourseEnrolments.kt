package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.*
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.toJavaLocalDate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Partially implements Generate_CurrFaculty_for_Stu
//Faculty groups are also assigned on the basis of the units being studied
//The rule AssignFacultyGroupsForUnitEnrolments is a companion of this rule
//In this current implementation the rule "AssignFacultyGroupsForCourseEnrolments" maps a set of SamsOrgUnitAssignments to a FacultyGroup type Role
//The RoleAssignmentState class then attempts to ensure that the set of FacultyGroup roles to be assigned to a user
//is a logical union of FacultyGroup roles derived from CourseEnrolments and UnitEnrolments
//A better alternative would be to:
//Create a unique set of Saviynt Entitlement values (one for each Faculty Code) and share that set of Entitlement values across two sets of Enterprise Roles:
//1. CourseFaculty
//2. UnitFaculty
//Based on our product knowledge of Saviynt, the entitlement management logic in Saviynt will ensure that the entitlement is assigned/un-assigned based
//on the union of the two sets of Enterprise Roles for the user
class AssignFacultyGroupsForCourseEnrolments : Rule<Identity, Calculated>() {

    val facultyGroupMap = FacultyGroupMap().facultyGroupMap

    override fun execute(source: Identity, calculations: Calculated): Array<FacultyRole>? {
        val currentCourses = changedAttribute(calculations.currentCourseEnrolments, calculations::currentCourseEnrolments)
        returns(calculations::facultyRoles)

        val samsOrgUnits = mutableListOf<SamsOrgUnitAssignment>()

        for (course in currentCourses) {
            if (course.isCurrent == true || course.isNotYetStarted == true) {
//              the Boomi getter for CourseEnrolments provides all the associated supervisors where:
//                supervisor_type_cd = 'S'
//                AND (start_dt <= SYSDATE )
//                AND (end_dt is null or s.end_dt > SYSDATE)
//              see novo_owner.integrate_course_enrolment - if there are multiple active supervisors, take one arbitrarily
//                TODO: check if there is a better way to select from multiple active supervisors
                val supervisorOrgUnit = course.supervisorOrgUnits?.firstOrNull()
                if (supervisorOrgUnit != null) {
                    samsOrgUnits.add(SamsOrgUnitAssignment(
                            supervisorOrgUnit.facultyUnitCode,
                            "CourseEnrolment",
                            course.commencementDate.toJavaLocalDate(),
                            course.completionDate?.toJavaLocalDate()))
                }
                else {
//                    Obtain owning org unit of the course attempt
                    val owningOrgUnit = course.owningOrgUnits?.maxByOrNull { it.responsibilityPercentage }
                    if (owningOrgUnit != null && owningOrgUnit.facultyUnitCode != "960000") {
                        samsOrgUnits.add(SamsOrgUnitAssignment(
                                owningOrgUnit.facultyUnitCode,
                                "CourseEnrolment",
                                course.commencementDate.toJavaLocalDate(),
                                course.completionDate?.toJavaLocalDate()))
                    }
                    else {
//                     Use course availability org units
                        val availabilityOrgUnits = course.availabilityOrgUnits?.map {
                            SamsOrgUnitAssignment(
                                    it.facultyUnitCode,
                                    "CourseEnrolment",
                                    course.commencementDate.toJavaLocalDate(),
                                    course.completionDate?.toJavaLocalDate())
                        }
                        if (!availabilityOrgUnits.isNullOrEmpty()) {
                            availabilityOrgUnits.toCollection(samsOrgUnits)
                        }
                    }
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