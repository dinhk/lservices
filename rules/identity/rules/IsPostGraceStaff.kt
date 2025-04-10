package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import kotlinx.datetime.*
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

private const val GRACE_POST_STAFF = 15
private val EXCLUDED_CLASSIFICATIONS = listOf<String>("PRACT")

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsPostGraceStaff() : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Boolean? {
        val occupancies = changedAttribute(calculations.singleOccupancyPerJobId, calculations::singleOccupancyPerJobId)

        returns(calculations::postGraceStaff)

        for(occupancy in occupancies) {
            if (isEndDateWithinGracePeriod(occupancy.endDate) && !isClassificationExcluded(occupancy.classificationCode)) {
                return true
            }
        }
        return false
    }

    fun isEndDateWithinGracePeriod(endDate: LocalDate?): Boolean {
        val timeNow: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        return endDate != null && endDate < timeNow && endDate > timeNow.minus(GRACE_POST_STAFF, DateTimeUnit.DAY)
    }

    fun isClassificationExcluded(classification: String): Boolean {
        return classification.uppercase() in EXCLUDED_CLASSIFICATIONS
    }
}