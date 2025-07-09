package com.toolkit.handler;

import com.toolkit.util.JwtUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class PlannerHandler {
    private final MongoClient client;

    public PlannerHandler(MongoClient client) {
        this.client = client;
    }

    // ➕ Create or Update Plan
    public void upsertPlan(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);
        JsonObject body = ctx.body().asJsonObject();
        String date = body.getString("date");

        JsonObject plan = new JsonObject()
                .put("_id", UUID.randomUUID().toString())
                .put("userId", userId)
                .put("date", date)
                .put("slots", body.getJsonObject("slots"));

        JsonObject query = new JsonObject()
                .put("userId", userId)
                .put("date", date);

        JsonObject update = new JsonObject().put("$set", plan);

        client.updateCollectionWithOptions("planner", query, update, new io.vertx.ext.mongo.UpdateOptions().setUpsert(true))
                .onSuccess(res -> ctx.response().end("Day plan saved"))
                .onFailure(err -> ctx.response().setStatusCode(500).end("Error saving day plan"));
    }

    // 📅 Get Plan for Date
    public void getPlanByDate(RoutingContext ctx) {
        String userId = getUserIdFromToken(ctx);
        String date = ctx.pathParam("date");

        client.findOne("planner", new JsonObject()
                        .put("userId", userId)
                        .put("date", date), null)
                .onSuccess(plan -> {
                    if (plan == null) {
                        ctx.response().setStatusCode(404).end("No plan found");
                    } else {
                        ctx.response().putHeader("Content-Type", "application/json")
                                .end(plan.encode());
                    }
                })
                .onFailure(err -> ctx.response().setStatusCode(500).end("Error fetching plan"));
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
