package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XSQLBluePrintRelation implements Serializable {
    private static final long serialVersionUID = 2L;
    private XSQLBluePrintNode parent = null;
    private String property = null;
    private Class<?> childClass = null;

    public XSQLBluePrintRelation(XSQLBluePrintNode _parent, String _property,
        Class<?> _childClass) {
        this.parent = _parent;
        this.property = _property;
        this.childClass = _childClass;
    }

    public Class<?> getNEClosestClass() {
        Class<?> p = parent.getInterface();
        return getNEClosestClass(p);
    }


    public static Class<?> getNEClosestClass(Class<?> p) {
        while (!p.getInterfaces()[0]
            .equals(Object.class/*XSQLBluePrint.STOP_INTERFACE*/)) {
            p = p.getInterfaces()[0];
        }
        return p;
    }

    public XSQLBluePrintNode getParent() {
        return parent;
    }

    public String getProperty() {
        return property;
    }

    public Class<?> getChildClass() {
        return this.childClass;
    }

    public boolean equals(Object obj) {
        XSQLBluePrintRelation other = (XSQLBluePrintRelation) obj;
        if (other.parent != null && this.parent == null) {
            return false;
        }
        if (other.parent == null && this.parent != null) {
            return false;
        }

        if (other.parent == null && this.parent == null) {
            return property.equals(other.property);
        }

        if (other.parent.toString().equals(this.parent.toString())) {
            return property.equals(other.property);
        }

        return false;
    }

    public int hashCode() {
        if (parent != null) {
            return parent.toString().hashCode() + property.hashCode();
        }
        return property.hashCode();
    }

    public String toString() {
        if (parent != null) {
            return parent.toString() + ":" + property;
        } else {
            return property;
        }
    }

    public List<?> execute(Object o) {
        List<Object> result = new LinkedList<>();
        if (o == null) {
            return null;
        }

        if (Set.class.isAssignableFrom(o.getClass())) {
            Set<?> lst = (Set<?>) o;
            for (Object oo : lst) {
                addToResult(result, execute(oo));
            }
            return result;
        } else if (List.class.isAssignableFrom(o.getClass())) {
            List<?> lst = (List<?>) o;
            for (Object oo : lst) {
                addToResult(result, execute(oo));
            }
            return result;
        } else if (Map.class.isAssignableFrom(o.getClass())) {
            Map<?, ?> map = (Map<?, ?>) o;
            for (Object oo : map.values()) {
                addToResult(result, execute(oo));
            }
            return result;
        }

        addToResult(result, XSQLCriteria.getValue(o, this.property));

        return result;
    }

    private static void addToResult(List<Object> result, Object o) {
        if (o == null) {
            return;
        }
        if (Set.class.isAssignableFrom(o.getClass())) {
            Set<?> lst = (Set<?>) o;
            for (Object oo : lst) {
                result.add(oo);
            }
        } else if (List.class.isAssignableFrom(o.getClass())) {
            List<?> lst = (List<?>) o;
            for (Object oo : lst) {
                result.add(oo);
            }
        } else if (Map.class.isAssignableFrom(o.getClass())) {
            Map<?, ?> map = (Map<?, ?>) o;
            for (Object oo : map.values()) {
                result.add(oo);
            }
        } else {
            result.add(o);
        }
    }

}

