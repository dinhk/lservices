package com.qut.webservices.igalogicservices.dataaccess

import com.qut.webservices.igalogicservices.models.ObjectHash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.sql.SQLException

class HashRowMapper : RowMapper<ObjectHash> {
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): ObjectHash? {
        return ObjectHash(
            rs.getString("IGA_USER_ID"),
            rs.getString("OBJECT_PATH"),
            rs.getNString("OBJECT_HASH"),
            rs.getDate("INSERTED_DT"),
            rs.getDate("UPDATED_DT")
            )
    }
}

/**
 * Simple data access class for storage and retrieval of hashes
 */
@Component
class HashDataAccess @Autowired constructor (private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate, private val jdbcTemplate: JdbcTemplate){

    /**
     * Get all hashes for the current identity
     */
    fun getHashes(igaUserId: String) : Map<String,String?>
    {
        val namedParameters: SqlParameterSource = MapSqlParameterSource().addValue("id", igaUserId)
        val sql = "SELECT * FROM OBJECT_HASH WHERE IGA_USER_ID = :id"

        val hashes: MutableList<ObjectHash> = namedParameterJdbcTemplate.query(
            sql,
            namedParameters,
            HashRowMapper()
        )

        return hashes.associate  { Pair(it.objectPath, it.objectHash) }
    }

    /**
     * Store a set of hashes using a batch update
     */
    fun putHashes(igaUserId: String, hashes: Map<String,String?>)
    {
        val namedParameters = hashes.map { MapSqlParameterSource().addValue("id", igaUserId).addValue("path", it.key,).addValue("hash", it.value,)}
        val sql = "MERGE INTO OBJECT_HASH oh USING DUAL ON (IGA_USER_ID = :id AND OBJECT_PATH = :path) when not matched then INSERT (IGA_USER_ID, OBJECT_PATH, OBJECT_HASH) VALUES (:id, :path, :hash) WHEN MATCHED THEN UPDATE SET OBJECT_HASH = :hash"
        namedParameterJdbcTemplate.batchUpdate(sql, namedParameters.toTypedArray())
    }

    fun clearHashes(igaUserId: String) : Int {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
                .addValue("igaUserId", igaUserId)
        val sql = "DELETE FROM OBJECT_HASH WHERE IGA_USER_ID = :igaUserId"
//        val count = namedParameterJdbcTemplate.update(sql, namedParameters)
        val count = jdbcTemplate.update(sql, igaUserId)
        return count
    }
}