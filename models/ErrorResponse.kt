package com.qut.webservices.igalogicservices.models

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.core.env.Environment

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    var message: String? = "Unknown Error"
) {
    constructor(ex: Throwable, env: Environment?) : this() {
        val sb = StringBuilder()
        sb.append(ex.message)

        if (env != null && ("local" in env.activeProfiles || "qas" in env.activeProfiles)) {
            sb.appendLine()
            sb.append(ex.stackTraceToString())
        }
        this.message = sb.toString()
    }
}
