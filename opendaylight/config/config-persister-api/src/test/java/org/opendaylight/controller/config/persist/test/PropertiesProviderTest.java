/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.test;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;

public class PropertiesProviderTest implements PropertiesProvider {
    private final Map<String,String> properties = new HashMap<>();

    public void addProperty(String key,String value){
        properties.put(key,value);
    }
    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String getFullKeyForReporting(String key) {
        return null;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public String getPropertyWithoutPrefix(String fullKey) {
        return null;
    }
}
