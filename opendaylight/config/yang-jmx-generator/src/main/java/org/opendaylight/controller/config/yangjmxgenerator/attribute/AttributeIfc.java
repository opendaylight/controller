/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import javax.management.openmbean.OpenType;

public interface AttributeIfc {

    /**
     * Name of attribute, starting with low case
     */
    String getAttributeYangName();

    String getNullableDescription();

    String getNullableDefault();

    String getUpperCaseCammelCase();

    String getLowerCaseCammelCase();

    OpenType<?> getOpenType();
}
