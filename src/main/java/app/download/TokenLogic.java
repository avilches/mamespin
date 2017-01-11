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

    public void markToken(Long id, String state) {
        dbLogic.markToken(id, state);
    }

    public void markToken(Long written, Long id, String state) {
        dbLogic.markToken(written, id, state);
    }
}
