
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * @file   UpdateType.java
 *
 * @brief  Describes update types
 *
 */
public enum UpdateType {
    ADDED("added"), REMOVED("removed"), CHANGED("changed");

    private String name;

    UpdateType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "UpdateType[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
