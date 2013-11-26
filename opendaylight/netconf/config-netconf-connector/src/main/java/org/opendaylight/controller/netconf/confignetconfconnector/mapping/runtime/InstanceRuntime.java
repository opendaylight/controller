/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.runtime;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.InstanceConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.ObjectName;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class InstanceRuntime {

    /**
     *
     */
    private static final String KEY_ATTRIBUTE_KEY = "key";

    private final InstanceConfig instanceMapping;
    private final Map<String, InstanceRuntime> childrenMappings;
    private final Map<String, String> jmxToYangChildRbeMapping;

    public InstanceRuntime(InstanceConfig instanceMapping, Map<String, InstanceRuntime> childrenMappings,
            Map<String, String> jmxToYangChildRbeMapping) {
        this.instanceMapping = instanceMapping;
        this.childrenMappings = childrenMappings;
        this.jmxToYangChildRbeMapping = jmxToYangChildRbeMapping;
    }

    /**
     * Finds all children runtime beans, same properties and values as current
     * root + any number of additional properties
     */
    private Set<ObjectName> findChildren(ObjectName innerRootBean, Set<ObjectName> childRbeOns) {
        final Hashtable<String, String> wantedProperties = innerRootBean.getKeyPropertyList();

        return Sets.newHashSet(Collections2.filter(childRbeOns, new Predicate<ObjectName>() {

            @Override
            public boolean apply(ObjectName on) {
                Hashtable<String, String> localProperties = on.getKeyPropertyList();
                for (Entry<String, String> propertyEntry : wantedProperties.entrySet()) {
                    if (!localProperties.containsKey(propertyEntry.getKey()))
                        return false;
                    if (!localProperties.get(propertyEntry.getKey()).equals(propertyEntry.getValue()))
                        return false;
                    if (localProperties.size() <= wantedProperties.size())
                        return false;
                }
                return true;
            }
        }));
    }

    /**
     * Finds next level root runtime beans, beans that have the same properties
     * as current root + one additional
     */
    private Set<ObjectName> getRootBeans(Set<ObjectName> childRbeOns, final String string, final int keyListSize) {
        return Sets.newHashSet(Collections2.filter(childRbeOns, new Predicate<ObjectName>() {

            @Override
            public boolean apply(ObjectName on) {
                if (on.getKeyPropertyList().size() != keyListSize + 1)
                    return false;
                if (!on.getKeyPropertyList().containsKey(string))
                    return false;
                return true;
            }
        }));
    }

    public Element toXml(ObjectName rootOn, Set<ObjectName> childRbeOns, Document document, Element parentElement, String namespace) {
        return toXml(rootOn, childRbeOns, document, null, parentElement, namespace);
    }

    public Element toXml(ObjectName rootOn, Set<ObjectName> childRbeOns, Document document, String instanceIndex,
                         Element parentElement, String namespace) {
        // TODO namespace
        Element xml = instanceMapping.toXml(rootOn, null, namespace, document, parentElement);

        if (instanceIndex != null) {
            xml.setAttribute(KEY_ATTRIBUTE_KEY, instanceIndex);
        }

        for (Entry<String, InstanceRuntime> childMappingEntry : childrenMappings.entrySet()) {
            Set<ObjectName> innerRootBeans = getRootBeans(childRbeOns, childMappingEntry.getKey(), rootOn
                    .getKeyPropertyList().size());

            for (ObjectName objectName : innerRootBeans) {
                Set<ObjectName> innerChildRbeOns = findChildren(objectName, childRbeOns);
                String runtimeInstanceIndex = objectName.getKeyProperty(childMappingEntry.getKey());

                String elementName = jmxToYangChildRbeMapping.get(childMappingEntry.getKey());

                Element innerXml = document.createElement(elementName);
                childMappingEntry.getValue().toXml(objectName, innerChildRbeOns, document,
                        runtimeInstanceIndex, innerXml, namespace);
                xml.appendChild(innerXml);
            }
        }

        return xml;
    }

}
