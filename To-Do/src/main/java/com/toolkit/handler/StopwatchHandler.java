package com.toolkit.handler;

import com.toolkit.util.JwtUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class StopwatchHandler {
    private final MongoClient client;

    public StopwatchHandler(MongoClient client) {
        this.client = client;
    }

    // ➕ Log Stopwatch Session
    public void logSession(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);
        JsonObject body = ctx.body().asJsonObject();

        long start = body.getLong("startTime");
        long end = body.getLong("endTime");
        long duration = end - start;

        JsonObject session = new JsonObject()
                .put("_id", UUID.randomUUID().toString())
                .put("userId", userId)
                .put("startTime", start)
                .put("endTime", end)
                .put("duration", duration)
                .put("label", body.getString("label", ""));

        client.insert("stopwatch", session)
                .onSuccess(res -> ctx.response().end("Stopwatch session logged"))
                .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to log stopwatch session"));
    }

    // 📥 Get All Sessions
    public void getSessions(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);

        client.find("stopwatch", new JsonObject().put("userId", userId))
                .onSuccess(list -> ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonArray(list).encode()))
                .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to fetch sessions"));
    }

    private String getUserIdFromToken(RoutingContext ctx) {
        String auth = ctx.request().getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            ctx.response().setStatusCode(401).end("Unauthorized");
            return null;
        }
        return JwtUtil.getUserId(auth.substring(7));
    }
}
