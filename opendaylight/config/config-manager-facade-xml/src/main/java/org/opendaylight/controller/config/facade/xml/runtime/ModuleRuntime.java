/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.runtime;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import javax.management.ObjectName;
import org.opendaylight.controller.config.facade.xml.mapping.config.ModuleConfig;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ModuleRuntime {

    private final InstanceRuntime instanceRuntime;

    public ModuleRuntime(InstanceRuntime instanceRuntime) {
        this.instanceRuntime = instanceRuntime;
    }

    private ObjectName findRoot(Collection<ObjectName> runtimeBeanOns) {
        for (ObjectName objectName : runtimeBeanOns) {
            if (objectName.getKeyPropertyList().size() == 3){
                return objectName;
            }
        }
        throw new IllegalStateException("Root runtime bean not found among " + runtimeBeanOns);
    }

    public Element toXml(String namespace, Collection<ObjectName> runtimeBeanOns,
                         Document document, ModuleConfig moduleConfig, ObjectName configBeanON, final EnumResolver enumResolver) {

        Element moduleElement = moduleConfig.toXml(configBeanON, document, namespace, enumResolver);

        ObjectName rootName = findRoot(runtimeBeanOns);

        Set<ObjectName> childrenRuntimeBeans = Sets.newHashSet(runtimeBeanOns);
        childrenRuntimeBeans.remove(rootName);

        // FIXME: why is this called and not used?
        instanceRuntime.toXml(rootName, childrenRuntimeBeans, document, moduleElement, namespace, enumResolver);

        return moduleElement;
    }

}
