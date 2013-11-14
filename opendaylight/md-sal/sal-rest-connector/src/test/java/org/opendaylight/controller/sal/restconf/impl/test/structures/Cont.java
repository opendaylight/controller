package org.opendaylight.controller.sal.restconf.impl.test.structures;

public class Cont extends LstItem {
    String name = null;

    public Cont(String name) {
        super();
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
        if (!super.equals(obj)) {
            return false;
        }
        Cont cont = (Cont) obj;
        if (this.name == null) {
            if (cont.name != null) {
                return false;
            }
        } else if (!this.name.equals(cont.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

}
