package com.toolkit.handler;

import com.toolkit.util.JwtUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class NoteHandler {
    private final MongoClient client;

    public NoteHandler(MongoClient client) {
        this.client = client;
    }

    // ➕ Add or Update Note
    public void upsertNote(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);
        JsonObject body = ctx.body().asJsonObject();

        String noteId = body.getString("id", UUID.randomUUID().toString());
        JsonObject note = new JsonObject()
                .put("_id", noteId)
                .put("userId", userId)
                .put("content", body.getString("content"))
                .put("updatedAt", System.currentTimeMillis());

        JsonObject query = new JsonObject().put("_id", noteId);
        JsonObject update = new JsonObject().put("$set", note);

        client.updateCollectionWithOptions("notes", query, update, new io.vertx.ext.mongo.UpdateOptions().setUpsert(true))
                .onSuccess(res -> ctx.response().end("Note saved"))
                .onFailure(err -> {
                    err.printStackTrace(); // 👈 Add this to see real cause
                    ctx.response().setStatusCode(500).end("Error saving note");
                });
    }

    // 📥 Get All Notes
    public void getNotes(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);

        client.find("notes", new JsonObject().put("userId", userId))
                .onSuccess(notes -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonArray(notes).encode());
                })
                .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to fetch notes"));
    }

    // ❌ Delete Note
    public void deleteNote(RoutingContext ctx) {
        String noteId = ctx.pathParam("id");

        client.removeDocument("notes", new JsonObject().put("_id", noteId))
                .onSuccess(res -> ctx.response().end("Note deleted"))
                .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to delete"));
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
