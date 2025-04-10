package com.qut.webservices.igalogicservices.rules.identity

import com.qut.webservices.igalogicservices.dataaccess.HashDataAccess
import com.qut.webservices.igalogicservices.dataaccess.ReprocessingDataAccess
import com.qut.webservices.igalogicservices.dataaccess.ReverseLookupDataAccess
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Citizenship
import com.qut.webservices.igalogicservices.models.DefaultUserRole
import com.qut.webservices.igalogicservices.models.EnrichedIdentity
import com.qut.webservices.igalogicservices.models.ExecuteRulesRequest
import com.qut.webservices.igalogicservices.models.ExecuteRulesResponse
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.models.Location
import com.qut.webservices.igalogicservices.models.Role
import com.qut.webservices.igalogicservices.rules.core.Attribute
import com.qut.webservices.igalogicservices.usernamegenerator.entity.Username
import com.qut.webservices.igalogicservices.usernamegenerator.service.UsernameService
import com.qut.webservices.igalogicservices.utils.Hashing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody
import java.time.LocalDate


@Component
class RulesExecutor @Autowired constructor(
    private val hashDataAccess: HashDataAccess,
    private val rulesLibrary: IdentityRules,
    private val reprocessingDataAccess: ReprocessingDataAccess,
    private val roleAssignmentState: RoleAssignmentState,
    private val reverseLookupDataAccess: ReverseLookupDataAccess,
    private val usernameService: UsernameService
) {

    private val logger: Logger = LoggerFactory.getLogger(RulesExecutor::class.java)


    /**
     * Execute business rules against the provided identity
     * The set of changed attributes is required. In the normal flow, this was provided by the Change Router.
     * The identity object must contain non-null entities for the set of "required entities" returned by the Change Router
     * To generate the required calculated attributes some ephemeral objects are created, such as currentCourseAttempts.
     * These are removed before returning the calculations object to the caller
     */
    fun executeRules(@RequestBody executeRulesRequest: ExecuteRulesRequest): ExecuteRulesResponse {
        val changedAttributes = mutableListOf<String>()
        val identity = Identity(executeRulesRequest.identity)

        val originalChanges = changedAttributes.map { Attribute(it) }

        if (executeRulesRequest.changedAttributesString != null) {
            changedAttributes.addAll(executeRulesRequest.changedAttributesString.split("^").toTypedArray())
        } else if (executeRulesRequest.changedAttributes != null) {
            changedAttributes.addAll(executeRulesRequest.changedAttributes)
        } else {
            throw IllegalArgumentException("${executeRulesRequest.correlationId} - ChangedAttributesString or ChangedAttributes is required.")
        }

//        store hashes
        val hashing = Hashing()
        val result = hashing.calculateHashes(identity)
        val calculatedHashes = result.second.entries.associateBy({ it.key.toString() }, { it.value })
        hashDataAccess.putHashes(identity.igaUserId.toString(), calculatedHashes)

//        if the treatNullsAsEmpty flag is set to true, create empty collections if the attribute is null
//        this is for caller-convenience
        if (executeRulesRequest.treatNullsAsEmpty) {
            PadInput(identity)
        }

        val required = rulesLibrary.checkRequirementsFulfilled(
            changedAttributes.map { Attribute(it) },
            identity,
            executeRulesRequest.correlationId
        )
        val calculations =
            rulesLibrary.executeRules(
                changedAttributes.map { Attribute(it) },
                identity,
                executeRulesRequest.correlationId
            )

        val allRoles: MutableList<Role>

        if (executeRulesRequest.generateRolesDelta) {
            // work out what role assignments and un-assignments need to be made
            allRoles = produceRoleDeltaCollection(calculations, identity)
        } else {
            allRoles = produceRoleCollection(calculations, identity)
        }

//        remove extra calculation collections
        calculations.currentCourseEnrolments = null
        calculations.currentCourseAttempts = null
        calculations.currentOccupancies = null
        calculations.currentOccupancies = null
        calculations.currentUnitEnrolments = null
        calculations.bestOccupancy = null


        if (calculations.scheduledReprocessing.isNotEmpty()) {
            reprocessingDataAccess.putReprocessingData(calculations.scheduledReprocessing)
        }

//        to save an extra call to getUser to work out if we have already created the user in Saviynt, use the RoleAssignmentState class to
//        work out if we've already assigned the user to the DefaultUserRole
        val defaultUserRole = DefaultUserRole()
        val resolvedRoles = roleAssignmentState.mutateState(
            identity.igaUserId!!,
            listOf(defaultUserRole),
            listOf("DefaultUser")
        )

        calculations.igaUserCreated = resolvedRoles.isEmpty()

        calculations.roleCode = when {
            executeRulesRequest.identity.employeeId != null -> "STAFF"
            executeRulesRequest.identity.studentId != null -> "STUDENT"
            else -> ""
        }

        val igaUserId = executeRulesRequest.identity.igaUserId
        var usernameEntity: Username? = null
        if (igaUserId != null) {
            val person = executeRulesRequest.identity.person
            usernameEntity = usernameService.getUsernameForIgaUserId(
                person!!.familyName, person.givenName, person.preferredGivenName, person.middleName, igaUserId
            )
        }

        val enrichedIdentity = EnrichedIdentity(
            studentId = executeRulesRequest.identity.studentId,
            employeeId = executeRulesRequest.identity.employeeId,
            igaUserId = executeRulesRequest.identity.igaUserId,
            studentAccountId = executeRulesRequest.identity.studentAccountId,
            staffAccountId = executeRulesRequest.identity.staffAccountId,
            iamClientId = executeRulesRequest.identity.iamClientId,
            calculated = calculations,
            person = executeRulesRequest.identity.person,
            location = executeRulesRequest.identity.location,
            roles = allRoles.toTypedArray(),
            cards = executeRulesRequest.identity.cards,
            qutUsername = usernameEntity?.username
        )

//
//        val changedOnly = getDelta(executeRulesRequest.correlationId, enrichedIdentity, originalChanges)

        return ExecuteRulesResponse(
            correlationId = executeRulesRequest.correlationId,
            message = null,
            enrichedIdentity = enrichedIdentity
        )
    }

    /**
     * Merge existing roles of the same type and derivation,
     * returning the role with the earliest effective from date and latest effectiveTo date
     */
    private inline fun <reified T> mergeRoles(allRoles: Array<T>): List<Role> where T : Role {
        val roleGroups = allRoles.groupBy { "${it.role},${it.deleted},${it.derivedFrom}" }
        val mergedRoles = mutableListOf<Role>()
        for (roles in roleGroups.values) {
            val earliestStart: LocalDate = roles.minBy { it.effectiveFrom }.effectiveFrom
            val latestEnd: LocalDate? = roles.sortedWith(compareBy(nullsLast()) { it.effectiveTo }).last().effectiveTo
            val firstInstance = roles.first()
            val mergedRole = Role(
                firstInstance.role,
                earliestStart,
                latestEnd,
                false,
                firstInstance.roleType,
                firstInstance.derivedFrom,
                null
            )
            mergedRoles.add(mergedRole)
        }
        return mergedRoles
    }

    /**
     * Compare the set of calculated roles with the set of roles believed to be assigned to the user in the IGA
     * This is approach is considered a last-resort solution and should only be used if Saviynt do not improve their
     * API, allowing QUT to efficiently retrieve previous role assignment requests
     */
    private fun produceRoleDeltaCollection(calculations: Calculated, identity: Identity): MutableList<Role> {
        val allRoles = mutableListOf<Role>()

        if (calculations.unitRoles != null) {
            val resolvedCurrentRoles = mergeRoles(calculations.unitRoles!!)
            val resolvedRoles = roleAssignmentState.mutateState(
                identity.igaUserId!!,
                resolvedCurrentRoles.toList(),
                listOf("UnitEnrolment")
            )
            allRoles.addAll(resolvedRoles)
            calculations.unitRoles = null
        }

        if (calculations.courseRoles != null) {
            val resolvedCurrentRoles = mergeRoles(calculations.courseRoles!!)
            val resolvedRoles = roleAssignmentState.mutateState(
                identity.igaUserId!!,
                resolvedCurrentRoles.toList(),
                listOf("CourseEnrolment")
            )
            allRoles.addAll(resolvedRoles)
            calculations.courseRoles = null
        }

        if (calculations.cLevelRoles != null) {
            val resolvedCurrentRoles = mergeRoles(calculations.cLevelRoles!!)
            val resolvedRoles = roleAssignmentState.mutateState(
                identity.igaUserId!!,
                resolvedCurrentRoles.toList(),
                listOf("Occupancy")
            )
            allRoles.addAll(resolvedRoles)
            calculations.cLevelRoles = null
        }

        if (calculations.facultyRoles != null) {
            val resolvedCurrentRoles = mergeRoles(calculations.facultyRoles!!)
            val resolvedRoles = roleAssignmentState.mutateState(
                identity.igaUserId!!,
                resolvedCurrentRoles.toList(),
                listOf("CourseEnrolment", "UnitEnrolment")
            )
            allRoles.addAll(resolvedRoles)
            calculations.facultyRoles = null
        }
        return allRoles
    }

    /*
    Return all resolved roles in a single collection, merging roles where the role name and derivation is the same
     */
    private fun produceRoleCollection(calculations: Calculated, identity: Identity): MutableList<Role> {
        val allRoles = mutableListOf<Role>()

        if (calculations.unitRoles != null) {
            val resolvedRoles = mergeRoles(calculations.unitRoles!!)
            allRoles.addAll(resolvedRoles)
            calculations.unitRoles = null
        }

        if (calculations.courseRoles != null) {
            val resolvedRoles = mergeRoles(calculations.courseRoles!!)
            allRoles.addAll(resolvedRoles)
            calculations.courseRoles = null
        }

        if (calculations.cLevelRoles != null) {
            val resolvedRoles = mergeRoles(calculations.cLevelRoles!!)
            allRoles.addAll(resolvedRoles)
            calculations.cLevelRoles = null
        }

        if (calculations.facultyRoles != null) {
            val resolvedRoles = mergeRoles(calculations.facultyRoles!!)
            allRoles.addAll(resolvedRoles)
            calculations.facultyRoles = null
        }

        if (calculations.epeRoles != null) {
            val resolvedRoles = mergeRoles(calculations.epeRoles!!)
            allRoles.addAll(resolvedRoles)
            calculations.epeRoles = null
        }

        return allRoles
    }

    private fun PadInput(identity: Identity) {
        if (identity.unitEnrolments == null) {
            identity.unitEnrolments = arrayOf()
        }
        if (identity.epeCourses == null) {
            identity.epeCourses = arrayOf()
        }
        if (identity.courseEnrolments == null) {
            identity.courseEnrolments = arrayOf()
        }
        if (identity.occupancies == null) {
            identity.occupancies = arrayOf()
        }
        if (identity.cards == null) {
            identity.cards = arrayOf()
        }
        if (identity.location == null) {
            identity.location = Location()
        }
        if (identity.citizenship == null) {
            identity.citizenship = Citizenship(null, false)
        }
        if (identity.person?.addresses == null) {
            identity.person?.addresses = arrayOf()
        }
        if (identity.person?.mobilePhones == null) {
            identity.person?.mobilePhones = arrayOf()
        }
        if (identity.person?.landlines == null) {
            identity.person?.landlines = arrayOf()
        }
        if (identity.person?.emails == null) {
            identity.person?.emails = arrayOf()
        }
    }

    /*
    //    For the given enriched identity object, return only the set of attributes which have changed since the last invocation of the
    //    rules engine by comparing the object with a set of stored hashes
     */
    fun getDelta(igaUserId: String, entity: EnrichedIdentity, originalChanges: List<Attribute>): EnrichedIdentity {
        val hashing = Hashing()
        val storedHashes = hashDataAccess.getHashes(igaUserId)
        val storedAttributeHashes = storedHashes.entries.associateBy({ Attribute(it.key) }, { it.value })
        val result = hashing.calculateHashes(entity)
        val calculatedHashes = result.second.entries.associateBy({ it.key.toString() }, { it.value })
        hashDataAccess.putHashes(igaUserId, calculatedHashes)
        val providedEntities = result.first
        logger.trace("providedEntities = {}", providedEntities)
        logger.trace("calculatedHashes = {}", calculatedHashes)
        logger.trace("originalChanges = {}", originalChanges)

        val changes = hashing.compareHashes(storedAttributeHashes, result.second)
        changes.addAll(originalChanges)
        val allDistinctChanges = changes.distinct()

        val changedOnlyView =
            hashing.whiteOutUnchanged(allDistinctChanges, entity, listOf(Attribute(entity::calculated)))

        logger.trace("changes = {}", changes.toList())
        logger.trace("storedHashes = {}", storedHashes.keys.toList())
        return changedOnlyView

    }
}