/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.rpc;

import com.google.common.collect.Maps;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry.Rpc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeReadingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.ObjectXmlReader;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving.AttributeResolvingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving.ObjectResolver;
import org.opendaylight.controller.netconf.util.xml.XmlElement;

import javax.management.openmbean.OpenType;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class InstanceRuntimeRpc {

    private final Map<String, AttributeIfc> yangToAttrConfig;
    private final Rpc rpc;

    public InstanceRuntimeRpc(Rpc rpc) {
        this.yangToAttrConfig = map(rpc.getParameters());
        this.rpc = rpc;
    }

    private Map<String, AttributeIfc> map(List<JavaAttribute> parameters) {
        Map<String, AttributeIfc> mapped = Maps.newHashMap();
        for (JavaAttribute javaAttribute : parameters) {
            mapped.put(javaAttribute.getAttributeYangName(), javaAttribute);
        }
        return mapped;
    }

    private void resolveConfiguration(Map<String, AttributeConfigElement> mappedConfig) {

        // TODO make field, resolvingStrategies can be instantiated only once
        Map<String, AttributeResolvingStrategy<?, ? extends OpenType<?>>> resolvingStrategies = new ObjectResolver(null)
                .prepareResolving(yangToAttrConfig);
        // TODO make constructor for object resolver without service tracker
        for (Entry<String, AttributeConfigElement> configDefEntry : mappedConfig.entrySet()) {
            try {

                AttributeResolvingStrategy<?, ? extends OpenType<?>> attributeResolvingStrategy = resolvingStrategies
                        .get(configDefEntry.getKey());

                configDefEntry.getValue().resolveValue(attributeResolvingStrategy, configDefEntry.getKey());
                configDefEntry.getValue().setJmxName(
                        yangToAttrConfig.get(configDefEntry.getKey()).getUpperCaseCammelCase());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to resolve value " + configDefEntry.getValue()
                        + " to attribute " + configDefEntry.getKey(), e);
            }
        }
    }

    public Map<String, AttributeConfigElement> fromXml(XmlElement configRootNode) {
        Map<String, AttributeConfigElement> retVal = Maps.newHashMap();

        Map<String, AttributeReadingStrategy> strats = new ObjectXmlReader().prepareReading(yangToAttrConfig);

        for (Entry<String, AttributeReadingStrategy> readStratEntry : strats.entrySet()) {
            List<XmlElement> configNodes = configRootNode.getChildElements(readStratEntry.getKey());
            AttributeConfigElement readElement = readStratEntry.getValue().readElement(configNodes);
            retVal.put(readStratEntry.getKey(), readElement);
        }

        resolveConfiguration(retVal);
        return retVal;
    }

    public String getName() {
        return rpc.getName();
    }

    public AttributeIfc getReturnType() {
        return rpc.getReturnType();
    }

}
