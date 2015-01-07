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
import org.opendaylight.datasand.codec.TypeDescriptor;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 *
 */
public class DefaultChildAttributeObserver implements IChildAttributeObserver{

    @Override
    public boolean isChildAttribute(AttributeDescriptor ad) {
        if(ad.isCollection() && !ad.getReturnType().getPackage().getName().startsWith("java"))
            return true;
        return false;
    }

    @Override
    public boolean isChildAttribute(TypeDescriptor td) {
        if(!td.getTypeClassName().startsWith("java"))
            return true;
        return false;
    }

    @Override
    public boolean supportAugmentation(AttributeDescriptor ad) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportAugmentation(TypeDescriptor td) {
        // TODO Auto-generated method stub
        return false;
    }

}
