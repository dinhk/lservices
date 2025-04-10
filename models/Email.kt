package com.qut.webservices.igalogicservices.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.qut.webservices.igalogicservices.rules.core.Sensitive


/**
 * 
 * @param contactType
 * @param address
 * @param contactPreference
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Email (
    val contactType: String,
    @Sensitive
    @JsonView(Sensitive::class)
    val address: String,
    val contactPreference: String,
) {

}

