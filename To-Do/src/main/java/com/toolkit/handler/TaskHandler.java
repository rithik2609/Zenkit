package com.toolkit.handler;

import com.toolkit.util.JwtUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class TaskHandler {
    private final MongoClient client;

    public TaskHandler(MongoClient client) {
        this.client = client;
    }

    // ✅ Create Task
    public void addTask(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);
        JsonObject body = ctx.body().asJsonObject();

        JsonObject task = new JsonObject()
                .put("_id", UUID.randomUUID().toString())
                .put("userId", userId)
                .put("title", body.getString("title"))
                .put("priority", body.getString("priority", "Medium"))
                .put("completed", false)
                .put("createdAt", System.currentTimeMillis());

        client.insert("todos", task)
                .onSuccess(res -> ctx.response().end("Task added"))
                .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to add task"));
    }

    // 📥 Get All Tasks
    public void getTasks(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);
        client.find("todos", new JsonObject().put("userId", userId))
                .onSuccess(tasks -> {
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(new io.vertx.core.json.JsonArray(tasks).encode());
                })
                .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to fetch tasks"));
    }


    // ✏️ Update Task (complete or change title/priority)
    public void updateTask(RoutingContext ctx) {
        String taskId = ctx.pathParam("id");
        JsonObject update = new JsonObject().put("$set", ctx.body().asJsonObject());

        client.updateCollection("todos", new JsonObject().put("_id", taskId), update)
                .onSuccess(res -> ctx.response().end("Task updated"))
                .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to update"));
    }

    // ❌ Delete Task
    public void deleteTask(RoutingContext ctx) {
        String taskId = ctx.pathParam("id");

        client.removeDocument("todos", new JsonObject().put("_id", taskId))
                .onSuccess(res -> ctx.response().end("Task deleted"))
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
