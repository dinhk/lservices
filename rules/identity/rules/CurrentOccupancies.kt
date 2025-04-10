package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.models.Occupancy
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class CurrentOccupancies : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Array<Occupancy>? {
        val occupancies = changedAttribute(calculations.singleOccupancyPerJobId, calculations::singleOccupancyPerJobId)
        returns(calculations::currentOccupancies)

        return getCurrentOccupancies(occupancies)
    }

    fun getCurrentOccupancies(
        occupancies: Array<Occupancy>,
    ): Array<Occupancy> {
        val timeNow: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val currentOccupancies = occupancies.filter {
            !it.deleted
                    && it.startDate <= timeNow
                    && (it.endDate == null || it.endDate >= timeNow.minus(1, DateTimeUnit.DAY))
                    && it.classificationCode != "PRACT"
        }.toTypedArray()
        return currentOccupancies
    }
}