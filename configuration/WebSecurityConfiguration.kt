package com.qut.webservices.igalogicservices.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


/* For more information about oauth with spring boot,
   see https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
*/
@Configuration
@EnableWebSecurity
@Profile("!test")
class WebSecurityConfiguration(
    private val allowedAudiencesProperties: AllowedAudiencesProperties
) {

    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private lateinit var jwkSetUri: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            csrf { disable() }
            headers { frameOptions { disable() } }

            authorizeHttpRequests {
                authorize(HttpMethod.GET, "/health", permitAll)
                authorize(HttpMethod.GET, "/v3/api-docs/**", permitAll)
                authorize(HttpMethod.GET, "/swagger-ui/**", permitAll)
                authorize(HttpMethod.GET, "/swagger-ui.html", permitAll)
                authorize(anyRequest, authenticated)
            }

            oauth2ResourceServer {
                jwt {
                    jwtAuthenticationConverter = jwtAuthenticationConverter()
                }
            }
        }

        // Add the custom JwtAuthenticationFilter before the UsernamePasswordAuthenticationFilter
        http.addFilterAfter(customJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_")
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles")

        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        return jwtAuthenticationConverter
    }

    @Bean
    fun customJwtAuthenticationFilter(): CustomJwtAuthorisationFilter {
        return CustomJwtAuthorisationFilter(jwtDecoder(), allowedAudiencesProperties.audiences)
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }
}