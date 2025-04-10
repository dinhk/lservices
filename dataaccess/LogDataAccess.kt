package com.qut.webservices.igalogicservices.dataaccess

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.qut.webservices.igalogicservices.models.LogEntry
import kotlinx.datetime.toJavaLocalDate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate

interface ILogDataAccess {
    /**
     * Store a set of hashes using a batch update
     */
    fun putLogMessages(igaUserId: String, correlationId: String, messages: List<String>)

    fun getLogMessages(igaUserId: String? = null,
                       correlationId: String? = null,
                       fromDate: LocalDate? = null,
                       toDate: LocalDate? = null) : List<LogEntry>
}

/**
 * Simple data access class for storage of log messages
 */
@Component
class LogDataAccess @Autowired constructor (private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) :
    ILogDataAccess {
    /**
     * Store a set of hashes using a batch update
     */
    override fun putLogMessages(igaUserId: String, correlationId: String, messages: List<String> )
    {
        val namedParameters = messages.map { MapSqlParameterSource().addValue("id", igaUserId).addValue(
            "coid",
            correlationId
        ).addValue("message", it)}
        val sql = "INSERT INTO LOG(IGA_USER_ID, CORRELATION_ID, LOG_MESSAGE) VALUES (:id, :coid, :message)"
        namedParameterJdbcTemplate.batchUpdate(sql, namedParameters.toTypedArray())
    }

    override fun getLogMessages(
            igaUserId: String?,
            correlationId: String?,
            fromDate: LocalDate?,
            toDate: LocalDate?): List<LogEntry> {

        val sql: String
        val namedParameters: SqlParameterSource

        var resolvedFromDate = fromDate
        var resolvedToDate = toDate

        if (resolvedFromDate == null) {
            resolvedFromDate = kotlinx.datetime.LocalDate(2023, 1, 1).toJavaLocalDate()
        }

        if (resolvedToDate == null) {
            resolvedToDate = kotlinx.datetime.LocalDate(2999,1,1).toJavaLocalDate()
        }

        if (igaUserId != null) {
            namedParameters = MapSqlParameterSource()
                    .addValue("id", igaUserId)
                    .addValue("fromDate", resolvedFromDate)
                    .addValue("toDate", resolvedToDate)
            sql = "SELECT * FROM LOG WHERE IGA_USER_ID = :id AND LOG_DT BETWEEN :fromDate AND :toDate"
        } else {
            namedParameters = MapSqlParameterSource()
                    .addValue("correlationId", correlationId)
                    .addValue("fromDate", resolvedFromDate)
                    .addValue("toDate", resolvedToDate)
            sql = "SELECT * FROM LOG WHERE CORRELATION_ID = :correlationId AND LOG_DT BETWEEN :fromDate AND :toDate"
        }

        return namedParameterJdbcTemplate.query(
            sql,
            namedParameters,
            LogRowMapper()
        )

    }
}
class LogRowMapper : RowMapper<LogEntry> {

    private val mapper = jacksonObjectMapper()
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): LogEntry? {

        return LogEntry(
            rs.getString("IGA_USER_ID"),
            mapper.readTree(String((rs.getClob("LOG_MESSAGE").asciiStream).readAllBytes())),
            rs.getTimestamp("LOG_DT").toLocalDateTime(),
                rs.getString("CORRELATION_ID")
        )
    }
}