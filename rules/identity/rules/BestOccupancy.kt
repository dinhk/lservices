package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import com.qut.webservices.igalogicservices.models.Occupancy
import com.qut.webservices.igalogicservices.models.Reprocess
import kotlinx.datetime.atTime
import kotlinx.datetime.toJavaLocalDate
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements IDM_UTIL.Get_Best_Occupancy
class BestOccupancy : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Occupancy? {
        val currentOccupancies = changedAttribute(calculations.currentOccupancies, calculations::currentOccupancies)

        returns(calculations::bestOccupancy)

        // In the current state the procedure Fix_Occupancy_Dates just selects the min/max dates for a given employee#, job# and position#
        // We are now using the HRM webservice which normalises start/end dates where there is a concurrent occupancy, as below
        //      S1                 SUB                    E1
        //      |------------------------------------------|
        //                 S2      CON     E2
        //                |------------------|
        //      Outputs:
        //         SUB            CON               SUB
        //      S1   S2-1||S2              E2||E2+1      E1
        //      |--------||------------------||------------|
        //   Given the ranking in the procedure, Concurrent and Substantive occupancies have the same weight, so the end date will is used to rank the Concurrent and the
        //   Substantive. The Substantive end date returned by the HRM Webservice is always later than the Concurrent end-date (by the diagram above), so th Substantive
        //   would be placed ahead of the concurrent. This means that the date normalising performed by the HRM web service will not impact the Best Occupancy calculation

        val sortedOccupancies = currentOccupancies.sortedWith(
                nullsLast(compareBy<Occupancy>(
                        {
                            when (it.awardCode.uppercase()) {
                                "SSGA" -> 1
                                "ASA" -> 2
                                "HEGSS" -> 2
                                "INTC" -> 2
                                "VISIT" -> 3
                                "QUT" -> 4
                                "SMRCC" -> 5
                                "NOAWRD" -> 6
                                else -> 7
                            }
                        },
                        {
                            when (it.classificationCode.uppercase()) {
                                "HWA10" -> 1
                                "HEWA9" -> 2
                                "HEWA8" -> 3
                                "HEWA7" -> 4
                                "HEWA6" -> 5
                                "HEWA5" -> 6
                                "HEWA4" -> 7
                                "HEWA3" -> 8
                                "HEWA2" -> 9
                                "HEWA1" -> 10
                                "STY4" -> 11
                                "STY3" -> 12
                                "STY2" -> 13
                                "HECSU" -> 14
                                "HEC15" -> 15
                                "LEVE" -> 1
                                "LEVD" -> 2
                                "LEVC" -> 3
                                "LEVB" -> 4
                                "LEVA" -> 5
                                else -> 50
                            }
                        },
                        {
                            when (it.employmentStatus.uppercase()) {
                                "OFT" -> 1
                                "FFT" -> 1
                                "OPT" -> 2
                                "FPT" -> 2
                                "CASA" -> 3
                                "SASA" -> 3
                                "CASG" -> 3
                                "VISIT" -> 4
                                else -> 5
                            }
                        },
                        {
                            when (it.occupancyTypeCode.uppercase()) {
                                "HDA" -> 1
                                "CON" -> 2
                                "SUB" -> 2
                                else -> null//Originally the Oracle SP does not provide a default value, the SP will provide NULL by default which by default will appear at the end
                            }
                        }
                ).thenByDescending { it.endDate }
                        .thenByDescending { it.startDate }
                )
        )
        if (sortedOccupancies.isEmpty()) {
            return null
        }
        else {
//            create a reprocessing event for end date of the occupancy
//            NOTE: this code should not be necessary because the Occupancy poller generates an event when an occupancy is at its start or end date
//            val bestOccupancy = sortedOccupancies.first()
//            if (bestOccupancy.endDate != null) {
//                calculations.scheduledReprocessing.add(Reprocess("Occupancy", source.employeeId!!, bestOccupancy.endDate.atTime(1,0,0)))
//            }
            return sortedOccupancies[0] // Return the "best" occupancy based on the sort criteria executed
        }

    }
}