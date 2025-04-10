package com.qut.webservices.igalogicservices.models

class ExecuteRulesRequest (val correlationId: String,
                           val changedAttributesString: String? = null,
                           val changedAttributes: Array<String>?,
                           val identity: IdentityDto,
                           val treatNullsAsEmpty: Boolean = true,
                           val generateRolesDelta: Boolean = false
        )
{
}