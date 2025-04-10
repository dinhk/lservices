package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.CalculatedCourseEnrolment
import com.qut.webservices.igalogicservices.models.CourseEnrolment
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class CurrentCourseEnrolments() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Array<CalculatedCourseEnrolment>? {

        val courses = changedAttribute(source.courseEnrolments, source::courseEnrolments)
        returns(calculations::currentCourseEnrolments)


        val currentCourses = mutableListOf<CalculatedCourseEnrolment>()
        val timeNow: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (courses.isEmpty())
            return null

        for (course in courses)
        {
            if (!course.deleted && (timeNow >= course.commencementDate))
                {
                    val calculatedCourse = CalculatedCourseEnrolment(
                            course,
                            ::mapStatusCode,
                            isCurrent(course, timeNow),
                            isPostGraceAttempt(course, timeNow),
                            isAwardCourse(course),
                            isNotYetStarted(course))
                    currentCourses.add(calculatedCourse)
                }
        }
        return currentCourses.toTypedArray()
    }

//    integrate_iam_course_enrolment, map_course_status_code
    fun mapStatusCode(sourceStatusCode: String) : String
    {
        if (sourceStatusCode == "APPWITHDRAWN")
        {
            return "APPLIED"
        }
        if (sourceStatusCode == "CONDLYCOMPLETE")
        {
            return "POTENTIALLYCOMPLETE"
        }
        if (sourceStatusCode in listOf("LAPSED", "RESCINDED", "DECLINED", "DEFERRED"))
        {
            return "OFFERED"
        }

        return sourceStatusCode
    }

    fun isCurrent(course: CourseEnrolment, timeNow: LocalDate) : Boolean
    {
        val mappedCourseCode = mapStatusCode(course.courseStatusCode)
        return mappedCourseCode in listOf("ADMITTED","POTENTIALLYCOMPLETE","LOA")
                ||
                (mappedCourseCode == "ACCEPTED" && course.commencementDate >
                        timeNow.minus(1, unit = DateTimeUnit.YEAR))
    }

    fun isNotYetStarted(course: CourseEnrolment) : Boolean
    {
        return mapStatusCode(course.courseStatusCode) in listOf("APPLIED","OFFERED") && course.discontinuedDate == null
    }

    fun isAwardCourse(course:CourseEnrolment) : Boolean
    {
        return course.spkCategoryTypeCode in
                listOf("101","102","103","104","105","106","107","108","109","110","111","112","130","131","201","202","211","212","601","602","603","604","605","606","900")
    }

    fun isPostGraceAttempt(course: CourseEnrolment, timeNow: LocalDate) : Boolean
    {
        val mappedCourseCode = mapStatusCode(course.courseStatusCode)
        val withdrawn = mappedCourseCode in listOf("WITHDRAWN","WITHDRAWNEARLY")
        val completed = mappedCourseCode in listOf("COMPLETED") && !isAwardCourse(course)

//        TODO:need course start date to populate effective start date
        val dateOk = (course.commencementDate > timeNow.minus(30, unit = DateTimeUnit.DAY))

        return (withdrawn || completed) && dateOk
    }

}