package com.toolkit.handler;

import com.toolkit.util.EmailUtil;
import com.toolkit.util.JwtUtil;
import com.toolkit.util.PasswordUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class AuthHandler {
    private final MongoClient client;

    public AuthHandler(MongoClient client) {
        this.client = client;
    }

    public void register(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String name = body.getString("name");
        String email = body.getString("email");
        String password = body.getString("password");

        client.findOne("users", new JsonObject().put("email", email), null)
                .onSuccess(existing -> {
                    if (existing != null) {
                        ctx.response().setStatusCode(400).end("User already exists");
                    } else {
                        String id = UUID.randomUUID().toString();
                        String hash = PasswordUtil.hashPassword(password);
                        JsonObject user = new JsonObject()
                                .put("_id", id)
                                .put("name", name)
                                .put("email", email)
                                .put("passwordHash", hash);
                        client.insert("users", user)
                                .onSuccess(res -> ctx.response().end("User registered"))
                                .onFailure(err -> ctx.response().setStatusCode(500).end("DB Error"));
                    }
                });
    }

    public void login(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String email = body.getString("email");
        String password = body.getString("password");

        client.findOne("users", new JsonObject().put("email", email), null)
                .onSuccess(user -> {
                    if (user == null) {
                        ctx.response().setStatusCode(401).end("Invalid credentials");
                    } else {
                        String hash = user.getString("passwordHash");
                        if (PasswordUtil.verifyPassword(password, hash)) {
                            String token = JwtUtil.createToken(user.getString("_id"));
                            ctx.response().putHeader("Content-Type", "application/json")
                                    .end(new JsonObject().put("token", token).encode());
                        } else {
                            ctx.response().setStatusCode(401).end("Invalid credentials");
                        }
                    }
                });
    }
    public void resetPassword(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String token = body.getString("token");
        String newPassword = body.getString("newPassword");

        JsonObject query = new JsonObject()
                .put("resetToken", token)
                .put("resetTokenExpiry", new JsonObject().put("$gt", System.currentTimeMillis()));

        client.findOne("users", query, null)
                .onSuccess(user -> {
                    if (user == null) {
                        ctx.response().setStatusCode(400).end("Invalid or expired token");
                        return;
                    }

                    String hashed = PasswordUtil.hashPassword(newPassword);

                    JsonObject update = new JsonObject().put("$set", new JsonObject()
                                    .put("password", hashed))
                            .put("$unset", new JsonObject()
                                    .put("resetToken", "")
                                    .put("resetTokenExpiry", ""));

                    client.updateCollection("users", new JsonObject().put("_id", user.getString("_id")), update)
                            .onSuccess(res -> ctx.response().end("Password reset successful"))
                            .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to reset password"));
                })
                .onFailure(err -> ctx.response().setStatusCode(500).end("Error finding reset token"));
    }

    public void sendResetLink(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String email = body.getString("email");

        client.findOne("users", new JsonObject().put("email", email), null)
                .onSuccess(user -> {
                    if (user == null) {
                        ctx.response().setStatusCode(404).end("Email not found");
                        return;
                    }

                    String token = UUID.randomUUID().toString();
                    long expiry = System.currentTimeMillis() + (15 * 60 * 1000); // 15 mins

                    JsonObject update = new JsonObject()
                            .put("$set", new JsonObject()
                                    .put("resetToken", token)
                                    .put("resetTokenExpiry", expiry));

                    client.updateCollection("users", new JsonObject().put("email", email), update)
                            .onSuccess(res -> {
                                String resetLink = "http://localhost:8888/reset-password.html?token=" + token;
                                EmailUtil.sendEmail(email, "Reset Password",
                                        "Click the link to reset your password:\n" + resetLink);
                                ctx.response().end("Reset link sent");
                            })
                            .onFailure(err -> ctx.response().setStatusCode(500).end("Failed to send reset link"));
                })
                .onFailure(err -> ctx.response().setStatusCode(500).end("Error finding user"));
    }

}
