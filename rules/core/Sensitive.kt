package com.qut.webservices.igalogicservices.rules.core

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@JacksonAnnotationsInside
annotation class Sensitive


class NotSensitive
