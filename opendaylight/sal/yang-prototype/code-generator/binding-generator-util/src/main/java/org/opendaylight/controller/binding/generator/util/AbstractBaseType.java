/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util;

import org.opendaylight.controller.sal.binding.model.api.Type;

public class AbstractBaseType implements Type {

    private final String packageName;
    private final String name;

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public String getName() {

        return name;
    }

    protected AbstractBaseType(String pkName, String name) {
        this.packageName = pkName;
        this.name = name;
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Type other = (Type) obj;
        if (name == null) {
            if (other.getName() != null)
                return false;
        } else if (!name.equals(other.getPackageName()))
            return false;
        if (packageName == null) {
            if (other.getPackageName() != null)
                return false;
        } else if (!packageName.equals(other.getPackageName()))
            return false;
        return true;
    }

    @Override
    public String toString() {

        return "Type (" + packageName + "." + name + ")";
    }

}
