package app

import groovy.sql.GroovyRowResult
import groovy.sql.Sql

import javax.sql.DataSource
import java.sql.Timestamp


class DbLogic {
    DataSource ds

    DbLogic(DataSource ds) {
        this.ds = ds
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

    int cleanTokens(int timeout) {
        long start = System.currentTimeMillis()
        int cleaned = 0
        withSql { Sql sql ->
            Date until
            use(groovy.time.TimeCategory) {
                until = new Date() - timeout.minutes
            }
            sql.eachRow("select id, downloaded, date_download_start, user_resource_id from file_download where state = 'download' and date_downloading is not null and date_downloading < ?", [until]) { row ->
                cleaned ++
                abort(row.downloaded as Long, row.date_download_start as Timestamp, new Timestamp(start), row.id as Long, row.user_resource_id as Long, true)
            }
            println "${new Date().format("dd/MM/yyyy HH:mm:ss.SSS")} cleanTokens(${timeout} minutes) = ${cleaned}. Time: ${System.currentTimeMillis()-start} millis"
        }
        return cleaned
    }

    DbLogic.TokenOptions findTokenOptions(String token) {
        withSql { Sql sql ->
            GroovyRowResult row = sql.firstRow(
                    "select fd.id, fd.user_id, fd.user_resource_id, u.slots, u.credits, fd.state, rf.local_path, rf.filename, rp.level as rLevel, u.level as uLevel " +
                            "from file_download fd " +
                            "inner join resource_pack rp on fd.resource_id = rp.id " +
                            "inner join resource_file rf on fd.resource_file_id = rf.id " +
                            "inner join user u on u.id = fd.user_id " +
                            "where fd.token = ?", [token])
            if (!row) return null

            DbLogic.TokenOptions options = new DbLogic.TokenOptions(id: row.id, level: computeLevel(row.uLevel, row.rLevel), slots: row.slots, userResourceId: row.user_resource_id, userId: row.user_id, state: row.state, file: new File(row.local_path), filename: row.filename)

            if (options.slots != null) {
                GroovyRowResult queryDownloadsCount = sql.firstRow(
                        "select count(id) as c from file_download where user_id = ? and state = 'download'", [options.userId])
                options.currentDownloads = queryDownloadsCount.c
            }
            return options
        }
    }

    int computeLevel(int uLevel, Integer rLevel) {
        if (rLevel == null) return uLevel
        return Math.max(uLevel, rLevel)
    }

    int start(Long id, Timestamp now, long size) {
        withSql { Sql sql ->
            sql.executeUpdate("update file_download fd inner join user_resource ur on (ur.id = fd.user_resource_id) set fd.state = 'download', ur.state = 'download', fd.downloaded = 0, fd.size = ?, fd.last_updated = ?, ur.last_updated = ?, fd.date_download_start = ?, fd.date_downloading = ? where fd.id = ?", [size, now, now, now, now, id])
        }
    }

    int downloading(Long written, Timestamp now, Long id) {
        withSql { Sql sql ->
            sql.executeUpdate("update file_download set downloaded = ?, last_updated = ?, date_downloading = ? where id = ? and state = 'download'", [written, now, now, id])
        }
    }

    int abort(Long written, Timestamp dateStart, Timestamp now, Long id, Long userResourceId, boolean hang) {
        withSql { Sql sql ->
            if (sql.executeUpdate("update file_download fd inner join user_resource ur on (ur.id = fd.user_resource_id)  set fd.state = 'unlocked', fd.last_updated = ?, fd.aborts = fd.aborts + 1, ur.aborts = ur.aborts + 1 where fd.id = ?", [now, id]) == 0) {
                return 0
            }
            if (sql.executeUpdate("insert into file_download_session set date_start = ?, date_end = ?, file_download_id = ?, downloaded = ?, state = ?", [dateStart, now, id, written, hang?"ha":"ko"]) == 0) {
                return 1
            }
            return 2 + updateUserResourceState(sql, userResourceId)
        }
    }

    int finish(Long id, Long userResourceId, Timestamp dateStart, Timestamp now, long total) {
        withSql { Sql sql ->
            if (sql.executeUpdate("update file_download fd inner join user_resource ur on (ur.id = fd.user_resource_id) set fd.token = ?, fd.state = 'finished', fd.downloaded = ?, fd.size = ?, fd.date_download_start = ?, fd.last_updated = ?, fd.date_downloaded = ?, fd.oks = fd.oks + 1, ur.oks = ur.oks + 1 where fd.id = ?", [null, total, total, dateStart, now, now, id]) == 0) {
                return 0
            }
            if (sql.executeUpdate("insert into file_download_session set date_start = ?, date_end = ?, file_download_id = ?, downloaded = ?, state = ?", [dateStart, now, id, total, 'ok']) == 0) {
                return 1
            }
            return 2 + updateUserResourceState(sql, userResourceId)
        }
    }

    private int updateUserResourceState(Sql sql, long userResourceId) {
        int pending = sql.firstRow("select count(id) count from file_download fd where fd.user_resource_id = ? and fd.state <> 'finished'", [userResourceId]).count
        if (pending == 0) {
            // No hay ninguno pendiente = estan todos finished = marcamos el user_resource como finished
            return sql.executeUpdate("update user_resource ur set ur.state = 'finished', ur.finished = ? where id = ?", [new Date(), userResourceId])
        }
        // Hay pendientes (alguno unlocked, alguno download)
        int downs = sql.firstRow("select count(id) count from file_download fd where fd.user_resource_id = ? and fd.state = 'download'", [userResourceId]).count
        if (downs == 0) {
            // No hay ninguno bajandose, todos son pendientes, marcamos la descarga como pendiente (probablmente se hayan cancelado todas las descargas)
            return sql.executeUpdate("update user_resource ur set ur.state = 'unlocked', ur.finished = ? where id = ?", [new Date(), userResourceId])
        }
        return 0
    }

    static class TokenOptions {
        Long id
        Long userResourceId
        Long userId
        Long slots
        String state
        File file
        String filename
        int level = 0
        int cps = 0
        String cpsMsg = 0
        int currentDownloads = 0

        String getFileSizeString() {
            StringTools.humanReadableString(file.length())
        }

        String getETA() {
            if (cps < 0) {
                return "-"
            }

            int s = file.length() / cps;
            if (s > 3600) {
                return String.format("%dh %02dm %02ds", (int)(s / 3600), (int)((s % 3600) / 60), (int)(s % 60));
            } else {
                return String.format("%dm %ds", (int)((s % 3600) / 60), (int)(s % 60));
            }

        }

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