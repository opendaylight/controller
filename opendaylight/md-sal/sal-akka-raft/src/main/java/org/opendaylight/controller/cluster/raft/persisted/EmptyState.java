/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

/**
 * Empty Snapshot State implementation.
 *
 * @author Thomas Pantelis
 */
public class EmptyState implements Snapshot.State {
    private static final long serialVersionUID = 1L;

    public static final EmptyState INSTANCE = new EmptyState();

    private EmptyState() {
    }

    @SuppressWarnings("static-method")
    private Object readResolve() {
        return INSTANCE;
    }
}
