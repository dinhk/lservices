package com.qut.webservices.igalogicservices.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import kotlinx.datetime.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
open class CourseEnrolment(
        val studentId: String,
        val courseCode: String,
        val courseAttemptNo: Int,
        val courseVersionNo: Int,
        @JsonSerialize(using = ToStringSerializer::class)
        val commencementDate: LocalDate,
        @JsonSerialize(using = ToStringSerializer::class)
        val completionDate: LocalDate? = null,
        val calcAttendanceModeCode: String?,
        var attendanceMode: String,
        @JsonSerialize(using = ToStringSerializer::class)
        val discontinuedDate: LocalDate? = null,
        val studyModeCode: String,
        val locationCode: String,
        val courseStatusCode: String,
        val studyTypeCode: String,
        val calcLoadCatCode: String?,
        var loadCategoryCode: String,
        @JsonSerialize(using = ToStringSerializer::class)
        val conferralDate: LocalDate? = null,
        val spkCategoryTypeCode: String,
        val owningOrgUnits: Array<OrgUnits>? = null,
        val supervisorOrgUnits: Array<OrgUnits>? = null,
        val availabilityOrgUnits: Array<OrgUnits>? = null,
        val deleted: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourseEnrolmentDto(
    val studentId: String,
    val courseCode: String,
    val courseAttemptNo: Int,
    val courseVersionNo: Int,
    @JsonSerialize(using = ToStringSerializer::class)
    val commencementDate: LocalDate,
    @JsonSerialize(using = ToStringSerializer::class)
    val completionDate: LocalDate? = null,
    val calcAttendanceModeCode: String?,
    val attendanceMode: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val discontinuedDate: LocalDate? = null,
    val studyModeCode: String,
    val calcLoadCatCode: String?,
    val locationCode: String,
    val courseStatusCode: String,
    val studyTypeCode: String,
    val loadCategoryCode: String,
    @JsonSerialize(using = ToStringSerializer::class)
    val conferralDate: LocalDate? = null,
    val spkCategoryTypeCode: String,
    val owningOrgUnitCode: String? = null,
    val responsibilityPercentage: Int? = null,
    val owningFacultyOrgUnitCode: String? = null,
    val supervisionOrgUnitCode: String? = null,
    val supervisionPercentage: Int? = null,
    val supervisionFacultyOrgUnitCode: String? = null,
    val availabilityOrgUnitCode: String? = null,
    val availabilityPercentage: Int? = null,
    val availabilityFacultyOrgUnitCode: String? = null,
    val deleted: Boolean,
){}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourseEnrolmentKey(
    val studentId: String,
    val courseCode: String,
    val courseAttemptNo: Int,
    val courseVersionNo: Int,
)

/**
 *
 * @param studentId
 * @param courseCode 
 * @param courseAttemptNo 
 * @param courseVersionNo 
 * @param commencementDate 
 * @param completionDate 
 * @param attendanceMode 
 * @param discontinuedDate 
 * @param studyModeCode 
 * @param locationCode 
 * @param courseStatusCode 
 * @param studyTypeCode 
 * @param loadCategoryCode 
 * @param conferralDate 
 * @param spkCategoryTypeCode 
 * @param deleted 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
open class CalculatedCourseEnrolment(
        studentId: String,
        courseCode: String,
        courseAttemptNo: Int,
        courseVersionNo: Int,
        commencementDate: LocalDate,
        completionDate: LocalDate? = null,
        calcAttendanceModeCode: String?,
        attendanceMode: String,
        discontinuedDate: LocalDate? = null,
        studyModeCode: String,
        locationCode: String,
        courseStatusCode: String,
        studyTypeCode: String,
        calcLoadCatCode: String?,
        loadCategoryCode: String,
        conferralDate: LocalDate? = null,
        spkCategoryTypeCode: String,
        owningOrgUnits: Array<OrgUnits>? = null,
        supervisorOrgUnits: Array<OrgUnits>? = null,
        availabilityOrgUnits: Array<OrgUnits>? = null,
        deleted: Boolean
) : CourseEnrolment(
    studentId,
    courseCode,
    courseAttemptNo,
    courseVersionNo,
    commencementDate,
    completionDate,
    calcAttendanceModeCode,
    attendanceMode,
    discontinuedDate,
    studyModeCode,
    locationCode,
    courseStatusCode,
    studyTypeCode,
    calcLoadCatCode,
    loadCategoryCode,
    conferralDate,
    spkCategoryTypeCode,
    owningOrgUnits,
    supervisorOrgUnits,
    availabilityOrgUnits,
    deleted
)
{
    var isCurrent: Boolean?  = null
    var isPostGraceAttempt: Boolean? = null
    var isAwardCourse: Boolean? = null
    var isNotYetStarted: Boolean? = null

    constructor (courseEnrolment: CourseEnrolment, statusMapper: (String) -> (String), isCurrent: Boolean, isPostGraceAttempt: Boolean, isAwardCourse: Boolean, isNotYetStarted: Boolean):
            this(
                courseEnrolment.studentId,
                courseEnrolment.courseCode,
                courseEnrolment.courseAttemptNo,
                courseEnrolment.courseVersionNo,
                courseEnrolment.commencementDate,
                courseEnrolment.completionDate, courseEnrolment.calcAttendanceModeCode,
                courseEnrolment.attendanceMode,
                courseEnrolment.discontinuedDate,
                courseEnrolment.studyModeCode,
                courseEnrolment.locationCode,
                statusMapper(courseEnrolment.courseStatusCode),
                courseEnrolment.studyTypeCode, courseEnrolment.calcLoadCatCode,
                courseEnrolment.loadCategoryCode,
                courseEnrolment.conferralDate,
                courseEnrolment.spkCategoryTypeCode,
                courseEnrolment.owningOrgUnits,
                courseEnrolment.supervisorOrgUnits,
                courseEnrolment.availabilityOrgUnits,
                courseEnrolment.deleted
            )
    {
        this.isCurrent = isCurrent
        this.isPostGraceAttempt = isPostGraceAttempt
        this.isAwardCourse = isAwardCourse
        this.isNotYetStarted = isNotYetStarted
        if (calcLoadCatCode != null)
        {
            loadCategoryCode = calcLoadCatCode
        }
        if (calcAttendanceModeCode != null)
        {
            attendanceMode = calcAttendanceModeCode
        }
    }

}


