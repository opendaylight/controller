/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.spi;

import org.opendaylight.controller.yang.binding.DataObject;
import org.opendaylight.controller.yang.binding.RpcService;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;

public interface MappingProvider {

    <T extends DataObject> Mapper<T> mapperForClass(Class<T> type);
    Mapper<DataObject> mapperForQName(QName name);
    
    /**
     * Returns {@link RpcMapper} associated to class
     * 
     * @param type Class for which RpcMapper should provide mapping
     * @return
     */
    <T extends RpcService> RpcMapper<T> rpcMapperForClass(Class<T> type);
    
    /**
     * Returns {@link RpcMapper} associated to the {@link RpcService} proxy.
     * 
     * @param proxy
     * @return
     */
    RpcMapper<? extends RpcService> rpcMapperForProxy(RpcService proxy);
    
    /**
     * 
     * 
     * @param rpc
     * @param inputNode
     * @return
     */
    RpcMapper<? extends RpcService> rpcMapperForData(QName rpc,
            CompositeNode inputNode);

    <T extends MappingExtension> MappingExtensionFactory<T> getExtensionFactory(Class<T> cls);

    public interface MappingExtension {

    }
    
    public interface MappingExtensionFactory<T> {
        T forClass(Class<?> obj);
    }



}
