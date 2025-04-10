package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.*
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class CurrentUnitEnrolments() : Rule<Identity, Calculated>(){

    override fun execute(source: Identity, calculations: Calculated): Array<CalculatedUnitEnrolment>? {
        val unitEnrolments = changedAttribute(source.unitEnrolments, source::unitEnrolments)
        val timeNow: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        returns(calculations::currentUnitEnrolments)

        return unitEnrolments
                .filter { !it.deleted }
                .map { CalculatedUnitEnrolment(it, isCurrent(it, timeNow), isNotYetStarted(it)) }.toTypedArray()
    }

    fun isCurrent(unitEnrolment: UnitEnrolment, timeNow: LocalDate) : Boolean
    {
        return unitEnrolment.courseStatusCode in listOf("ADMITTED","POTENTIALLYCOMPLETE","LOA")
                ||
                (unitEnrolment.courseStatusCode == "ACCEPTED" && unitEnrolment.courseCommencementDate!! >
                        timeNow.minus(1, unit = DateTimeUnit.YEAR))
    }

    fun isNotYetStarted(unitEnrolment: UnitEnrolment) : Boolean
    {
        return unitEnrolment.courseStatusCode in listOf("APPLIED","OFFERED")
    }

}