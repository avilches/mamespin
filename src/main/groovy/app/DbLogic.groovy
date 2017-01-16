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
            String query = "select id, downloaded from file_download where state = 'download' and (date_downloading < ? or last_updated < ?)"
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
                    "select fd.id, fd.user_id, fd.user_resource_id, u.slots, u.credits, fd.state, rf.local_path " +
                            "from file_download fd " +
                            "inner join resource_file rf on fd.resource_file_id = rf.id " +
                            "inner join user u on u.id = fd.user_id " +
                            "where fd.token = ?", [token])
            if (!row) return null
            DbLogic.TokenOptions options = new DbLogic.TokenOptions(id: row.id, slots: row.slots, userResourceId: row.user_resource_id, userId: row.user_id, state: row.state, path: row.local_path)

            if (options.slots != null) {
                GroovyRowResult queryDownloadsCount = sql.firstRow(
                        "select count(id) as c from file_download where user_id = ? and state = 'download'", [options.userId])
                options.currentDownloads = queryDownloadsCount.c
            }
            return options
        }
    }

    int start(TokenOptions tokenOptions, long size) {
        withSql { Sql sql ->
            Date now = new Date()
            sql.executeUpdate("update file_download fd inner join user_resource ur on (ur.id = fd.user_resource_id) set fd.state = 'download', ur.d.state = 'download', fd.downloaded = 0, fd.size = ?, fd.last_updated = ?, ur.last_updated = ?, date_download_start = ? where id = ?", [size, now, now, now, tokenOptions.id])
        }
    }

    int downloading(Long written, Long id) {
        withSql { Sql sql ->
            Date now = new Date()
            sql.executeUpdate("update file_download set downloaded = ?, last_updated = ?, date_downloading = ? where id = ? and state = 'download'", [written, now, now, id])
        }
    }

    int abort(Long written, Long id, boolean hang) {
        withSql { Sql sql ->
            Date now = new Date()
            if (sql.executeUpdate("update file_download set state = 'unlocked', last_updated = ?, aborts = aborts + 1 where id = ?", [now, id]) == 1) {
                return sql.executeUpdate("insert into file_download_abort set date_created = ?, file_download_id = ?, downloaded = ?, hang = ?", [now, id, written, hang])
            }
            return 0
        }
    }

    int finish(Long id, long total) {
        withSql { Sql sql ->
            Date now = new Date()
            sql.executeUpdate("update file_download set state = 'finished', downloaded = ?, last_updated = ?, date_downloaded = ? where id = ?", [total, now, now, id])
        }
    }

    static class TokenOptions {
        Long id
        Long userResourceId
        Long userId
        Long slots
        String state
        String path
        int currentDownloads = 0

        boolean isSlotOverflow() {
            slots != null && currentDownloads >= slots
        }

        boolean isDownloading() {
            state == "download"
        }

        boolean isFinished() {
            state == "finished"
        }
    }
}