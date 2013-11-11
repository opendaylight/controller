package org.opendaylight.controller.sal.restconf.impl.test.structures;

public class Lf extends YangElement {
    private String value;

    public Lf(String name, String value) {
        super(name);
        this.value = value;
    }

    public Lf(String value) {
        super("");
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        Lf lf = (Lf) obj;
        if (!this.value.equals(lf.value)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + value;
    }

}
