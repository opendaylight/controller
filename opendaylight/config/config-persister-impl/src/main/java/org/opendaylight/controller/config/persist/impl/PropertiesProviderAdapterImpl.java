/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.impl;

import org.opendaylight.controller.config.persist.api.PropertiesProvider;

public class PropertiesProviderAdapterImpl implements PropertiesProvider {
    private final PropertiesProvider inner;
    private final String index;

    public PropertiesProviderAdapterImpl(PropertiesProvider inner, String index) {
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
    public String getPropertyWithoutPrefix(String fullKey) {
        return inner.getPropertyWithoutPrefix(fullKey);
    }


    @Override
    public String getFullKeyForReporting(String key) {
        return getPrefix()  + "." + key;
    }
}
