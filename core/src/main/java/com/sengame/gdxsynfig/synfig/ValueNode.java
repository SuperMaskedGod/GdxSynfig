package com.sengame.gdxsynfig.synfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ValueNode {
    private String type;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private String textContent;
    private Double x;
    private Double y;
    private Double z;
    private final Map<String, ValueNode> namedChildren = new LinkedHashMap<>();
    private final List<Waypoint> waypoints = new ArrayList<>();
    private SifCanvas nestedCanvas;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public Map<String, ValueNode> getNamedChildren() {
        return namedChildren;
    }

    public ValueNode getNamedChild(String name) {
        return namedChildren.get(name);
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public SifCanvas getNestedCanvas() {
        return nestedCanvas;
    }

    public void setNestedCanvas(SifCanvas nestedCanvas) {
        this.nestedCanvas = nestedCanvas;
    }

    public boolean isAnimated() {
        return "animated".equals(type);
    }

    public Double getDoubleValue() {
        String v = attributes.get("value");
        if (v == null) return null;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Boolean getBooleanValue() {
        String v = attributes.get("value");
        return v == null ? null : Boolean.parseBoolean(v);
    }

    public String getStringValue() {
        String v = attributes.get("value");
        if (v != null) return v;
        if (textContent != null) return textContent;
        if (isAnimated() && !waypoints.isEmpty()) {
            Waypoint firstWp = waypoints.get(0);
            if (firstWp.getValue() != null) {
                return firstWp.getValue().getStringValue();
            }
        }
        return null;
    }

    public String getActiveLayerName() {
        return getStringValue();
    }

    public String getActiveLayerNameAt(String time) {
        if (!isAnimated()) {
            return getActiveLayerName();
        }
        for (Waypoint wp : waypoints) {
            if (time.equals(wp.getTime())) {
                if (wp.getValue() != null) {
                    return wp.getValue().getStringValue();
                }
            }
        }
        return getActiveLayerName();
    }

    public boolean isStatic() {
        return "true".equals(attributes.get("static"));
    }

    public String getTimeRaw() {
        return attributes.get("value");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type == null ? "?" : type);
        if (!attributes.isEmpty()) sb.append(attributes);
        if (x != null || y != null) sb.append("(x=").append(x).append(",y=").append(y).append(")");
        if (textContent != null) sb.append("=\"").append(textContent).append('"');
        if (isAnimated()) sb.append("[").append(waypoints.size()).append(" waypoints]");
        if (!namedChildren.isEmpty()) sb.append(namedChildren.keySet());
        if (nestedCanvas != null) sb.append("[nestedCanvas]");
        return sb.toString();
    }
}
