/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Constructor;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;

public class AbstractFactoryTemplate extends GeneralClassTemplate {

    private static final List<String> IMPLEMENTED_IFCS = Lists
            .newArrayList(ModuleFactory.class.getCanonicalName());

    public AbstractFactoryTemplate(Header header, String packageName,
                                   String abstractFactoryName,
                                   List<Field> fields) {
        super(header, packageName, abstractFactoryName, Collections
                .<String> emptyList(), IMPLEMENTED_IFCS, fields, Collections
                .<MethodDefinition> emptyList(), true, false, Collections
                .<Constructor> emptyList());
    }
}
