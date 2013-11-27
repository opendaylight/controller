/**
 * @author Tomas Olvecky
 *
 * 11 2013
 *
 * Copyright (c) 2013 by Cisco Systems, Inc.
 * All rights reserved.
 */
package org.opendaylight.controller.netconf.persist.impl;

import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.netconf.persist.impl.osgi.PropertiesProviderBaseImpl;

public class PropertiesProviderAdapterImpl implements PropertiesProvider {
    private final PropertiesProviderBaseImpl inner;
    private final String index;

    public PropertiesProviderAdapterImpl(PropertiesProviderBaseImpl inner, String index) {
        this.inner = inner;
        this.index = index;
    }

    @Override
    public String getProperty(String key) {
        String fullKey = getFullKeyForReporting(key);
        return inner.getPropertyWithoutPrefix(fullKey);
    }

    public String getPrefix() {
        return inner.getPrefix() + "." + index + ".properties";
    }

    @Override
    public String getFullKeyForReporting(String key) {
        return getPrefix()  + "." + key;
    }
}
