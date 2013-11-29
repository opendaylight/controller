/**
 * @author Tomas Olvecky
 *
 * 11 2013
 *
 * Copyright (c) 2013 by Cisco Systems, Inc.
 * All rights reserved.
 */
package org.opendaylight.controller.netconf.persist.impl.osgi;

import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesProviderBaseImpl implements PropertiesProvider {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesProviderBaseImpl.class);
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
        logger.trace("Full key {}", fullKey);
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
