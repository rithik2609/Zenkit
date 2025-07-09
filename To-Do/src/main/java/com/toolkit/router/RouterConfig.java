package com.toolkit.router;

import com.toolkit.handler.*;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.core.Vertx;

public class RouterConfig {

    public static Router setup(Vertx vertx, MongoClient mongoClient) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        AuthHandler authHandler = new AuthHandler(mongoClient);
        TaskHandler taskHandler = new TaskHandler(mongoClient);
        NoteHandler noteHandler = new NoteHandler(mongoClient);
        JournalHandler journalHandler = new JournalHandler(mongoClient);
        PomodoroHandler pomodoroHandler = new PomodoroHandler(mongoClient);
        StopwatchHandler stopwatchHandler = new StopwatchHandler(mongoClient);
        PlannerHandler plannerHandler = new PlannerHandler(mongoClient);

// Daily Planner routes
        router.post("/api/planner").handler(plannerHandler::upsertPlan);
        router.get("/api/planner/:date").handler(plannerHandler::getPlanByDate);

// Stopwatch routes
        router.post("/api/stopwatch").handler(stopwatchHandler::logSession);
        router.get("/api/stopwatch").handler(stopwatchHandler::getSessions);

// Pomodoro session routes
        router.post("/api/pomodoro").handler(pomodoroHandler::logSession);
        router.get("/api/pomodoro").handler(pomodoroHandler::getSessions);


// Journal routes
        router.post("/api/journals").handler(journalHandler::addOrUpdateJournal);
        router.get("/api/journals").handler(journalHandler::getJournals);


// Notes endpoints
        router.post("/api/notes").handler(noteHandler::upsertNote);
        router.get("/api/notes").handler(noteHandler::getNotes);
        router.delete("/api/notes/:id").handler(noteHandler::deleteNote);


        // Auth routes
        router.post("/api/auth/register").handler(authHandler::register);
        router.post("/api/auth/login").handler(authHandler::login);
        router.post("/api/auth/forgot-password").handler(authHandler::sendResetLink);
        router.post("/api/auth/reset-password").handler(authHandler::resetPassword);

        // To-Do Routes (with JWT in Authorization header)
        router.post("/api/todos").handler(taskHandler::addTask);
        router.get("/api/todos").handler(taskHandler::getTasks);
        router.put("/api/todos/:id").handler(taskHandler::updateTask);
        router.delete("/api/todos/:id").handler(taskHandler::deleteTask);

        return router;
    }
}
