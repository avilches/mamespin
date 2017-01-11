package app

import groovy.sql.GroovyRowResult
import groovy.sql.Sql

import javax.sql.DataSource


class DbLogic {
    int cleanTimeoutMinutes = 60
    DataSource ds

    DbLogic(DataSource ds, int cleanTimeoutMinutes) {
        this.ds = ds
        this.cleanTimeoutMinutes = cleanTimeoutMinutes
    }

    def listUsers() {
        withSql { Sql sql ->
            sql.eachRow("select * from user") { row ->
                println row[0]
            }
        }

    }

    def withSql(Closure closure) {
        Sql sql
        try {
            sql = new Sql(ds)
            return closure.call(sql)
        } finally {
            try {
                sql.close()
            } catch (e) {
            }
        }
    }

    void cleanTokens(int timeout = cleanTimeoutMinutes) {
        withSql { Sql sql ->
            Date until
            use(groovy.time.TimeCategory) {
                until = new Date() - timeout.minutes
            }
            int cleaned = sql.executeUpdate("update user_request set state = 'unlocked', downloaded = 0, last_updated = ? where last_updated < ? and date_removed is null and state = 'download' and downloaded < size", [new Date(), until])
            println "${new Date().format("dd/MM/yyyy HH:mm:ss.SSS")} cleanTokens(${timeout} minutes) = ${cleaned}"
        }
    }

    DbLogic.TokenOptions findTokenOptions(String token) {
        withSql { Sql sql ->
            GroovyRowResult row = sql.firstRow(
                    "select ur.id as id, u.credits as credits, ur.state as state, rf.local_path " +
                            "from user_request ur " +
                            "inner join resource_file rf on ur.resource_file_id = rf.id " +
                            "inner join user u on u.id = ur.user_id " +
                            "where ur.date_removed is null and ur.token = ?", [token])
            return row ? new DbLogic.TokenOptions(id:row.id, unlimited: row.credits > 0, state: row.state, path: row.local_path) : null
        }
    }

    void markToken(Long id, String state) {
        withSql { Sql sql ->
            sql.executeUpdate("update user_request set state = ?, last_updated = ? where id = ?", [state, new Date(), id])
        }
    }

    void markToken(Long written, Long id, String state) {
        withSql { Sql sql ->
            sql.executeUpdate("update user_request set downloaded = ?, state = ?, last_updated = ? where id = ?", [written, state, new Date(), id])
        }
    }

    static class TokenOptions {
        Long id
        boolean unlimited
        String state
        String path
    }
}