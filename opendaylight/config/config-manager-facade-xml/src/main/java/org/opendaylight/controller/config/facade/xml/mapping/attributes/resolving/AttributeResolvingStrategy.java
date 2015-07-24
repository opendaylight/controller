/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving;

import com.google.common.base.Optional;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.config.util.xml.DocumentedException;

/**
 * Create real object from String or Map that corresponds to given opentype.
 */
public interface AttributeResolvingStrategy<T, O extends OpenType<?>> {
    O getOpenType();

    Optional<T> parseAttribute(String attrName, Object value) throws DocumentedException;
}
