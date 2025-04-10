package com.qut.webservices.igalogicservices.dataaccess

import com.qut.webservices.igalogicservices.models.Reprocess
import kotlinx.datetime.toJavaLocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.sql.Types

/**
 * Simple data access class for insertions into the Reprocessing table
 */
@Component
class ReprocessingDataAccess @Autowired constructor(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate)
{
    /**
     * Store a set of hashes using a batch update
     */
    fun putReprocessingData(toReprocess: List<Reprocess>) {

        val unhandledType = toReprocess.filter { it.entityType != "Occupancy" }
        if (unhandledType.any())
        {
            throw NotImplementedError("The reprocessing data contains unhandled types.")
        }
        val occupancyTasks = toReprocess.filter { it.entityType == "Occupancy" }
        val namedParameters = occupancyTasks.map {
            MapSqlParameterSource()
                .addValue("keys", it.naturalKeys)
                .addValue("date", Timestamp.valueOf(it.reprocessDate.toJavaLocalDateTime()), Types.TIMESTAMP)
        }
        val sql =
            "MERGE INTO REPROCESSING_OCCUPANCY r USING DUAL ON (EMPLOYEE_ID = :keys AND REPROCESS_TS = :date) WHEN NOT MATCHED THEN INSERT (EMPLOYEE_ID, REPROCESS_TS) VALUES (:keys, :date)"
        namedParameterJdbcTemplate.batchUpdate(sql, namedParameters.toTypedArray())
    }
}