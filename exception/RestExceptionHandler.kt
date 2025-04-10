package com.qut.webservices.igalogicservices.exception

import com.qut.webservices.igalogicservices.models.ErrorResponse
import org.springframework.beans.ConversionNotSupportedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.sql.SQLException

/**
 * Provisional handler class: returns full exception details to caller
 */
@ControllerAdvice
class RestExceptionHandler : ResponseEntityExceptionHandler() {

    @Autowired
    var env: Environment? = null

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.error(ex.message, ex)
        val errors = ex.bindingResult.fieldErrors.associate { "error" to it.defaultMessage }
        return ResponseEntity(errors, HttpStatus.BAD_REQUEST)
    }

    override fun handleHttpRequestMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.error(ex.message, ex)
        return ResponseEntity(ErrorResponse(ex, env), HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    override fun handleNoHandlerFoundException(
        ex: NoHandlerFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.error(ex.message, ex)
        return ResponseEntity(ErrorResponse(ex, env), HttpHeaders(), HttpStatus.NOT_FOUND)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.error(ex.message, ex)
        return ResponseEntity(ErrorResponse(ex, env), HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    override fun handleConversionNotSupported(
        ex: ConversionNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.error(ex.message, ex)
        return ResponseEntity(ErrorResponse(ex, env), HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    override fun handleHttpMessageNotWritable(
        ex: HttpMessageNotWritableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.error(ex.message, ex)
        return ResponseEntity(ErrorResponse(ex, env), HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    //Ideally a custom exception would be created instead of a generic exception thrown
    @ExceptionHandler(Exception::class, NullPointerException::class, IllegalArgumentException::class)
    fun handleExceptionsWithBadRequest(ex: Exception): ResponseEntity<Any> {
        logger.error(ex.message, ex)
        return ResponseEntity(ErrorResponse(ex, env), HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(SQLException::class, NotImplementedError::class, IllegalStateException::class)
    fun handleExceptionsWithInternalServerError(ex: Exception): ResponseEntity<Any> {
        logger.error(ex.message, ex)
        return ResponseEntity(ErrorResponse(ex, env), HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
