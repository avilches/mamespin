/*
* @author Alberto Vilches
* @date 24/12/2016
*/
package app.download;

import app.DbLogic;
import redis.clients.jedis.Jedis;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Date;

public class TokenLogic {
    public Jedis jedis;
    public DataSource ds;
    public DbLogic dbLogic;

    DbLogic.TokenOptions checkToken(String token, String ip) {
        DbLogic.TokenOptions tokenOptions = dbLogic.findTokenOptions(token);
        return tokenOptions;
    }

    public boolean start(Long id, Date date, long size) {
        return dbLogic.start(id, new Timestamp(date.getTime()), size) == 2;
    }

    public boolean downloading(long written, Date date, Long id) {
        return dbLogic.downloading(written, new Timestamp(date.getTime()), id) == 1;
    }

    public void abort(long written, Date startDate, Date endDate, Long id, long userResourceId) {
        dbLogic.abort(written, new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime()), id, userResourceId, false);
    }

    public void finish(long id, long userResourceId, Date startDate, Date endDate, long total) {
        dbLogic.finish(id, userResourceId, new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime()), total);
    }
}
