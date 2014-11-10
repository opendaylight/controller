
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connection;

/**
 * ConnectionConstants
 * Expand this enum as and when needed to support other connection parameters that
 * might be needed for certain protocol plugins.
 */
public enum ConnectionConstants {
    ADDRESS("address"),
    PORT("port"),
    PROTOCOL("protocol"),
    USERNAME("username"),
    PASSWORD("password");

    private ConnectionConstants(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}