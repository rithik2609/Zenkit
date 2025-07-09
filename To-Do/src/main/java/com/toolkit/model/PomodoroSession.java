package com.toolkit.model;

public class PomodoroSession {
    public String id;
    public String userId;
    public String type; // "focus" or "break"
    public long startTime;
    public int duration; // in minutes
}
