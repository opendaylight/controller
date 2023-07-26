/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persistance;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class PersistenceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceProvider.class);
    public String name = null;

    @ObjectClassDefinition()
    public @interface Config {
        @AttributeDefinition(name = "enable-metric-capture")
        boolean metricCapture() default true;
        @AttributeDefinition(name = "bounded-mailbox-capacity")
        int boundedMailboxCapacity() default 1000;
    }


    @Activate
    void activate(final Config config) {
        name = "aaa";
        LOG.info("PersistenceProvider service starting");
    }
}
