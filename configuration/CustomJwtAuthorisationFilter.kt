package com.qut.webservices.igalogicservices.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

const val GENERATE_USERNAME_VISITOR_ENDPOINT = "/generate-username/visitor"
const val GENERATE_USERNAME_STAFF_ENDPOINT = "/generate-username/staff"
const val GENERATE_USERNAME_STUDENT_ENDPOINT = "/generate-username/student"
const val REGISTER_USERNAME_ENDPOINT = "/register-username"

class CustomJwtAuthorisationFilter(
    private val jwtDecoder: JwtDecoder,
    private val allowedAudiences: Map<String, List<String>>
) : OncePerRequestFilter() {

    @Throws(IOException::class, ServletException::class)
    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val endpoint = request.requestURI
        val token = request.getHeader("Authorization")?.substring(7)
        val jwt = token?.let { jwtDecoder.decode(it) }
        val audience = jwt?.audience?.firstOrNull()

        if (systemPaths.any { endpoint.contains(it) }) {
            filterChain.doFilter(request, response)
            return
        } else if (audience != null && isAudienceAllowed(endpoint, audience)) {
            val authentication: Authentication = JwtAuthenticationToken(jwt)
            SecurityContextHolder.getContext().authentication = authentication
            logger.info("User with audience $audience is authorised to access $endpoint")
        } else {
            val audienceString = audience?.let { " with audience $it"} ?: ""
            logger.warn("User$audienceString is not authorised to access $endpoint")
            response.status = HttpServletResponse.SC_FORBIDDEN
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isAudienceAllowed(endpoint: String, audience: String?): Boolean {
        return when {
//            systemPaths.any { endpoint.contains(it) } -> true
            endpoint.contains(GENERATE_USERNAME_VISITOR_ENDPOINT) -> allowedAudiences["visitor"]?.contains(audience) == true
            endpoint.contains(GENERATE_USERNAME_STAFF_ENDPOINT) -> allowedAudiences["staff"]?.contains(audience) == true
            endpoint.contains(GENERATE_USERNAME_STUDENT_ENDPOINT) -> allowedAudiences["student"]?.contains(audience) == true
            endpoint.contains(REGISTER_USERNAME_ENDPOINT) -> allowedAudiences["register"]?.contains(audience) == true
            logicservicesPaths.any { endpoint.startsWith(it) } -> allowedAudiences["logicservices"]?.contains(audience) == true
            else -> false
        }
    }

    private val logicservicesPaths = listOf(
        "/igaState",
        "/hash",
        "/log",
        "/executeRules",
        "/resolveDataRequirements"
    )

    private val systemPaths = listOf(
        "/health",
        "/swagger",
        "/v3/api-docs",
        "/favicon.ico"
    )
}
