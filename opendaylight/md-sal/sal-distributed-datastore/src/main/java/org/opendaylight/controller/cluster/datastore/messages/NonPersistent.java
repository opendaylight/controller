/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

/**
 * A NonPersistent message is to be used when we want to trigger state update
 * for an actor without actually persisting the data to disk. This could be
 * useful for test purposes.
 */
public class NonPersistent {
    private final Object payload;

    public NonPersistent(Object payload){
        this.payload = payload;
    }

    public Object payload() {
        return payload;
    }

    public static NonPersistent create(Object payload){
        return new NonPersistent(payload);
    }
}
