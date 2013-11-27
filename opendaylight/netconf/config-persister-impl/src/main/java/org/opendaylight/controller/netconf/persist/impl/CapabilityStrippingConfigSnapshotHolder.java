/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;

public class CapabilityStrippingConfigSnapshotHolder implements ConfigSnapshotHolder {
    private static final Logger logger = LoggerFactory.getLogger(CapabilityStrippingConfigSnapshotHolder.class);

    private final String configSnapshot;
    private final StripCapabilitiesResult stripCapabilitiesResult;

    public CapabilityStrippingConfigSnapshotHolder(Element snapshot, Set<String> capabilities, Pattern ignoredMissingCapabilityRegex) {
        final XmlElement configElement = XmlElement.fromDomElement(snapshot);
        configSnapshot = XmlUtil.toString(configElement.getDomElement());
        stripCapabilitiesResult = stripCapabilities(configElement, capabilities, ignoredMissingCapabilityRegex);
    }

    private static class StripCapabilitiesResult {
        private final SortedSet<String> requiredCapabilities, missingNamespaces;

        private StripCapabilitiesResult(SortedSet<String> requiredCapabilities, SortedSet<String> missingNamespaces) {
            this.requiredCapabilities = Collections.unmodifiableSortedSet(requiredCapabilities);
            this.missingNamespaces = Collections.unmodifiableSortedSet(missingNamespaces);
        }
    }


    @VisibleForTesting
    static StripCapabilitiesResult stripCapabilities(XmlElement configElement, Set<String> allCapabilitiesFromHello,
                                                     Pattern ignoredMissingCapabilityRegex) {
        // collect all namespaces
        Set<String> foundNamespacesInXML = getNamespaces(configElement);
        logger.trace("All capabilities {}\nFound namespaces in XML {}", allCapabilitiesFromHello, foundNamespacesInXML);
        // required are referenced both in xml and hello
        SortedSet<String> requiredCapabilities = new TreeSet<>();
        // can be removed
        Set<String> obsoleteCapabilities = new HashSet<>();
        // are in xml but not in hello
        SortedSet<String> missingNamespaces = new TreeSet<>(foundNamespacesInXML);
        for (String capability : allCapabilitiesFromHello) {
            String namespace = capability.replaceAll("\\?.*","");
            if (foundNamespacesInXML.contains(namespace)) {
                requiredCapabilities.add(capability);
                checkState(missingNamespaces.remove(namespace));
            } else {
                obsoleteCapabilities.add(capability);
            }
        }

        logger.trace("Required capabilities {}, \nObsolete capabilities {}",
                requiredCapabilities, obsoleteCapabilities);

        for(Iterator<String> iterator = missingNamespaces.iterator();iterator.hasNext(); ){
            String capability = iterator.next();
            if (ignoredMissingCapabilityRegex.matcher(capability).matches()){
                logger.trace("Ignoring missing capability {}", capability);
                iterator.remove();
            }
        }
        if (missingNamespaces.size() > 0) {
            logger.warn("Some capabilities are missing: {}", missingNamespaces);
        }
        return new StripCapabilitiesResult(requiredCapabilities, missingNamespaces);
    }

    static Set<String> getNamespaces(XmlElement element){
        Set<String> result = new HashSet<>();
        for (Entry<String,Attr> attribute : element.getAttributes().entrySet()) {
            if  (attribute.getKey().startsWith("xmlns")){
                result.add(attribute.getValue().getValue());
            }
        }
        //element.getAttributes()
        for(XmlElement child: element.getChildElements()) {
            result.addAll(getNamespaces(child));
        }
        return result;
    }

    @Override
    public SortedSet<String> getCapabilities() {
        return stripCapabilitiesResult.requiredCapabilities;
    }

    @VisibleForTesting
    Set<String> getMissingNamespaces(){
        return stripCapabilitiesResult.missingNamespaces;
    }

    @Override
    public String getConfigSnapshot() {
        return configSnapshot;
    }
}
