/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configuration;

public enum ConfigurationEvent {
    SAVE("Save"),
    BACKUP("Backup"),
    RESTORE("Restore"),
    DELETE("Delete");

    private ConfigurationEvent(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }

    public static ConfigurationEvent fromString(String pName) {
        for(ConfigurationEvent p:ConfigurationEvent.values()) {
            if (p.toString().equals(pName)) {
                return p;
            }
        }
        return null;
    }
}
