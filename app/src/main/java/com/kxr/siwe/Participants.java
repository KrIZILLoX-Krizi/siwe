package com.kxr.siwe;

public class Participants {
    private String name, date, time_from, time_to;

    public Participants (String name, String date, String time_from, String time_to) {
        this.time_from = time_from;
        this.time_to = time_to;
        this.date = date;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getTime_from() {
        return time_from;
    }

    public String getTime_to() {
        return time_to;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTime_from(String time_from) {
        this.time_from = time_from;
    }

    public void setTime_to(String time_to) {
        this.time_to = time_to;
    }
}
