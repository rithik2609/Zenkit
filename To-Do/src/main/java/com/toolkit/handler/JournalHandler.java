package com.toolkit.handler;

import com.toolkit.util.JwtUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class JournalHandler {
    private final MongoClient client;

    public JournalHandler(MongoClient client) {
        this.client = client;
    }

    // ➕ Add or Update Journal
    public void addOrUpdateJournal(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);
        JsonObject body = ctx.body().asJsonObject();

        String date = body.getString("date");
        JsonObject entry = new JsonObject()
                .put("_id", UUID.randomUUID().toString())
                .put("userId", userId)
                .put("date", date)
                .put("content", body.getString("content"))
                .put("mood", body.getString("mood", "😊"));

        JsonObject query = new JsonObject()
                .put("userId", userId)
                .put("date", date);

        JsonObject update = new JsonObject().put("$set", entry);

        client.updateCollectionWithOptions("journals", query, update, new io.vertx.ext.mongo.UpdateOptions().setUpsert(true))
                .onSuccess(res -> ctx.response().end("Journal entry saved"))
                .onFailure(err -> ctx.response().setStatusCode(500).end("Error saving journal"));
    }

    // 📥 Get All Journals
    public void getJournals(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);

        client.find("journals", new JsonObject().put("userId", userId))
                .onSuccess(entries -> ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonArray(entries).encode()))
                .onFailure(err -> ctx.response().setStatusCode(500).end("Error fetching journals"));
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
