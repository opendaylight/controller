/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

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
        if (o == null) {
            return null;
        }

        List<Object> result = new LinkedList<>();
        if (o instanceof Set) {
            for (Object oo : (Set<?>) o) {
                addToResult(result, execute(oo));
            }
        } else if (o instanceof List) {
            for (Object oo : (List<?>) o) {
                addToResult(result, execute(oo));
            }
        } else if (o instanceof Map) {
            for (Object oo : ((Map<?, ?>) o).values()) {
                addToResult(result, execute(oo));
            }
        } else {
            addToResult(result, XSQLCriteria.getValue(o, this.property));
        }
        return result;
    }

    private static void addToResult(List<Object> result, Object o) {
        if (o instanceof Set) {
            result.addAll((Set<?>)o);
        } else if (o instanceof List) {
            result.addAll((List<?>)o);
        } else if (o instanceof Map) {
            result.addAll(((Map<?, ?>)o).values());
        } else if (o != null) {
            result.add(o);
        }
    }
}

