package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.models.Reprocess
import com.qut.webservices.igalogicservices.models.ReverseLookup
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

const val EmployeePositionRelationship:String = "EMPLOYEE_POSITION"
const val EmployeeManagerPositionRelationship:String ="EMPLOYEE_MANAGER_POSITION"
const val ReprocessingType:String = "Occupancy"

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsSupervisor: Rule<Identity, Calculated>() {


    override fun execute(source: Identity, calculations: Calculated): Any? {
        val occupancies = changedAttribute(calculations.currentOccupancies, calculations::currentOccupancies)
        val employeeId = requiredAttribute(source.employeeId, source::employeeId)

        returns(calculations::supervisor)

//        get an instance of the reverseLookupDataAccess class or a stub, if we're at metadata collection time
        val reverseLookupDataAccess = reverseLookup()

//            get the previous positions of the employee
        val previousPositions = reverseLookupDataAccess.getByParent(employeeId, EmployeePositionRelationship)

//            BEGIN reprocess all employees reporting to any changed positions
//            get current positions
        val currentPositions = occupancies.filter { !it.deleted }
        val currentPositionIds = currentPositions.map { it.positionId }
//                find vacated positions
        val vacatedPositions = previousPositions
                .filter { it.childKey !in currentPositionIds }
//           find new positions
        val newPositions = currentPositions
                            .filter { it -> it.positionId !in previousPositions.map { it.childKey } }

//        TODO:reduce to a single query
        val currentSubordinates = reverseLookupDataAccess.getByParent(currentPositionIds, EmployeeManagerPositionRelationship)
        val newSubordinates = reverseLookupDataAccess.getByParent(newPositions.map { it.positionId }, EmployeeManagerPositionRelationship)
        val previousSubordinates = reverseLookupDataAccess.getByParent(vacatedPositions.map { it.childKey }, EmployeeManagerPositionRelationship)
//        we assume we need to reprocess the employee's who occupy the position to which the employee reports (so that the supervisor flag is set for those employees)
        val newSupervisors = reverseLookupDataAccess.getByChild(newPositions
                .filter { !it.managerPositionId.isNullOrEmpty() }
                .map { it.managerPositionId!! }, EmployeePositionRelationship)

        val changedSubordinates = newSubordinates.union(previousSubordinates)

//           reprocess all new and previous subordinates - and new supervisors - by adding an entry to the scheduledReprocessing collection
        calculations.scheduledReprocessing
                .addAll(changedSubordinates
                        .map { Reprocess(ReprocessingType, it.childKey, Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())) })

        calculations.scheduledReprocessing
                .addAll(newSupervisors
                        .map { Reprocess(ReprocessingType, it.parentKey, Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())) })

//           clear previous positions for the employee
        reverseLookupDataAccess.clearByParent(employeeId, EmployeePositionRelationship)
//           clear previous manager positions
        reverseLookupDataAccess.clearByChild(employeeId, EmployeeManagerPositionRelationship)
//            create a map of employee to position
        val employeePositions = source.occupancies?.map { ReverseLookup(it.positionId, employeeId, EmployeePositionRelationship) }
//            create a map of employee to manager position
        val employeeManagers = source.occupancies?.filter {it.managerPositionId != null}?.map { ReverseLookup(employeeId, it.managerPositionId!!, EmployeeManagerPositionRelationship) }
//            store the relationships
        if (employeePositions != null) {
            val allLookups:MutableList<ReverseLookup> = employeePositions.toMutableList()
            if (employeeManagers != null)
            {
                allLookups.addAll(employeeManagers)
            }
            reverseLookupDataAccess.putLookups(allLookups)
        }

        return currentSubordinates.any()


    }


}