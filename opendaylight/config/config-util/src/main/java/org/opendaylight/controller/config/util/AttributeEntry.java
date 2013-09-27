/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

public class AttributeEntry {
    private final String key;
    private final String description;
    private final Object value;
    private final String type;
    private final boolean rw;

    public AttributeEntry(String key, String description, Object value,
            String type, boolean rw) {
        this.key = key;
        this.description = description;
        this.value = value;
        this.type = type;
        this.rw = rw;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public Object getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public boolean isRw() {
        return rw;
    }

}
