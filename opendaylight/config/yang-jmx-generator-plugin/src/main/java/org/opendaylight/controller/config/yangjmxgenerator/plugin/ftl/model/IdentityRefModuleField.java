/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import org.opendaylight.controller.config.api.IdentityAttributeRef;

public class IdentityRefModuleField extends ModuleField {

    public static final String IDENTITY_CLASS_FIELD_SUFFIX = "IdentityClass";
    private final String identityBaseClass;

    public IdentityRefModuleField(String type, String name, String attributeName, String identityBaseClass) {
        super(type, name, attributeName, null, false, null, false, false);
        this.identityBaseClass = identityBaseClass;
    }

    public String getIdentityBaseClass() {
        return identityBaseClass;
    }

    @Override
    public boolean isIdentityRef() {
        return true;
    }

    public String getType() {
        return IdentityAttributeRef.class.getName();
    }

    public String getIdentityClassType() {
        return super.getType();
    }

    public String getIdentityClassName() {
        return addIdentityClassFieldSuffix(getName());
    }

    public static String addIdentityClassFieldSuffix(String prefix) {
        return prefix + IDENTITY_CLASS_FIELD_SUFFIX;
    }
}
