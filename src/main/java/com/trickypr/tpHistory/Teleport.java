package com.trickypr.tpHistory.objects;

import org.bukkit.Location;

public class Teleport {
    private String playerName;
    private long timestamp;
    private String fromWorld;
    private double fromX, fromY, fromZ;
    private String toWorld;
    private double toX, toY, toZ;

    public Teleport(String playerName, long timestamp, Location fromLoc, Location toLoc) {
        this.playerName = playerName;
        this.timestamp = timestamp;
        this.fromWorld = fromLoc.getWorld() != null ? fromLoc.getWorld().getName() : "unknown_world";
        this.fromX = fromLoc.getX();
        this.fromY = fromLoc.getY();
        this.fromZ = fromLoc.getZ();
        this.toWorld = toLoc.getWorld() != null ? toLoc.getWorld().getName() : "unknown_world";
        this.toX = toLoc.getX();
        this.toY = toLoc.getY();
        this.toZ = toLoc.getZ();
    }

    // Getters (necesarios para acceder a los datos)
    public String getPlayerName() {
        return playerName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFromWorld() {
        return fromWorld;
    }

    public double getFromX() {
        return fromX;
    }

    public double getFromY() {
        return fromY;
    }

    public double getFromZ() {
        return fromZ;
    }

    public String getToWorld() {
        return toWorld;
    }

    public double getToX() {
        return toX;
    }

    public double getToY() {
        return toY;
    }

    public double getToZ() {
        return toZ;
    }
}