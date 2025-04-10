package com.qut.webservices.igalogicservices.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.qut.webservices.igalogicservices.rules.core.Sensitive
import kotlinx.datetime.LocalDate


/**
 * 
 * @param roleCode
 * @param issueLevel
 * @param cardaxNumber 
 * @param cardaxIssueLevel
 * @param issueDate
 * @param frontBarcode 
 * @param cardBarcode
 * @param expiryDate
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Card (
    val roleCode: String,
    val issueLevel: Int? = null,
    val cardaxNumber: Int? = null,
    val cardaxIssueLevel: Int? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    val issueDate: LocalDate? = null,
    @Sensitive
    @JsonView(Sensitive::class)
    val frontBarcode: String,
    @Sensitive
    @JsonView(Sensitive::class)
    val cardBarcode: String? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    val expiryDate: LocalDate
) {

}

