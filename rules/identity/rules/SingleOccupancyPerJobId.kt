package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import com.qut.webservices.igalogicservices.models.Occupancy
import kotlinx.datetime.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

const val Hda = "HDA"
const val Substantive = "SUB"
const val Concurrent = "CON"
private const val GRACE_POST_STAFF = 15

class OccupancyFacts(val rank: Int, val occupancy: Occupancy)
class ConsolidatedOccupancyDate(val jobId: String, val positionId: String, val occupancyTypeCode: String, val startDate:LocalDate, val endDate: LocalDate?)

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements IDM_POLL_HR.Get_Best_Occupancy
class SingleOccupancyPerJobId : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Array<Occupancy>? {
        val occupancies = changedAttribute(source.occupancies, source::occupancies)

        returns(calculations::singleOccupancyPerJobId)

        //        *Begin Fix_Occupancy_Dates implementation: calculate the earliest start and latest end dates per jobId/positionId
        val occupanciesByJobIdAndPositionId = occupancies
                .groupBy { Triple(it.jobId, it.positionId, it.occupancyTypeCode) }
        val consolidatedDates = mutableListOf<ConsolidatedOccupancyDate>()

        for (group in occupanciesByJobIdAndPositionId)
        {
            val earliestStart = group.value.minOf { it.startDate }
            val latestEnd = group.value.sortedWith(nullsLast(compareBy { it.endDate })).last().endDate
            consolidatedDates.add(ConsolidatedOccupancyDate(group.key.first, group.key.second, group.key.third, earliestStart, latestEnd))
        }
//        *End Fix_Occupancy_Dates

        val notDeletedOccupancies = occupancies.filter { !it.deleted }

        val hdaOccupanciesByJobNumber = notDeletedOccupancies
                .filter { it.occupancyTypeCode == Hda }
                .groupBy { it.jobId }
        val concAndSubsOccupanciesByJobNumber = notDeletedOccupancies
                .filter { it.occupancyTypeCode in listOf(Concurrent, Substantive) }
                .groupBy { it.jobId }

        val reducedOccupancies = mutableListOf<Occupancy>()
        val timeNow: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val gracePeriod: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.minus(15, unit = DateTimeUnit.DAY)

        //          * Begin handling concurrent and substantive occupancies *
        for (group in concAndSubsOccupanciesByJobNumber.values)
        {
            val sortedCurrentSubstantives = group
//                    current only
                    .filter { it.occupancyTypeCode.uppercase() == Substantive && it.startDate <= timeNow && (it.endDate == null || it.endDate >= gracePeriod) }
                    .sortedByDescending { it.startDate }

            val sortedFutureSubstantives = group
//                    future dated
                    .filter { it.occupancyTypeCode.uppercase() == Substantive && it.startDate > timeNow && (it.endDate == null || it.endDate > timeNow) }
                    .sortedBy { it.startDate }

            val selectedSubstantiveWithinGracePeriod = sortedCurrentSubstantives
                    .union(sortedFutureSubstantives)
                    .filter { it.endDate != null &&
                            it.endDate >= gracePeriod &&
                            it.endDate <= timeNow   }
                    .sortedBy { it.startDate }.firstOrNull()

            val selectedSubstantiveBeforeGracePeriod = sortedCurrentSubstantives
                    .union(sortedFutureSubstantives)
                    .filter { it.endDate == null || it.endDate >= timeNow }
                    .sortedBy { it.startDate }.firstOrNull()

            val selectedConcurrents = group
//                    no future dated, inside or before the grace period
                    .filter { it.occupancyTypeCode.uppercase() == Concurrent && it.startDate <= timeNow && (it.endDate == null || it.endDate >= gracePeriod) }

            val selectedConcurrentsWithinGracePeriod = selectedConcurrents
                    .filter {
                            it.endDate != null &&
                                    it.endDate >= gracePeriod &&
                                    it.endDate <= timeNow }

            val selectedConcurrentsBeforeGracePeriod = selectedConcurrents
//                    no future dated
                    .filter { it.endDate == null || it.endDate >= timeNow }

            if (selectedSubstantiveWithinGracePeriod == null && selectedSubstantiveBeforeGracePeriod == null && selectedConcurrents.isNotEmpty()) {
                val thisSelection = selectedConcurrents.sortedWith(nullsLast(compareBy { it.endDate })).last()
                val consolidatedDate = consolidatedDates.find { it.jobId == thisSelection.jobId
                        && it.positionId == thisSelection.positionId
                        && it.occupancyTypeCode == thisSelection.occupancyTypeCode}!!
                reducedOccupancies.add(Occupancy(thisSelection, consolidatedDate.startDate, consolidatedDate.endDate))
            }
            else
            {
                val concurrentAndSubstantiveOccupancies =
                        selectedConcurrentsBeforeGracePeriod.map {OccupancyFacts(1, it )}
//                                give a grace period concurrent the same weight as a substantives
                        .union(selectedConcurrentsWithinGracePeriod.map {OccupancyFacts(2, it )}).toMutableList()

                if (selectedSubstantiveBeforeGracePeriod != null) {
                    concurrentAndSubstantiveOccupancies.add(OccupancyFacts(2, selectedSubstantiveBeforeGracePeriod))
                }

                if (selectedSubstantiveWithinGracePeriod != null) {
                    concurrentAndSubstantiveOccupancies.add(OccupancyFacts(3, selectedSubstantiveWithinGracePeriod))
                }

                val selectedConcurrentOrSubstantive = concurrentAndSubstantiveOccupancies.sortedWith(
                        compareBy<OccupancyFacts> {it.rank }
                                .thenComparing(nullsFirst(compareByDescending { it.occupancy.endDate }))
//                        .thenByDescending { it.occupancy.endDate }
                ).firstOrNull()
//                not possible to be null here (thanks Kotlin)
                if (selectedConcurrentOrSubstantive != null) {
                    val thisSelection = selectedConcurrentOrSubstantive.occupancy
                    val consolidatedDate = consolidatedDates.find { it.jobId == thisSelection.jobId
                            && it.positionId == thisSelection.positionId
                            && it.occupancyTypeCode == thisSelection.occupancyTypeCode}!!
                    reducedOccupancies.add(Occupancy(thisSelection, consolidatedDate.startDate, consolidatedDate.endDate))
                }
            }
        }
//          * End handling concurrent and substantive occupancies *

//          * Begin handling HDA occupancies *

        for (group in hdaOccupanciesByJobNumber.values) {
            val selectedHdaOccupancy = group.
//                    not future dated occupancies
                    filter { it.startDate <= timeNow && (it.endDate == null || it.endDate >= gracePeriod)  }.
                    sortedByDescending { it.startDate }.firstOrNull()
            if (selectedHdaOccupancy != null) {
                val consolidatedDate = consolidatedDates.find { it.jobId == selectedHdaOccupancy.jobId
                        && it.positionId == selectedHdaOccupancy.positionId
                        && it.occupancyTypeCode == selectedHdaOccupancy.occupancyTypeCode}!!
                reducedOccupancies.add(Occupancy(selectedHdaOccupancy, consolidatedDate.startDate, consolidatedDate.endDate))
            }
        }
//        *End handling HDA occupancies*

        return reducedOccupancies.toTypedArray()
    }
}