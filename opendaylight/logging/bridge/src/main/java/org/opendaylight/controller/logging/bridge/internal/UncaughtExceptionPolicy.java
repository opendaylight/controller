/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.logging.bridge.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum UncaughtExceptionPolicy implements Thread.UncaughtExceptionHandler {
    ABORT {
        public static final int EXIT_CODE = 1;

        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            log.error("Thread {} died because of an uncaught exception, forcing virtual machine shutdown", t, e);
            System.exit(EXIT_CODE);
        }
    },
    IGNORE {
        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
            log.error("Thread {} died because of an uncaught exception", t, e);
        }
    };

    private static final Logger log = LoggerFactory.getLogger(UncaughtExceptionPolicy.class);
}
