package com.sengame.gdxsynfig.synfig;

import java.util.ArrayList;
import java.util.List;

public class SifCanvas {
    private String version;
    private String width;
    private String height;
    private String xres;
    private String yres;
    private String viewBox;
    private String antialias;
    private String fps;
    private String beginTime;
    private String endTime;
    private String bgcolor;
    private String name;
    private final List<Meta> metas = new ArrayList<>();
    private final List<Keyframe> keyframes = new ArrayList<>();
    private final List<Layer> layers = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getXres() {
        return xres;
    }

    public void setXres(String xres) {
        this.xres = xres;
    }

    public String getYres() {
        return yres;
    }

    public void setYres(String yres) {
        this.yres = yres;
    }

    public String getViewBox() {
        return viewBox;
    }

    public void setViewBox(String viewBox) {
        this.viewBox = viewBox;
    }

    public String getAntialias() {
        return antialias;
    }

    public void setAntialias(String antialias) {
        this.antialias = antialias;
    }

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getBgcolor() {
        return bgcolor;
    }

    public void setBgcolor(String bgcolor) {
        this.bgcolor = bgcolor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Meta> getMetas() {
        return metas;
    }

    public List<Keyframe> getKeyframes() {
        return keyframes;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    @Override
    public String toString() {
        return "SifCanvas{name=" + name + ", " + width + "x" + height + ", layers=" + layers.size() + ", keyframes=" + keyframes.size() + ", metas=" + metas.size() + "}";
    }
}
