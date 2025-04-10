package com.qut.webservices.igalogicservices.dataaccess

import com.qut.webservices.igalogicservices.models.ReverseLookup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.sql.SQLException

class ReverseLookupRowMapper : RowMapper<ReverseLookup> {
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): ReverseLookup {
        return ReverseLookup(
            rs.getString("CHILD_KEY"),
            rs.getString("PARENT_KEY"),
            rs.getString("TYPE"),
            rs.getDate("INSERTED_DT").toLocalDate(),
            rs.getDate("UPDATED_DT").toLocalDate()
            )
    }
}

interface IReverseLookupDataAccess {
    fun getByParent(parentKey: String, type: String) : List<ReverseLookup>

    fun getByParent(parentKeys: List<String>, type: String) : List<ReverseLookup>

    fun getByChild(childKey: String, type: String) : List<ReverseLookup>

    fun getByChild(childKeys: List<String>, type: String) : List<ReverseLookup>

    fun getByChildOrParent(key: String, type: String) : List<ReverseLookup>

    fun putLookups(lookups: List<ReverseLookup>)

    fun clearByChild(childKey: String, type: String): Int
    fun clearByParent(parentKey: String, type: String): Int
}

class ReverseLookupDataAccessStub () : IReverseLookupDataAccess {
    override fun getByParent(parentKey: String, type: String): List<ReverseLookup> {
        return listOfNotNull()
    }

    override fun getByParent(parentKeys: List<String>, type: String): List<ReverseLookup> {
        return listOfNotNull()
    }

    override fun getByChild(childKey: String, type: String): List<ReverseLookup> {
        return listOfNotNull()
    }

    override fun getByChild(childKeys: List<String>, type: String): List<ReverseLookup> {
        return listOfNotNull()
    }

    override fun getByChildOrParent(key: String, type: String): List<ReverseLookup> {
        return listOfNotNull()
    }

    override fun putLookups(lookups: List<ReverseLookup>) {
        return
    }

    override fun clearByChild(childKey: String, type: String): Int {
        return 0
    }

    override fun clearByParent(parentKey: String, type: String): Int {
        return 0
    }


}

/**
 * Simple data access class for storage and retrieval of reverse lookups
 */
@Component
class ReverseLookupDataAccess @Autowired constructor (
        private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
        private val jdbcTemplate: JdbcTemplate) : IReverseLookupDataAccess {


    override fun getByParent(parentKey: String, type: String) : List<ReverseLookup>
    {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
                .addValue("PARENT_KEY", parentKey)
                .addValue("TYPE", type)
        val sql = "SELECT * FROM REVERSE_LOOKUP WHERE PARENT_KEY = :PARENT_KEY AND TYPE = :TYPE"

        val reverseLookups: MutableList<ReverseLookup> = namedParameterJdbcTemplate.query(
            sql,
            namedParameters,
            ReverseLookupRowMapper()
        )

        return reverseLookups
    }

    override fun getByParent(parentKeys: List<String>, type: String) : List<ReverseLookup>
    {
        if (parentKeys.isEmpty())
        {
            return emptyList()
        }
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
                .addValue("PARENT_KEYS", parentKeys)
                .addValue("TYPE", type)
        val sql = "SELECT * FROM REVERSE_LOOKUP WHERE PARENT_KEY IN (:PARENT_KEYS) AND TYPE = :TYPE"

        val reverseLookups: MutableList<ReverseLookup> = namedParameterJdbcTemplate.query(
                sql,
                namedParameters,
                ReverseLookupRowMapper()
        )

        return reverseLookups
    }

    override fun getByChild(childKey: String, type: String) : List<ReverseLookup>
    {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
                .addValue("CHILD_KEY", childKey)
                .addValue("TYPE", type)
        val sql = "SELECT * FROM REVERSE_LOOKUP WHERE CHILD_KEY = :CHILD_KEY AND TYPE = :TYPE"

        val reverseLookups: MutableList<ReverseLookup> = namedParameterJdbcTemplate.query(
                sql,
                namedParameters,
                ReverseLookupRowMapper()
        )

        return reverseLookups
    }

    override fun getByChild(childKeys: List<String>, type: String) : List<ReverseLookup>
    {
        if (childKeys.isEmpty())
        {
            return emptyList()
        }
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
                .addValue("CHILD_KEYS", childKeys)
                .addValue("TYPE", type)
        val sql = "SELECT * FROM REVERSE_LOOKUP WHERE CHILD_KEY IN (:CHILD_KEYS) AND TYPE = :TYPE"

        val reverseLookups: MutableList<ReverseLookup> = namedParameterJdbcTemplate.query(
                sql,
                namedParameters,
                ReverseLookupRowMapper()
        )

        return reverseLookups
    }

    override fun getByChildOrParent(key: String, type: String) : List<ReverseLookup>
    {
        val namedParameters: SqlParameterSource = MapSqlParameterSource()
                .addValue("KEY", key)
                .addValue("TYPE", type)
        val sql = "SELECT * FROM REVERSE_LOOKUP WHERE (PARENT_KEY = :KEY OR CHILD_KEY = :KEY) AND TYPE = :TYPE"

        val reverseLookups: MutableList<ReverseLookup> = namedParameterJdbcTemplate.query(
                sql,
                namedParameters,
                ReverseLookupRowMapper()
        )

        return reverseLookups
    }

    override fun putLookups(lookups: List<ReverseLookup>)
    {
        val namedParameters = lookups.map {
            MapSqlParameterSource()
                    .addValue("CHILD_KEY", it.childKey)
                    .addValue("PARENT_KEY", it.parentKey)
                    .addValue("TYPE", it.type)
        }
        val sql = "MERGE INTO REVERSE_LOOKUP r USING DUAL ON (CHILD_KEY = :CHILD_KEY AND PARENT_KEY = :PARENT_KEY AND TYPE = :TYPE) WHEN NOT MATCHED THEN INSERT (CHILD_KEY, PARENT_KEY, TYPE) VALUES (:CHILD_KEY, :PARENT_KEY, :TYPE)"
        namedParameterJdbcTemplate.batchUpdate(sql, namedParameters.toTypedArray())
    }

    override fun clearByChild(childKey: String, type: String): Int {
        val sql = "DELETE FROM REVERSE_LOOKUP WHERE CHILD_KEY = :CHILD_KEY AND TYPE = :TYPE"
        return jdbcTemplate.update(sql, childKey, type)
    }

    override fun clearByParent(parentKey: String, type: String): Int {
        val sql = "DELETE FROM REVERSE_LOOKUP WHERE PARENT_KEY = :PARENT_KEY AND TYPE = :TYPE"
        return jdbcTemplate.update(sql, parentKey, type)
    }

}