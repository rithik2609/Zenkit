package com.toolkit.model;

import java.util.Map;

public class DayPlan {
    public String id;
    public String userId;
    public String date; // "YYYY-MM-DD"
    public Map<String, String> slots; // e.g., "09:00": "Gym", "10:00": "Work"
}
