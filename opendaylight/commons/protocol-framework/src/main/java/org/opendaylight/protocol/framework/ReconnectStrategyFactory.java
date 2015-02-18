/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

/**
 * Factory interface for creating new ReconnectStrategy instances. This is
 * primarily useful for allowing injection of a specific type of strategy for
 * on-demand use, pretty much like you would use a ThreadFactory.
 */
@Deprecated
public interface ReconnectStrategyFactory {
    /**
     * Create a new ReconnectStrategy.
     *
     * @return a new reconnecty strategy
     */
    ReconnectStrategy createReconnectStrategy();
}

