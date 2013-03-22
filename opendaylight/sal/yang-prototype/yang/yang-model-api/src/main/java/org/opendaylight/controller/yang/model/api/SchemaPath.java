/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;

public class SchemaPath {

    final List<QName> path;
    final boolean absolute;

    public SchemaPath(final List<QName> path, boolean absolute) {
        this.path = Collections.unmodifiableList(new ArrayList<QName>(path));
        this.absolute = absolute;
    }

    public List<QName> getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (absolute ? 1231 : 1237);
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
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
        SchemaPath other = (SchemaPath) obj;
        if (absolute != other.absolute) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SchemaPath [path=");
        builder.append(path);
        builder.append(", absolute=");
        builder.append(absolute);
        builder.append("]");
        return builder.toString();
    }
}
