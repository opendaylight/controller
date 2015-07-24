/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.impl.osgi;

import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesProviderBaseImpl implements PropertiesProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesProviderBaseImpl.class);
    private final BundleContext bundleContext;

    public PropertiesProviderBaseImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public String getProperty(String key) {
        String fullKey = getFullKeyForReporting(key);
        return getPropertyWithoutPrefix(fullKey);
    }

    public String getPropertyWithoutPrefix(String fullKey){
        LOG.trace("Full key {}", fullKey);
        return bundleContext.getProperty(fullKey);
    }

    public String getPrefix(){
        return ConfigPersisterActivator.NETCONF_CONFIG_PERSISTER;
    }

    @Override
    public String getFullKeyForReporting(String key) {
        return getPrefix() + "." + key;
    }
}
