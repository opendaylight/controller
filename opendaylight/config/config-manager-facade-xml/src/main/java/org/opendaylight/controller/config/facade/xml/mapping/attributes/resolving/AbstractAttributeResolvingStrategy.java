/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.resolving;

import javax.management.openmbean.OpenType;

abstract class AbstractAttributeResolvingStrategy<T, O extends OpenType<?>> implements AttributeResolvingStrategy<T, O> {
    private O openType;

    public AbstractAttributeResolvingStrategy(O openType) {
        this.openType = openType;
    }

    @Override
    public O getOpenType() {
        return openType;
    }

    /**
     * Composite types might change during resolution. Use this setter to update open type
     */
    public void setOpenType(final O openType) {
        this.openType = openType;
    }
}
