/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

/**
 * Type of data that goes in {@link org.opendaylight.controller.remote.rpc.registry.gossip.Bucket}.
 * The implementers should do deep cloning in copy() method.
 */
public interface Copier<T> {
    T copy();
}
