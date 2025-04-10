package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class CLevelDescHierarchy : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): String {
        changedNullableAttribute(source.employeeId, source::employeeId)
        val occupancies = changedAttribute(calculations.singleOccupancyPerJobId, calculations::singleOccupancyPerJobId)
        returns(calculations::clevelDescHierarchy)

        val currentOccupancies = CurrentOccupancies().getCurrentOccupancies(occupancies)

        // Return an empty string if currentOccupancies is empty
        if (currentOccupancies.isEmpty()) {
            return ""
        }

        val descriptions = currentOccupancies.flatMap {
            listOfNotNull(
                it.clevel2Description?.stripSpecialChars(),
                it.clevel3Description?.stripSpecialChars(),
                it.clevel5Description?.stripSpecialChars()
            )
        }.distinct().sorted()

        return descriptions.joinToString(PIPE_DELIMITER)
    }

    private fun String.stripSpecialChars(): String {
        return this.replace(PIPE_DELIMITER, "")
    }

    companion object {
        const val PIPE_DELIMITER = "|"
    }
}
