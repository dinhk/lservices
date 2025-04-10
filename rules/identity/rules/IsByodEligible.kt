package com.qut.webservices.igalogicservices.rules.identity.rules
import com.qut.webservices.igalogicservices.models.Calculated
import com.qut.webservices.igalogicservices.models.Identity
import com.qut.webservices.igalogicservices.rules.core.Rule
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/*  Retrieved the following list using the SQL command
    select attribute_value from cds_system_attribute
    where attribute_cd = 'ADOBE_STUDENT_BYOD_UNIT_CODE'
 */
private val ADOBE_STUDENT_BYOD_UNIT_CODE = listOf("DTB102","CCB102","KPB116","CJB203","UXB230","KNB225","KNB226","KNB227","KNB320","KPB101","KPB121","KPB122","KPB220","KPB221","KPB222","KRB121","KRB221","KRB308","KVB104","KVB104","KVB127","KVB222","KVB227","KVB327","KYB101","KYB302","KYB303","UXB133","UXH331","KVB104","DFB104","DAB102","DAB202","DAB302","DFB111","DFB208","DFB211","DFB216","DFB305","DFB311","DFH801","DLB102","DLB204","DLB302","DLB303","DLH800","DNB111","DNB212","DNB311","DNB312","DNH803","DNH804","DTB204","DTB305","DVB102","DVB203","DVB303","DVB305","DXB111","DXB205","DXB212","DXB311","DXH602","DXH801","DXH803","DYB102","DYB113","DYB122","DYB123","DYB124","DYB201","DYB301","KDB223","KKB285","KKB385","KKP625","KKP626","KNB127","KNB135","KNB136","KNB137")
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
//Implements SVC_ADOBE_UTIL.GENERATE_STUDENT_BYOD_FLAG
class IsByodEligible() : Rule<Identity, Calculated>() {

    override fun execute(source: Identity, calculations: Calculated): Boolean {
        val currentUnits = changedAttribute(calculations.currentUnitEnrolments, calculations::currentUnitEnrolments)

        returns(calculations::byod)

        return currentUnits.any{ it.isCurrent && it.unitCode in ADOBE_STUDENT_BYOD_UNIT_CODE }
    }


}