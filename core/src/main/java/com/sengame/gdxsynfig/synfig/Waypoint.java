package com.sengame.gdxsynfig.synfig;

public class Waypoint {
    private String time;
    private String before;
    private String after;
    private ValueNode value;
    private float tension;
    private float continuity;
    private float bias;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public ValueNode getValue() {
        return value;
    }

    public void setValue(ValueNode value) {
        this.value = value;
    }

    public float getTension() {
        return tension;
    }

    public void setTension(float tension) {
        this.tension = tension;
    }

    public float getContinuity() {
        return continuity;
    }

    public void setContinuity(float continuity) {
        this.continuity = continuity;
    }

    public float getBias() {
        return bias;
    }

    public void setBias(float bias) {
        this.bias = bias;
    }

    @Override
    public String toString() {
        return "Waypoint{time=" + time + ", before=" + before + ", after=" + after + ", value=" + value + "}";
    }
}
