/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

/**
 * Base class for various factory-style callbacks invoked when a message
 * is received.
 *
 * <D> delegate type
 * <M> message type
 */
abstract class DelegateFactory<M, D> {
    abstract D createDelegate(M message);
}
