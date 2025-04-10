package com.qut.webservices.igalogicservices.models

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExecuteRulesResponse (
    val correlationId: String,
    val enrichedIdentity: EnrichedIdentity? = null,
    val message: String? = null,
) {
}


