package com.qut.webservices.igalogicservices.rules.identity

import com.qut.webservices.igalogicservices.models.Role
import com.qut.webservices.igalogicservices.rules.core.StateManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import java.time.LocalDate


@Component
@RequestScope
class RoleAssignmentState  @Autowired constructor(val stateManager: StateManager)  {

    /**
     * Retrieve the currently assigned roles from the database
     * Compare with the provided set of roles assignments generated in this session
     * Return the set of operations required to synchronise the IGA with the new set of roles
     */
    final inline fun <reified T> mutateState(igaUserId:String, roles:List<T>, derivedFrom: List<String>) : List<Role> where T: Role
    {
        val existingAssignments = stateManager.getCollection<T>(igaUserId)
        val unmatchedRoles = mutableListOf<Role>()
        val resolvedRoles = mutableListOf<Role>()


//      check for new assignments/de-assignments
        for (role in roles)
        {
            val existing = existingAssignments.firstOrNull { it.role == role.role && !it.deleted }
            val existingDeleted = existingAssignments.firstOrNull { it.role == role.role && it.deleted }
            val existingOtherDerivation = existingAssignments.firstOrNull { it.role == role.role && it.derivedFrom != role.derivedFrom && !it.deleted }
            
            if (existing == null) {
                if (!role.deleted || existingDeleted == null) {
                    unmatchedRoles.add(role)
                }
            }
            else if (existingOtherDerivation == null) {
//                derivedFrom attribute matches
                if (role.deleted && existingDeleted == null) {
                    // a role is being explicitly deleted
                    // if any of the existing roles have a different derivedFrom value don't delete
                    resolvedRoles.add(role)
                } else {
//                    only do anything if the effective dates have changed
                    if (role.effectiveFrom != existing.effectiveFrom || role.effectiveTo != existing.effectiveTo) {
                        // delete the existing
                        resolvedRoles.add(Role(role.role, role.effectiveFrom, null, deleted = true, role.roleType, role.derivedFrom))
//                    add a new role with new effective dates
                        resolvedRoles.add(role)
                    }
                }
            }
            else {
                if (role.deleted) {
//            only delete from the state table
                    stateManager.updateCollection(igaUserId, T::class.simpleName!!, listOf(role), arrayOf("role", "roleType", "derivedFrom"))
                } else {
//                we have a role name match, and we're not deleting, but the derivation is different
//                calculate the widest effective dates
                    var earliestStart: LocalDate? = null
                    var earliestStartSet: Boolean = false
                    if (existingOtherDerivation.effectiveFrom < role.effectiveFrom) {
                        earliestStart = existingOtherDerivation.effectiveFrom
                        earliestStartSet = true
                    } else if (role.effectiveFrom < existingOtherDerivation.effectiveFrom) {
                        earliestStart = role.effectiveFrom
                        earliestStartSet = true
                    } else {
                        earliestStart = role.effectiveFrom
                    }

                    var latestEnd: LocalDate? = null
                    var latestEndSet: Boolean = false
                    if (role.effectiveTo == null && existingOtherDerivation.effectiveTo != null) {
                        latestEndSet = true
                        latestEnd = null
                    } else if (role.effectiveTo != null && existingOtherDerivation.effectiveTo != null && role.effectiveTo > existingOtherDerivation.effectiveTo) {
                        latestEndSet = true
                        latestEnd = role.effectiveTo
                    } else {
                        latestEnd = role.effectiveTo
                    }

                    if (earliestStartSet || latestEndSet) {
                        // delete the existing
                        resolvedRoles.add(Role(role.role, role.effectiveFrom, null, deleted = true, role.roleType, role.derivedFrom))
                        // add a new role with new effective dates
                        resolvedRoles.add(Role(role.role, earliestStart, latestEnd, deleted = false, role.roleType, role.derivedFrom))
                    }
                }
            }
        }

        val distinctUnmatched = unmatchedRoles.distinct()
        for (distinctRole in distinctUnmatched)
        {
            val allMatching = unmatchedRoles.filter { it.role == distinctRole.role}
            val earliestStart = allMatching.minByOrNull { it.effectiveFrom }!!
            val latestEnd = allMatching.sortedWith(compareByDescending<Role, LocalDate?>( nullsFirst()) { it.effectiveTo }).first()
            resolvedRoles.add(Role(distinctRole.role, earliestStart.effectiveFrom, latestEnd.effectiveTo, deleted = false, distinctRole.roleType, distinctRole.derivedFrom))
        }

//      check if we need to de-assign based on the current state
//      loop through currently assigned roles (from the database)
//      if a role is present in the database
//          and not in the passed set of roles
//              and the derivedFrom matches
//      then add the role with the "deleted" flag set to remove the
//      role assignment
        for (existingRole in existingAssignments)
        {
            if (existingRole.deleted != true && existingRole !in roles && existingRole.derivedFrom in derivedFrom)
            {
                existingRole.deleted = true
                resolvedRoles.add(existingRole)
            }
        }
        stateManager.updateCollection(igaUserId, T::class.simpleName!!, resolvedRoles, arrayOf("role", "roleType", "derivedFrom"))

        return resolvedRoles
    }

}