/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.listener.type.command;

/**
 * Sent to the listener actor to stop it on demand ie during listener unregistration.
 */
public final class TerminateListener extends TypeListenerCommand {

    public static final TerminateListener INSTANCE = new TerminateListener();

    private TerminateListener() {
        // Hidden on purpose
    }
}
