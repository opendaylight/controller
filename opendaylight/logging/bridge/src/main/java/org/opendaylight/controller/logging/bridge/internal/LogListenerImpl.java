
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.logging.bridge.internal;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;

public class LogListenerImpl implements LogListener {
    private Logger logger = null;

    public LogListenerImpl(Logger l) {
        this.logger = l;
    }

    @Override
    public void logged(LogEntry entry) {
        if (this.logger != null) {
            switch (entry.getLevel()) {
            case LogService.LOG_DEBUG:
                this.logger.debug(entry.getMessage());
                break;
            case LogService.LOG_INFO:
                this.logger.info(entry.getMessage());
                break;
            case LogService.LOG_WARNING:
                this.logger.warn(entry.getMessage());
                break;
            case LogService.LOG_ERROR:
                this.logger.error(entry.getMessage());
                break;
            }
        }
    }
}
