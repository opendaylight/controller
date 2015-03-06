/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.messagebus.app.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Providers {
    private static final Logger LOGGER = LoggerFactory.getLogger(Providers.class);

    public static class BindingAware implements BindingAwareProvider, AutoCloseable {


        @Override
        public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
            LOGGER.info("BindingAwareBroker.ProviderContext initialized");
        }

        @Override
        public void close() throws Exception {}
    }

    public static class BindingIndependent extends AbstractProvider implements AutoCloseable {

        @Override
        public void onSessionInitiated(final Broker.ProviderSession session) {
            LOGGER.info("Broker.ProviderSession initialized");
        }

        @Override
        public void close() throws Exception {}
    }

}
