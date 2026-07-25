package com.sengame.gdxsynfig.synfig;

import java.util.ArrayList;
import java.util.List;
public class Layer {
    private String type;
    private boolean active;
    private boolean excludeFromRendering;
    private String version;
    private String desc;
    private final List<Param> params = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isExcludeFromRendering() {
        return excludeFromRendering;
    }

    public void setExcludeFromRendering(boolean excludeFromRendering) {
        this.excludeFromRendering = excludeFromRendering;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<Param> getParams() {
        return params;
    }

    public Param getParam(String name) {
        for (Param p : params) {
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

    public SifCanvas getChildCanvas() {
        Param p = getParam("canvas");
        if (p == null || p.getValue() == null) return null;
        return p.getValue().getNestedCanvas();
    }

    @Override
    public String toString() {
        return "Layer{type=" + type + ", desc=" + desc + ", params=" + params.size() + "}";
    }
}
