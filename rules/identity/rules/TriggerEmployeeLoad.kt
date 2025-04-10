package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
/*
This rule instructs Boomi to load/reload occupancies, location data and cards when en employeeId is added or changes
 */
class TriggerEmployeeLoad() : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Boolean? {
        changedNullableAttribute(source.employeeId, source::employeeId)
        requiredAttribute(source.occupancies, source::occupancies)
        requiredAttribute(source.location, source::location)
        requiredAttribute(source.cards, source::cards)

        returns(calculations::employeeReloaded)

        return true
    }
}