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
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;

public class StubFactoryTemplate extends GeneralClassTemplate {

    public StubFactoryTemplate(Header header, String packageName, String name,
                               String extendedClass) {
        super(header, packageName, name, Lists.newArrayList(extendedClass),
                Collections.<String> emptyList(), Collections
                        .<Field> emptyList(), Collections
                        .<MethodDefinition> emptyList());
    }

}
