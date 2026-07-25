package com.sengame.gdxsynfig.synfig;

public class Keyframe {
    private String time;
    private boolean active;
    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    @Override
    public String toString() {
        return "Keyframe{time=" + time + ", active=" + active + "}";
    }
}
