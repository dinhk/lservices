package com.qut.webservices.igalogicservices.rules.identity

import com.qut.webservices.igalogicservices.utils.Hashing
import com.qut.webservices.igalogicservices.dataaccess.HashDataAccess
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.models.ResolveDataRequirementsRequest
import com.qut.webservices.igalogicservices.models.ResolveDataRequirementsResponse
import com.qut.webservices.igalogicservices.rules.core.Attribute
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


@Component
class ChangeRouter @Autowired constructor (private val hashDataAccess: HashDataAccess, private val rulesLibrary: IdentityRules) {
    private val logger: Logger = LoggerFactory.getLogger(ChangeRouter::class.java)

    /**
     * Used by the resolveDataRequirements API endpoint to:
     * -compute hashes for the incoming data change event
     * -compare those hashes with any stored hashes for the user/identity
     * -determine if there are any other data entities required to execute in-scope business rules
     * NOTE: by design all hash comparisons work at a maximum of 2 levels deep from the root Identity object
     * For example: field level changes are detected for attributes on the root Identity object and for attributes on the Person object.
     * Field level changes are not detected for arrays/collections - for example student Courses. This method only detects changes
     * to the overall array/collection. This is sufficient to determine what rules need to be (re-)executed and what data is required to execute the
     * rules.
     * Any change to the Contacts points (which are attached to the Person object) will be detected at the Entity level - i.e. this method
     * will detect a change to contact points but not which contact point.
     * If, in the future, there is a requirement to detect field level changes in an object which 3 levels deep:
     * -attach that object to the root object (the internal representation of the identity doesn't need to match the service contract)
     * If the field belongs to a collection (such as a phone number in a contact point):
     * -attach that object to the root object and create a new Type for the contact point e.g. PersonalPhone
     * If the field belongs to a collection of objects which don't have a fixed identifying attribute
     * -this method will need to be extended to traverse collections and will need to ensure that each item in the collection is sorted in a deterministic way.
     * Note: there is no foreseen requirement to do this
     */
    fun resolveDataRequirements(resolveDataRequirementsRequest: ResolveDataRequirementsRequest): Any {
        val identity = Identity(resolveDataRequirementsRequest.identity)
        val correlationId = resolveDataRequirementsRequest.correlationId
        val fullRefresh = resolveDataRequirementsRequest.fullRefresh

        val hashing = Hashing()

        if (identity.igaUserId == null) {
            throw IllegalArgumentException("$correlationId - igaUserId was not provided.")
        }

        val storedHashes = hashDataAccess.getHashes(identity.igaUserId.toString())
        val storedAttributeHashes = storedHashes.entries.associateBy({ Attribute(it.key) }, {it.value})
        val result = hashing.calculateHashes(identity)
        val calculatedHashes = result.second.entries.associateBy({ it.key.toString() }, {it.value})
        hashDataAccess.putHashes(identity.igaUserId.toString(), calculatedHashes)
        val providedEntities =  result.first
        logger.trace("providedEntities = {}", providedEntities)
        logger.trace("calculatedHashes = {}", calculatedHashes)

        if (providedEntities.none { it.isKotlinType == false }) {
            throw IllegalArgumentException("$correlationId - At least 1 non-null entity is expected.")
        }

        val allChanges = mutableListOf<Attribute>()

        if (!fullRefresh) {
            providedEntities.forEach {
                allChanges += hashing.compareHashes(storedAttributeHashes, result.second, it)
            }
        } else {
            // Force a full refresh by marking the employeeId/studentId as having changed
            if (identity.employeeId != null) {
                allChanges.add(Attribute(identity::employeeId))
            }
            if (identity.studentId != null) {
                allChanges.add(Attribute(identity::studentId))
            }
        }

        val changes = allChanges.distinct()

        logger.trace("changes = {}", changes.toList())
        logger.trace("storedHashes = {}", storedHashes.keys.toList())

        var requiredEntities: List<String> = listOf()
        val haltFurtherProcessing: Boolean //True - No changes have been identified and therefore Boomi does not need to continue in processing the identity, False - Changes have been
        val skipRulesExecution: Boolean // True - The changes detected will not trigger a rule, False - The changes detected will trigger a rule

        if (changes.isNotEmpty()) {
            requiredEntities =
                rulesLibrary.resolveDataRequirements(changes, identity, storedAttributeHashes.filter { !it.value.isNullOrEmpty() }.keys.toList())
            //        exclude the incoming changed entity and the Calculations object
            requiredEntities = requiredEntities.filter { it !in providedEntities.map { it.className } }
            haltFurtherProcessing = false
            skipRulesExecution = !rulesLibrary.isRuleTriggeredByAttributes(changes.toList())
        } else { // Changes have not been detected
            haltFurtherProcessing = true
            skipRulesExecution = true
        }

        val requiredEntitiesArray = requiredEntities.distinct().toTypedArray()
        val changesArray = changes.map {it.toString()} .toTypedArray()

        return ResolveDataRequirementsResponse(
            correlationId = correlationId,
            fullRefresh = fullRefresh,
            identity = resolveDataRequirementsRequest.identity,
            requiredEntities = requiredEntitiesArray,
            changedAttributes = changesArray,
            requiredEntitiesString = requiredEntitiesArray.joinToString("^"),
            changedAttributesString = changesArray.joinToString("^"),
            haltFurtherProcessing = haltFurtherProcessing,
            skipRulesExecution = skipRulesExecution
        )
    }

}