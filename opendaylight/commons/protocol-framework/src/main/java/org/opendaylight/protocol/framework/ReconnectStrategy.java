/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.util.concurrent.Future;

/**
 * Interface exposed by a reconnection strategy provider. A reconnection
 * strategy decides whether to attempt reconnection and when to do that.
 *
 * The proper way of using this API is such that when a connection attempt
 * has failed, the user will call scheduleReconnect() to obtain a future,
 * which tracks schedule of the next connect attempt. The user should add its
 * own listener to be get notified when the future is done. Once the
 * the notification fires, user should examine the future to see whether
 * it is successful or not. If it is successful, the user should immediately
 * initiate a connection attempt. If it is unsuccessful, the user must
 * not attempt any more connection attempts and should abort the reconnection
 * process.
 */
@Deprecated
public interface ReconnectStrategy {
    /**
     * Query the strategy for the connect timeout.
     *
     * @return connect try timeout in milliseconds, or
     *         0 for infinite (or system-default) timeout
     * @throws Exception if the connection should not be attempted
     */
    int getConnectTimeout() throws Exception;

    /**
     * Schedule a connection attempt. The precise time when the connection
     * should be attempted is signaled by successful completion of returned
     * future.
     *
     * @param cause Cause of previous failure
     * @return a future tracking the schedule, may not be null
     * @throws IllegalStateException when a connection attempt is currently
     *         scheduled.
     */
    Future<Void> scheduleReconnect(Throwable cause);

    /**
     * Reset the strategy state. Users call this method once the reconnection
     * process succeeds.
     */
    void reconnectSuccessful();
}
