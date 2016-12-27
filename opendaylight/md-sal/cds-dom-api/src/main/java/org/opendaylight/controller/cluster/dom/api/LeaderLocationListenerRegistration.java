/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.dom.api;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Registration of a {@link LeaderLocationListener}.
 *
 * @author Robert Varga
 *
 * @param <T> Listener type
 */
@Beta
public interface LeaderLocationListenerRegistration<T extends LeaderLocationListener> extends ListenerRegistration<T> {

}
