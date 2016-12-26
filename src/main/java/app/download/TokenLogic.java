/*
* @author Alberto Vilches
* @date 24/12/2016
*/
package app.download;

import redis.clients.jedis.Jedis;

public class TokenLogic {
    public Jedis jedis;

    Result checkToken(String token, String ip) {
        String r = jedis.get("token#"+token);
        if (r == "null" || r == null || r == "denied") {
            return Result.denied;
//        } else if (r == "freeSlow") {
//            return Result.verySlow;
        }
        return Result.fast;
    }

    enum Result {
        denied   (true,  null),
        verySlow (false, CPSPauser.createInKBps(100)),
        slow     (false, CPSPauser.createInKBps(400)),
        fast     (false, CPSPauser.createInKBps(800)),
        ultraFast(false, null);

        boolean forbidden = false;
        CPSPauser cpsPauser;

        Result(boolean forbidden, CPSPauser cpsPauser) {
            this.forbidden = forbidden;
            this.cpsPauser = cpsPauser;
        }
    }
}
