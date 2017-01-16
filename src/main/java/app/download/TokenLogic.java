/*
* @author Alberto Vilches
* @date 24/12/2016
*/
package app.download;

import app.DbLogic;
import redis.clients.jedis.Jedis;

import javax.sql.DataSource;

public class TokenLogic {
    public Jedis jedis;
    public DataSource ds;
    public DbLogic dbLogic;

    DbLogic.TokenOptions checkToken(String token, String ip) {
        DbLogic.TokenOptions tokenOptions = dbLogic.findTokenOptions(token);
        return tokenOptions;
    }

    public void start(DbLogic.TokenOptions opts, long size) {
        dbLogic.start(opts, size);
    }

    public boolean downloading(long written, Long id) {
        return dbLogic.downloading(written, id) == 1;
    }

    public void abort(long written, Long id) {
        dbLogic.abort(written, id, false);
    }

    public void finish(long id, long total) {
        dbLogic.finish(id, total);
    }
}
