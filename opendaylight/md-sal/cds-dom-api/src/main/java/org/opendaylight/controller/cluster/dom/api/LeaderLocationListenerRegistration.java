/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.dom.api;

import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Registration of a {@link LeaderLocationListener}.
 *
 * @param <T> Listener type
 */
public interface LeaderLocationListenerRegistration<T extends LeaderLocationListener> extends ListenerRegistration<T> {
    // Just a specialization
}
