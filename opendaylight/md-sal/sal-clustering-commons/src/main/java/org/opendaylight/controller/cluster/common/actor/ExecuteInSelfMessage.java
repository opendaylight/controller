/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import static java.util.Objects.requireNonNull;

import akka.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Message internal to {@link ExecuteInSelfActor} implementations in this package.
 *
 * @author Robert Varga
 */
final class ExecuteInSelfMessage implements ControlMessage {
    private final Runnable runnable;

    ExecuteInSelfMessage(final @NonNull Runnable runnable) {
        this.runnable = requireNonNull(runnable);
    }

    void run() {
        runnable.run();
    }
}
