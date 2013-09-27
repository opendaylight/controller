/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDeclaration;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.TypeDeclaration;

public class GeneralInterfaceTemplate extends AbstractFtlTemplate {

    public GeneralInterfaceTemplate(Header header, String packageName,
            String name, List<String> extendedInterfaces,
            List<MethodDeclaration> methods) {
        super(header, packageName, Collections.<Field> emptyList(), methods,
                new TypeDeclaration("interface", name, extendedInterfaces,
                        Collections.<String> emptyList()));
    }
}
