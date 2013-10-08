/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.xml;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

import com.google.common.collect.ImmutableMap;

// http://www.ibm.com/developerworks/library/x-nmspccontext/
public class HardcodedNamespaceResolver implements NamespaceContext {
    private final Map<String/* prefix */, String/* namespace */> prefixesToNamespaces;

    public HardcodedNamespaceResolver(String prefix, String namespace) {
        this(ImmutableMap.of(prefix, namespace));
    }

    public HardcodedNamespaceResolver(Map<String, String> prefixesToNamespaces) {
        this.prefixesToNamespaces = Collections.unmodifiableMap(prefixesToNamespaces);
    }

    /**
     * This method returns the uri for all prefixes needed. Wherever possible it
     * uses XMLConstants.
     *
     * @param prefix
     * @return uri
     */
    @Override
    public String getNamespaceURI(String prefix) {
        if (prefixesToNamespaces.containsKey(prefix)) {
            return prefixesToNamespaces.get(prefix);
        } else {
            throw new IllegalStateException("Prefix mapping not found for " + prefix);
        }
    }

    @Override
    public String getPrefix(String namespaceURI) {
        // Not needed in this context.
        return null;
    }

    @Override
    public Iterator<?> getPrefixes(String namespaceURI) {
        // Not needed in this context.
        return null;
    }

}
