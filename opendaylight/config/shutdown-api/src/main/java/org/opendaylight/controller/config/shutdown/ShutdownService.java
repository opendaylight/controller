/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.shutdown;

import com.google.common.base.Optional;

public interface ShutdownService {

    /**
     * Shut down the server.
     *
     * @param inputSecret must match configured secret of the implementation
     * @param reason Optional string to be logged while shutting down
     */
    void shutdown(String inputSecret, Long maxWaitTime, Optional<String> reason);
}
