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
        long start = System.currentTimeMillis()
        withSql { Sql sql ->
            Date until
            use(groovy.time.TimeCategory) {
                until = new Date() - timeout.minutes
            }
            int cleaned = 0
            String query = "select id, downloaded from user_request where state = 'download' and (downloaded < size or downloaded is null) and (date_downloading < ? or last_updated < ?)"
            def params = [until, until]
            sql.eachRow(query, params) { row ->
                cleaned += abort(row.downloaded as long, row.id as long, true)
            }
            println "${new Date().format("dd/MM/yyyy HH:mm:ss.SSS")} cleanTokens(${timeout} minutes) = ${cleaned}. Time: ${System.currentTimeMillis()-start} millis"
        }
    }

    DbLogic.TokenOptions findTokenOptions(String token) {
        withSql { Sql sql ->
            GroovyRowResult row = sql.firstRow(
                    "select ur.id as id, ur.user_id as user_id, u.credits as credits, ur.state as state, rf.local_path " +
                            "from user_request ur " +
                            "inner join resource_file rf on ur.resource_file_id = rf.id " +
                            "inner join user u on u.id = ur.user_id " +
                            "where ur.token = ?", [token])
            if (!row) return null
            boolean unlimited = row.credits > 0
            def options = new DbLogic.TokenOptions(id: row.id, userId: row.user_id, unlimited: unlimited, state: row.state, path: row.local_path)

            if (!unlimited) {
                GroovyRowResult queryDownloadsCount = sql.firstRow(
                        "select count(id) as c from user_request where user_id = ? and state = 'download'", [options.userId])
                options.currentDownloads = queryDownloadsCount.c
            }
            return options
        }
    }

    int start(Long id, long size) {
        withSql { Sql sql ->
            Date now = new Date()
            sql.executeUpdate("update user_request set state = 'download', downloaded = 0, size = ?, last_updated = ?, date_download_start = ? where id = ?", [size, now, now, id])
        }
    }

    int downloading(Long written, Long id) {
        withSql { Sql sql ->
            Date now = new Date()
            sql.executeUpdate("update user_request set downloaded = ?, last_updated = ?, date_downloading = ? where id = ? and state = 'download'", [written, now, now, id])
        }
    }

    int abort(Long written, Long id, boolean hang) {
        withSql { Sql sql ->
            Date now = new Date()
            if (sql.executeUpdate("update user_request set state = 'unlocked', last_updated = ?, aborts = aborts + 1 where id = ?", [now, id]) == 1) {
                return sql.executeUpdate("insert into user_request_abort set date_created = ?, user_request_id = ?, downloaded = ?, hang = ?", [now, id, written, hang])
            }
            return 0
        }
    }

    int finish(Long id, long total) {
        withSql { Sql sql ->
            Date now = new Date()
            sql.executeUpdate("update user_request set state = 'finished', downloaded = ?, last_updated = ?, date_downloaded = ? where id = ?", [total, now, now, id])
        }
    }

    static class TokenOptions {
        Long id
        Long userId
        boolean unlimited
        String state
        String path
        int currentDownloads = 0
    }
}