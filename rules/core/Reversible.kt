package com.qut.webservices.igalogicservices.rules.core

//An annotation which should be added to an attribute whose pre-hash value is short - and could theoretically be reversed (such as date of birth)
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Reversible
