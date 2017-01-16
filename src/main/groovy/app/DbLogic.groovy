package app

import groovy.sql.GroovyRowResult
import groovy.sql.Sql

import javax.sql.DataSource
import java.sql.Timestamp


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
        Date start = new Date()
        withSql { Sql sql ->
            Date until
            use(groovy.time.TimeCategory) {
                until = new Date() - timeout.minutes
            }
            int cleaned = 0
            sql.eachRow("select id, downloaded, date_download_start from file_download where state = 'download' and date_downloading < ?", [until]) { row ->
                cleaned += abort(row.downloaded as Long, row.date_download_start as Timestamp, start as Timestamp, row.id as Long, true)
            }
            println "${new Date().format("dd/MM/yyyy HH:mm:ss.SSS")} cleanTokens(${timeout} minutes) = ${cleaned}. Time: ${System.currentTimeMillis()-start.time} millis"
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

    int start(Long id, Timestamp now, long size) {
        withSql { Sql sql ->
            sql.executeUpdate("update file_download fd inner join user_resource ur on (ur.id = fd.user_resource_id) set fd.state = 'download', ur.state = 'download', fd.downloaded = 0, fd.size = ?, fd.last_updated = ?, ur.last_updated = ?, date_download_start = ? where fd.id = ?", [size, now, now, now, id])
        }
    }

    int downloading(Long written, Timestamp now, Long id) {
        withSql { Sql sql ->
            sql.executeUpdate("update file_download set downloaded = ?, last_updated = ?, date_downloading = ? where id = ? and state = 'download'", [written, now, now, id])
        }
    }

    int abort(Long written, Timestamp dateStart, Timestamp now, Long id, boolean hang) {
        withSql { Sql sql ->
            if (sql.executeUpdate("update file_download set state = 'unlocked', last_updated = ?, aborts = aborts + 1 where id = ?", [now, id]) == 1) {
                return 1 + sql.executeUpdate("insert into file_download_session set date_start = ?, date_end = ?, file_download_id = ?, downloaded = ?, state = ?", [dateStart, now, id, written, hang?"ha":"ko"])
            }
            return 0
        }
    }

    int finish(Long id, Timestamp dateStart, Timestamp now, long total) {
        withSql { Sql sql ->
            if (sql.executeUpdate("update file_download set state = 'finished', downloaded = ?, last_updated = ?, date_downloaded = ?, oks = oks + 1 where id = ?", [total, now, now, id]) == 1) {
                return 1 + sql.executeUpdate("insert into file_download_session set date_start = ?, date_end = ?, file_download_id = ?, downloaded = ?, state = ?", [dateStart, now, id, total, 'ok'])
            }
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