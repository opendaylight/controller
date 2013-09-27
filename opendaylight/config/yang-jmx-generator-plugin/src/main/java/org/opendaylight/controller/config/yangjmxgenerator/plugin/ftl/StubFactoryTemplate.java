/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import java.util.Collections;

import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;

import com.google.common.collect.Lists;

public class StubFactoryTemplate extends GeneralClassTemplate {

    private final String moduleInstanceType;

    public StubFactoryTemplate(Header header, String packageName, String name,
            String extendedClass, String moduleInstanceType) {
        super(header, packageName, name, Lists.newArrayList(extendedClass),
                Collections.<String> emptyList(), Collections
                        .<Field> emptyList(), Collections
                        .<MethodDefinition> emptyList());
        this.moduleInstanceType = moduleInstanceType;
    }

    public String getModuleInstanceType() {
        return moduleInstanceType;
    }

    public String getDynamicMBeanWithInstanceType() {
        return DynamicMBeanWithInstance.class.getCanonicalName();
    }
}
