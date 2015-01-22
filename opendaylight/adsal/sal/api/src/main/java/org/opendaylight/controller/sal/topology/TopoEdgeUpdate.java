/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.topology;

import java.util.Set;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;

/**
 * The class represents an Edge, the Edge's Property Set and its UpdateType.
 * If update is on new properties added to an existing edge, appropriate type is CHANGED.
 */
@Deprecated
public class TopoEdgeUpdate {
    private Edge edge;
    private Set<Property> props;
    private UpdateType type;
    private boolean isLocal;

    /**
     * Constructor for a topology element update. A TopologyElementUpdate is an
     * object that summarize what has happened on an Edge and if the update is
     * generated locally to this controller or no
     *
     * @param e
     *            Edge being updated
     * @param p
     *            Set of Properties attached to the edge
     * @param t
     *            Type of update
     */
    public TopoEdgeUpdate(Edge e, Set<Property> p, UpdateType t) {
        edge = e;
        props = p;
        type = t;
        setLocal(true);
    }

    public Edge getEdge() {
        return edge;
    }

    public Set<Property> getProperty() {
        return props;
    }

    public UpdateType getUpdateType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((edge == null) ? 0 : edge.hashCode());
        result = (prime * result) + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "TopoEdgeUpdate [edge=" + edge + ", props=" + props + ", type=" + type + ", isLocal=" + isLocal + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TopoEdgeUpdate other = (TopoEdgeUpdate) obj;
        if (edge == null) {
            if (other.edge != null) {
                return false;
            }
        } else if (!edge.equals(other.edge)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean isLocal) {
        this.isLocal = isLocal;
    }
}
