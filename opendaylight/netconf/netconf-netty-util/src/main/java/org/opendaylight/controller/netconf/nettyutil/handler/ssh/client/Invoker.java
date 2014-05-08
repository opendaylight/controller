/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import java.io.IOException;

/**
 * Abstract class providing mechanism of invoking various SSH level services.
 * Class is not allowed to be extended, as it provides its own implementations via instance initiators.
 */
abstract class Invoker {
    private boolean invoked = false;

    private Invoker() {
    }

    protected boolean isInvoked() {
        return invoked;
    }

    public void setInvoked() {
        this.invoked = true;
    }

    abstract void invoke(SshSession session) throws IOException;

    public static Invoker netconfSubsystem(){
        return subsystem("netconf");
    }

    public static Invoker subsystem(final String subsystem) {
        return new Invoker() {
            @Override
            synchronized void invoke(SshSession session) throws IOException {
                if (isInvoked()) {
                    throw new IllegalStateException("Already invoked.");
                }
                try {
                    session.startSubSystem(subsystem);
                } finally {
                    setInvoked();
                }
            }
        };
    }
}
