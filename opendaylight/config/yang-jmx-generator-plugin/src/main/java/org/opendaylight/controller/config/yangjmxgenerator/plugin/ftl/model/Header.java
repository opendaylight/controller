/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

public class Header {
    private final String yangModuleName, yangModuleLocalName;

    public Header(String yangModuleName, String yangModuleLocalName) {
        super();
        this.yangModuleName = yangModuleName;
        this.yangModuleLocalName = yangModuleLocalName;
    }

    public String getYangModuleName() {
        return yangModuleName;
    }

    public String getYangModuleLocalName() {
        return yangModuleLocalName;
    }

    @Override
    public String toString() {
        return "yang module name: " + yangModuleName + " "
                + " yang module local name: " + yangModuleLocalName;
    }

}
