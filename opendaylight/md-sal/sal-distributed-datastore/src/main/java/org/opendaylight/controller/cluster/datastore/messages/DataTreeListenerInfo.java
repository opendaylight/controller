/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.beans.ConstructorProperties;

/**
 * Response to a {@link GetInfo} query from a data tree listener actor.
 *
 * @author Thomas Pantelis
 */
public class DataTreeListenerInfo {
    private final String listener;
    private final String registeredPath;
    private final boolean isEnabled;

    @ConstructorProperties({"listener","registeredPath", "isEnabled"})
    public DataTreeListenerInfo(String listener, String registeredPath, boolean isEnabled) {
        this.listener = listener;
        this.registeredPath = registeredPath;
        this.isEnabled = isEnabled;
    }

    public String getListener() {
        return listener;
    }

    public String getRegisteredPath() {
        return registeredPath;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
