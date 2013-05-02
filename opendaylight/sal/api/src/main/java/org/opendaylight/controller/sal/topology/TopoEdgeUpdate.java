/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.topology;

import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;

/**
 * The class represents an Edge, the Edge's Property Set and its UpdateType.
 */

public class TopoEdgeUpdate {
    private Edge edge;
    private Set<Property> props;
    private UpdateType type;

    public TopoEdgeUpdate(Edge e, Set<Property> p, UpdateType t) {
        edge = e;
        props = p;
        type = t;
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
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return "TopoEdgeUpdate[" + ReflectionToStringBuilder.toString(this)
                + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
