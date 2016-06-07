/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

/**
 * Common interface for client histories and client transactions, which can be aborted immediately without replicating
 * the effect to the backend. This is needed for abrupt shutdowns.
 *
 * Since classes which need to expose this functionality do not need a base class, this is an abstract class and not
 * an interface -- which allows us to not leak the {@link #localAbort(Throwable)} method.
 *
 * @author Robert Varga
 */
abstract class LocalAbortable {
    /**
     * Immediately abort this object.
     *
     * @param cause Failure which caused this abort.
     */
    abstract void localAbort(Throwable cause);
}
