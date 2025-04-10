package com.qut.webservices.igalogicservices.rules.identity.rules

import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_IDENTITY_UTIL.Generate_Display_Name
class GenerateLocationText : Rule<Identity, Calculated>() {
    override fun execute(source: Identity, calculations: Calculated): Any? {
        changedNullableAttribute(source.employeeId, source::employeeId)
        val location = changedAttribute(source.location, source::location)
        val campus = changedNullableAttribute(location.campus, location::campus)?.toTitleCase()
        val buildingName = changedNullableAttribute(location.buildingName, location::buildingName)
        val floorName = changedNullableAttribute(location.floorName, location::floorName)
        val roomId = changedNullableAttribute(location.roomId, location::roomId)

        returns(calculations::locationText)

        val locationTextArgs = mutableListOf<String>().apply {
            if (!campus.isNullOrBlank()) add(campus)
            if (!buildingName.isNullOrBlank()) add(buildingName)
            if (!floorName.isNullOrBlank()) add(floorName)
            if (!roomId.isNullOrBlank()) add(roomId)
        }

        return locationTextArgs.joinToString(separator = ", ")
    }

    private fun String.toTitleCase(): String {
        // change the 1st character of each word to uppercase if it's lowercase
        return lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase() }
        }
    }

}
