package com.qut.webservices.igalogicservices.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource

@Configuration
class LdapConfig {

    @Value("\${spring.ldap.urls}")
    private lateinit var ldapUrl: String

    @Value("\${spring.ldap.base}")
    private lateinit var ldapBase: String

    @Value("\${spring.ldap.username}")
    private lateinit var ldapUserDn: String

    @Value("\${spring.ldap.password}")
    private lateinit var ldapPassword: String

    @Bean
    fun contextSource(): LdapContextSource {
        val contextSource = LdapContextSource()
        contextSource.setUrl(ldapUrl)
        contextSource.setBase(ldapBase)
        contextSource.userDn = ldapUserDn
        contextSource.password = ldapPassword
        return contextSource
    }

    @Bean
    fun ldapTemplate(): LdapTemplate {
        val ldapTemplate = LdapTemplate(contextSource())
        ldapTemplate.setIgnorePartialResultException(true)
        return ldapTemplate
    }
}