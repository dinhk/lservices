package com.qut.webservices.igalogicservices.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.qut.webservices.igalogicservices.rules.core.Sensitive


/**
 * 
 * @param contactType
 * @param contactPreference
 * @param addressLine1
 * @param addressLine2
 * @param addressLine3
 * @param state
 * @param postCode
 * @param countryCode
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Address (
    val contactType: String,
    val contactPreference: String,
    @Sensitive
    @JsonView(Sensitive::class)
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val addressLine3: String? = null,
    val state: String? = null,
    val postCode: String? = null,
    val countryCode: String? = null,
) {

}

