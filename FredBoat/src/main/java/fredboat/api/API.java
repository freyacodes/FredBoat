/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.api;

import fredboat.Config;
import fredboat.FredBoat;
import fredboat.audio.player.PlayerRegistry;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.util.ArrayList;
import java.util.List;

public class API {

    private static final Logger log = LoggerFactory.getLogger(API.class);

    private static final int PORT = 1356;

    private API() {}

    public static void start() {
        if(!Config.CONFIG.isRestServerEnabled()) {
            log.warn("Rest server is not enabled. Skipping Spark ignition!");
            return;
        }

        log.info("Igniting Spark API on port: " + PORT);

        Spark.port(PORT);

        Spark.before((request, response) -> {
            log.info(request.requestMethod() + " " + request.pathInfo());
            response.header("Access-Control-Allow-Origin", "*");
            response.type("application/json");
        });

        Spark.get("/stats", (req, res) -> {
            res.type("application/json");

            JSONObject root = new JSONObject();
            JSONArray a = new JSONArray();

            //make a copy to avoid concurrent modification errors
            List<FredBoat> shards = new ArrayList<>(FredBoat.getShards());
            for (FredBoat fb : shards) {
                JSONObject fbStats = new JSONObject();
                fbStats.put("id", fb.getShardInfo().getShardId())
                        .put("guilds", fb.getGuildCount())
                        .put("users", fb.getUserCount())
                        .put("status", fb.getJda().getStatus());

                a.put(fbStats);
            }

            JSONObject g = new JSONObject();
            g.put("playingPlayers", PlayerRegistry.getPlayingPlayers().size())
                    .put("totalPlayers", PlayerRegistry.getRegistry().size())
                    .put("distribution", Config.CONFIG.getDistribution())
                    .put("guilds", FredBoat.countAllGuilds())
                    .put("users", FredBoat.countAllUniqueUsers());

            root.put("shards", a);
            root.put("global", g);

            return root;
        });

        /* Exception handling */
        Spark.exception(Exception.class, (e, request, response) -> {
            log.error(request.requestMethod() + " " + request.pathInfo(), e);

            response.body(ExceptionUtils.getStackTrace(e));
            response.type("text/plain");
            response.status(500);
        });
    }

}
