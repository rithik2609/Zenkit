package com.toolkit.handler;

import com.toolkit.util.JwtUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class PomodoroHandler {
    private final MongoClient client;

    public PomodoroHandler(MongoClient client) {
        this.client = client;
    }

    // ➕ Log Pomodoro Session
    public void logSession(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);
        JsonObject body = ctx.body().asJsonObject();

        JsonObject session = new JsonObject()
                .put("_id", UUID.randomUUID().toString())
                .put("userId", userId)
                .put("type", body.getString("type", "focus")) // "focus" or "break"
                .put("startTime", body.getLong("startTime", System.currentTimeMillis()))
                .put("duration", body.getInteger("duration", 25)); // minutes

        client.insert("pomodoros", session)
                .onSuccess(res -> ctx.response().end("Pomodoro session logged"))
                .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to log session"));
    }

    // 📥 Get All Pomodoro Sessions
    public void getSessions(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);

        client.find("pomodoros", new JsonObject().put("userId", userId))
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
