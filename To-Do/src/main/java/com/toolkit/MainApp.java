package com.toolkit;

import com.toolkit.config.ConfigLoader;
import com.toolkit.router.RouterConfig;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class MainApp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        JsonObject config = new JsonObject()
                .put("connection_string", ConfigLoader.get("MONGO_URI"))
                .put("db_name", ConfigLoader.get("DB_NAME"));

        MongoClient client = MongoClient.createShared(vertx, config);
        vertx.createHttpServer()
                .requestHandler(RouterConfig.setup(vertx, client))
                .listen(Integer.parseInt(ConfigLoader.get("PORT")), res -> {
                    if (res.succeeded()) {
                        System.out.println("Server started on port " + ConfigLoader.get("PORT"));
                    } else {
                        res.cause().printStackTrace();
                    }
                });
    }
}
