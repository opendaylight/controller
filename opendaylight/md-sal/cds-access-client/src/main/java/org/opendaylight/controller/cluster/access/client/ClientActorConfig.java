/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface for client actor configuration parameters.
 *
 * @author Thomas Pantelis
 */
public interface ClientActorConfig {
    /**
     * Returns the maximum size in bytes for a message slice when fragmenting messages thru the akka remoting framework.
     *
     * @return the maximum size in bytes
     */
    int getMaximumMessageSliceSize();

    /**
     * Returns the threshold in bytes before switching from storing in memory to buffering to a file when streaming
     * large amounts of data.
     *
     * @return the threshold in bytes
     */
    int getFileBackedStreamingThreshold();

    /**
     * Returns the directory in which to create temporary files.
     *
     * @return the directory name
     */
    @NonNull Path getTempFileDirectory();

    /**
     * Returns the timer interval whereby, on expiration after response inactivity from the back-end, the connection to
     * the back-end is torn down and reconnection is attempted.

     * @return timer interval in nanoseconds.
     */
    long getBackendAlivenessTimerInterval();

    /**
     * Returns the timeout interval whereby requests are failed.
     *
     * @return the timeout interval in nanoseconds.
     */
    long getRequestTimeout();

    /**
     * Returns the timeout interval whereby the client front-end hasn't made progress with the back-end on any request
     * and terminates.
     *
     * @return the timeout interval in nanoseconds.
     */
    long getNoProgressTimeout();
}
