package com.qut.webservices.igalogicservices.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "allowed")
class AllowedAudiencesProperties {
    lateinit var audiences: Map<String, List<String>>
}