package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class IsOffShoreInternationalStudent() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Boolean? {

        val citizenship = changedAttribute(source.citizenship, source::citizenship)
        val citizenshipCode = changedNullableAttribute(citizenship.citizenshipCode, citizenship::citizenshipCode)
        val currentStudent = changedAttribute(calculations.currentStudent, calculations::currentStudent)
        val intermitStudent = changedAttribute(calculations.intermitStudent, calculations::intermitStudent)
        val currentApplicant = changedAttribute(calculations.currentApplicant, calculations::currentApplicant)
        val graduandStudent = changedAttribute(calculations.graduandStudent, calculations::graduandStudent)

        returns(calculations::offshoreInternationalStudent)

        return if (citizenshipCode == null) {
            false
        } else {
            (currentStudent || intermitStudent || currentApplicant || graduandStudent) && citizenshipCode == "5"
        }
    }


}