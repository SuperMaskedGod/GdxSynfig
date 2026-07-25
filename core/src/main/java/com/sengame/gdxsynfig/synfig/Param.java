package com.sengame.gdxsynfig.synfig;

public class Param {
    private String name;
    private ValueNode value;
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ValueNode getValue() {
        return value;
    }

    public void setValue(ValueNode value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Param{" + name + "=" + value + "}";
    }
}
