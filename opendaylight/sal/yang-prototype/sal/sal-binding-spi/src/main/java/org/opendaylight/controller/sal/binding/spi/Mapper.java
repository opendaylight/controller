/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.spi;

import org.opendaylight.controller.yang.binding.DataObject;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
/**
 * Translator between Binding-Independent format and generated Binding Data Objects
 * 
 * 
 * 
 * 
 * @param <T> Result Type 
 */
public interface Mapper<T extends DataObject> {

    QName getQName();
    Class<T> getDataObjectClass();    
    T objectFromDom(CompositeNode object);
    
    /**
     * 
     * @param obj
     * @return
     * @throws IllegalArgumentException 
     */
    CompositeNode domFromObject(DataObject obj) throws IllegalArgumentException;

}
