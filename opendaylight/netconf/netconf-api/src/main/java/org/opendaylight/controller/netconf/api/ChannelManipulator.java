/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.api;

import io.netty.channel.ChannelHandler;

public class ChannelManipulator {

    private String name;
    private String baseName;
    private ChannelHandler handler;

    public ChannelManipulator(String name, String baseName,
            ChannelHandler handler) {

        this.name = name;
        this.baseName = baseName;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return baseName;
    }

    public ChannelHandler getHandler() {
        return handler;
    }

}
