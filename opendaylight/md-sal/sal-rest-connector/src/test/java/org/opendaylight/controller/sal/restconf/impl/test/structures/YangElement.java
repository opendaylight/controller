package org.opendaylight.controller.sal.restconf.impl.test.structures;

public class YangElement {
    protected String name;

    protected YangElement(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        YangElement yangElement = (YangElement) obj;
        if (this.name == null) {
            if (yangElement.name != null) {
                return false;
            }
        } else if (!this.name.equals(yangElement.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

}
