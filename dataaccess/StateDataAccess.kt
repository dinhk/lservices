package com.qut.webservices.igalogicservices.dataaccess

import com.qut.webservices.igalogicservices.models.StateObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.sql.SQLException

class StateRowMapper : RowMapper<StateObject> {
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): StateObject {
        return StateObject(
            rs.getString("IGA_USER_ID"),
            rs.getString("COLLECTION_NAME"),
            rs.getNString("OBJECT_KEY_HASH"),
            rs.getNString("OBJECT_JSON"), 
            rs.getNString("OBJECT_STATE_CD"),
            rs.getDate("INSERTED_DT"),
            rs.getDate("UPDATED_DT")
            )
    }
}

/**
 * Simple data access class for storage and retrieval of hashes
 */
@Component
class StateDataAccess @Autowired constructor (private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate){

    /**
     * Get all state objects for the current identity
     */
    fun getStateObjects(igaUserId: String) : List<StateObject>
    {
        val namedParameters: SqlParameterSource = MapSqlParameterSource().addValue("id", igaUserId)
        val sql = "SELECT * FROM IGA_STATE WHERE IGA_USER_ID = :id"

        val stateObjects: MutableList<StateObject> = namedParameterJdbcTemplate.query(
            sql,
            namedParameters,
            StateRowMapper()
        )

        return stateObjects
    }

    /**
     * Store a set of hashes using a batch update
     */
    fun putStateObjects(stateObjects: List<StateObject>)
    {
        val namedParameters = stateObjects.map {
            MapSqlParameterSource()
                    .addValue("igaUserId", it.igaUserId)
                    .addValue("collection", it.collectionName,)
                    .addValue("hash", it.objectKeyHash,)
                    .addValue("json", it.objectJson,)
                    .addValue("statusCode", it.objectStateCode,)
        }
        val sql = "MERGE INTO IGA_STATE oh USING DUAL ON (IGA_USER_ID = :igaUserId AND COLLECTION_NAME = :collection AND OBJECT_KEY_HASH = :hash) when not matched then INSERT (IGA_USER_ID, COLLECTION_NAME, OBJECT_KEY_HASH, OBJECT_JSON, OBJECT_STATE_CD) VALUES (:igaUserId, :collection, :hash, :json, :statusCode) WHEN MATCHED THEN UPDATE SET OBJECT_JSON = :json, OBJECT_STATE_CD =:statusCode"
        namedParameterJdbcTemplate.batchUpdate(sql, namedParameters.toTypedArray())
    }

    /**
     * Update the write status of a State Object
     */
    fun updateStateObjectStatus(igaUserId: String, objectKeyHash: String, status:String)
    {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
                .addValue("igaUserId", igaUserId)
                .addValue("keyHash", objectKeyHash)
                .addValue("status", status)
        val sql = "UPDATE IGA_STATE SET OBJECT_STATE_CD = :status WHERE IGA_USER_ID = :igaUserId AND OBJECT_KEY_HASH =:keyHash"
        namedParameterJdbcTemplate.update(sql, namedParameters)
    }
}