package com.sengame.gdxsynfig.synfig;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

import java.io.IOException;
import java.io.InputStream;

public final class SifParser {
    private SifParser() {
    }

    public static SifCanvas parse(FileHandle file) {
        try (InputStream in = file.read()) {
            return parse(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SifCanvas parse(InputStream in) {
        try {
            InputStream xmlIn = GzipSupport.unwrap(in);
            Element root = new XmlReader().parse(xmlIn);
            return parseCanvas(root);
        } catch (IOException | SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    private static SifCanvas parseCanvas(Element canvasEl) {
        SifCanvas canvas = new SifCanvas();
        canvas.setVersion(attr(canvasEl, "version"));
        canvas.setWidth(attr(canvasEl, "width"));
        canvas.setHeight(attr(canvasEl, "height"));
        canvas.setXres(attr(canvasEl, "xres"));
        canvas.setYres(attr(canvasEl, "yres"));
        canvas.setViewBox(attr(canvasEl, "view-box"));
        canvas.setAntialias(attr(canvasEl, "antialias"));
        canvas.setFps(attr(canvasEl, "fps"));
        canvas.setBeginTime(attr(canvasEl, "begin-time"));
        canvas.setEndTime(attr(canvasEl, "end-time"));
        canvas.setBgcolor(attr(canvasEl, "bgcolor"));

        for (int i = 0; i < canvasEl.getChildCount(); i++) {
            Element child = canvasEl.getChild(i);
            switch (child.getName()) {
                case "n":
                case "name":
                    canvas.setName(text(child));
                    break;
                case "meta": {
                    Meta meta = new Meta();
                    meta.setName(attr(child, "name"));
                    meta.setContent(attr(child, "content"));
                    canvas.getMetas().add(meta);
                    break;
                }
                case "keyframe": {
                    Keyframe kf = new Keyframe();
                    kf.setTime(attr(child, "time"));
                    kf.setActive(child.getBooleanAttribute("active", false));
                    canvas.getKeyframes().add(kf);
                    break;
                }
                case "layer":
                    canvas.getLayers().add(parseLayer(child));
                    break;
                default:
                    break;
            }
        }
        return canvas;
    }

    private static Layer parseLayer(Element layerEl) {
        Layer layer = new Layer();
        layer.setType(attr(layerEl, "type"));
        layer.setActive(layerEl.getBooleanAttribute("active", false));
        layer.setExcludeFromRendering(layerEl.getBooleanAttribute("exclude_from_rendering", false));
        layer.setVersion(attr(layerEl, "version"));
        layer.setDesc(attr(layerEl, "desc"));

        for (int i = 0; i < layerEl.getChildCount(); i++) {
            Element child = layerEl.getChild(i);
            if (!"param".equals(child.getName())) continue;

            Param param = new Param();
            param.setName(attr(child, "name"));
            Element valueEl = child.getChildCount() > 0 ? child.getChild(0) : null;
            if (valueEl != null) {
                param.setValue(parseValueNode(valueEl));
            }
            layer.getParams().add(param);
        }
        return layer;
    }

    private static ValueNode parseValueNode(Element el) {
        ValueNode node = new ValueNode();
        String tag = el.getName();
        node.setType(tag);

        ObjectMap<String, String> attrs = el.getAttributes();
        if (attrs != null) {
            for (ObjectMap.Entry<String, String> e : attrs.entries()) {
                node.getAttributes().put(e.key, e.value);
            }
        }

        switch (tag) {
            case "vector": {
                node.setX(childDouble(el, "x"));
                node.setY(childDouble(el, "y"));
                node.setZ(childDouble(el, "z"));
                break;
            }
            case "color": {
                node.setR(childDouble(el, "r"));
                node.setG(childDouble(el, "g"));
                node.setB(childDouble(el, "b"));
                node.setA(childDouble(el, "a"));
                break;
            }
            case "composite": {
                for (int i = 0; i < el.getChildCount(); i++) {
                    Element wrapper = el.getChild(i);
                    Element inner = wrapper.getChildCount() > 0 ? wrapper.getChild(0) : null;
                    if (inner != null) {
                        node.getNamedChildren().put(wrapper.getName(), parseValueNode(inner));
                    }
                }
                break;
            }
            case "animated": {
                for (int i = 0; i < el.getChildCount(); i++) {
                    Element wp = el.getChild(i);
                    if (!"waypoint".equals(wp.getName())) continue;

                    Waypoint waypoint = new Waypoint();
                    waypoint.setTime(attr(wp, "time"));
                    waypoint.setBefore(attr(wp, "before"));
                    waypoint.setAfter(attr(wp, "after"));

                    String tension = attr(wp, "tension");
                    if (tension != null) waypoint.setTension(Float.parseFloat(tension));

                    String continuity = attr(wp, "continuity");
                    if (continuity != null) waypoint.setContinuity(Float.parseFloat(continuity));

                    String bias = attr(wp, "bias");
                    if (bias != null) waypoint.setBias(Float.parseFloat(bias));

                    Element inner = wp.getChildCount() > 0 ? wp.getChild(0) : null;
                    if (inner != null) {
                        waypoint.setValue(parseValueNode(inner));
                    }
                    node.getWaypoints().add(waypoint);
                }
                break;
            }
            case "canvas": {
                node.setNestedCanvas(parseCanvas(el));
                break;
            }
            case "string": {
                node.setTextContent(text(el));
                break;
            }
            default:
                break;
        }
        return node;
    }

    private static String attr(Element el, String name) {
        return el.getAttribute(name, null);
    }

    private static String text(Element el) {
        String t = el.getText();
        return t == null ? null : t.trim();
    }

    private static Double childDouble(Element parent, String tag) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            Element child = parent.getChild(i);
            if (!tag.equals(child.getName())) continue;

            String t = text(child);
            if (t != null && !t.isEmpty()) {
                try {
                    return Double.parseDouble(t);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
