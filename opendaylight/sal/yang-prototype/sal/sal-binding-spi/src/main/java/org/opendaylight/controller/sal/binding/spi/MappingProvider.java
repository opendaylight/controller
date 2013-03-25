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

public interface MappingProvider {

    <T extends DataObject> Mapper<T> getMapper(Class<T> type);
    Mapper<DataObject> getMapper(QName name);

    <T extends MappingExtension> MappingExtensionFactory<T> getExtensionFactory(Class<T> cls);

    public interface MappingExtension {

    }
    
    public interface MappingExtensionFactory<T> {
        T forClass(Class<?> obj);
    }

}
