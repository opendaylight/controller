/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.resolving;

import com.google.common.base.Optional;

import javax.management.openmbean.OpenType;

/**
 * Create real object from String or Map that corresponds to given opentype.
 */
public interface AttributeResolvingStrategy<T, O extends OpenType<?>> {
    O getOpenType();

    Optional<T> parseAttribute(String attrName, Object value);
}
