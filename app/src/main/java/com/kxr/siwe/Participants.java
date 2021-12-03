package com.kxr.siwe;

public class Participants {
    private String myKey;
    private String name;
    private String email;
    private String date;
    private String time_from;
    private String time_to;

    public Participants (String key, String name, String email, String date, String time_from, String time_to) {
        this.time_from = time_from;
        this.time_to = time_to;
        this.date = date;
        this.name = name;
        this.myKey = key;
        this.email = email;
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

    public String getMyKey() {
        return myKey;
    }

    public String getEmail() { return email; }

    public void setName(String name) { this.name = name; }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTime_from(String time_from) {
        this.time_from = time_from;
    }

    public void setTime_to(String time_to) {
        this.time_to = time_to;
    }

    public void setMyKey(String key) { this.myKey = key; }

    public void setEmail(String email) { this.email = email; }
}
