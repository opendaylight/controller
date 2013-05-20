/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util;

import org.opendaylight.controller.sal.binding.model.api.Type;


public final class ReferencedTypeImpl implements Type {
    
    private final String packageName;
    private final String name;
    
    public ReferencedTypeImpl(String packageName, String name) {
        super();
        this.packageName = packageName;
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.opendaylight.controller.sal.binding.model.api.Type#getPackageName()
     */
    @Override
    public String getPackageName() {
        return packageName;
    }

    /* (non-Javadoc)
     * @see org.opendaylight.controller.sal.binding.model.api.Type#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((packageName == null) ? 0 : packageName.hashCode());
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
        ReferencedTypeImpl other = (ReferencedTypeImpl) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (packageName == null) {
            if (other.packageName != null) {
                return false;
            }
        } else if (!packageName.equals(other.packageName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ReferencedTypeImpl [packageName=");
        builder.append(packageName);
        builder.append(", name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }
}
