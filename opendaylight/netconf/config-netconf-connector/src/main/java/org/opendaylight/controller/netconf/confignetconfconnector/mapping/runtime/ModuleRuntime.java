/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.runtime;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.ObjectName;
import java.util.Collection;
import java.util.Set;

public class ModuleRuntime {

    private final String moduleName;
    private final InstanceRuntime instanceRuntime;

    public ModuleRuntime(String moduleName, InstanceRuntime instanceRuntime) {
        this.moduleName = moduleName;
        this.instanceRuntime = instanceRuntime;
    }

    public InstanceRuntime getMbeanMapping() {
        return instanceRuntime;
    }

    private ObjectName findRoot(Collection<ObjectName> runtimeBeanOns) {
        for (ObjectName objectName : runtimeBeanOns) {
            if (objectName.getKeyPropertyList().size() == 3)
                return objectName;
        }
        throw new IllegalStateException("Root runtime bean not found among " + runtimeBeanOns);
    }

    public Element toXml(String namespace, Multimap<String, ObjectName> instances, Document document) {
        Element root = document.createElement(XmlNetconfConstants.MODULE_KEY);
        XmlUtil.addNamespaceAttr(root, namespace);

        Element nameElement = XmlUtil.createTextElement(document, XmlNetconfConstants.NAME_KEY, moduleName);
        root.appendChild(nameElement);

        for (String instanceName : instances.keySet()) {
            Element instance = document.createElement(XmlNetconfConstants.INSTANCE_KEY);

            Element innerNameElement = XmlUtil.createTextElement(document, XmlNetconfConstants.NAME_KEY, instanceName);
            instance.appendChild(innerNameElement);

            Collection<ObjectName> runtimeBeanOns = instances.get(instanceName);
            ObjectName rootName = findRoot(runtimeBeanOns);

            Set<ObjectName> childrenRuntimeBeans = Sets.newHashSet(runtimeBeanOns);
            childrenRuntimeBeans.remove(rootName);

            instance.appendChild(instanceRuntime.toXml(rootName, childrenRuntimeBeans, document));

            root.appendChild(instance);
        }

        return root;
    }

}
