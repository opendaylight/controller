/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.rpc;

import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.config.facade.xml.mapping.IdentityMapping;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeReadingStrategy;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.ObjectXmlReader;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving.AttributeResolvingStrategy;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving.ObjectResolver;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry.Rpc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;

public final class InstanceRuntimeRpc {

    private final Map<String, AttributeIfc> yangToAttrConfig;
    private final Rpc rpc;
    private final EnumResolver enumResolver;

    public InstanceRuntimeRpc(Rpc rpc, final EnumResolver enumResolver) {
        this.enumResolver = enumResolver;
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
                .prepareResolving(yangToAttrConfig, enumResolver);
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

    public Map<String, AttributeConfigElement> fromXml(XmlElement configRootNode) throws DocumentedException {
        Map<String, AttributeConfigElement> retVal = Maps.newHashMap();

        // FIXME add identity map to runtime data
        Map<String, AttributeReadingStrategy> strats = new ObjectXmlReader().prepareReading(yangToAttrConfig,
                Collections.<String, Map<Date, IdentityMapping>> emptyMap());

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
