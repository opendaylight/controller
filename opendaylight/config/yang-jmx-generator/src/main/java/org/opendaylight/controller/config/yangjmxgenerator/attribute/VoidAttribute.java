/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import javax.management.openmbean.OpenType;

public class VoidAttribute implements AttributeIfc {

    private static VoidAttribute instance = new VoidAttribute();

    public static VoidAttribute getInstance() {
        return instance;
    }

    private VoidAttribute() {
    }

    @Override
    public String getAttributeYangName() {
        return null;
    }

    @Override
    public String getNullableDescription() {
        return null;
    }

    @Override
    public String getNullableDefault() {
        return null;
    }

    @Override
    public String getUpperCaseCammelCase() {
        return null;
    }

    @Override
    public String getLowerCaseCammelCase() {
        return null;
    }

    @Override
    public OpenType<?> getOpenType() {
        return null;
    }
}
