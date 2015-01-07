/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.codec.observers;

import org.opendaylight.datasand.codec.AttributeDescriptor;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 *
 */
public class DefaultPOJOTypeAttribute implements ITypeAttributeObserver{
    @Override
    public boolean isTypeAttribute(AttributeDescriptor ad) {
        if(ad.getReturnType().isPrimitive()) return false;
        if(ad.getReturnType().getName().startsWith("java.")) return false;
        return true;
    }
}
