/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.logback.config.loader.test.logwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * dummy logging guy
 */
public class Tracer {

    private static final Logger LOG = LoggerFactory.getLogger(Tracer.class);

    /**
     * all logging
     */
    public static void doSomeAction() {
        LOG.trace("tracing");
        LOG.debug("debugging");
        LOG.info("infoing");
        LOG.warn("warning");
        LOG.error("erroring");
    }

}
