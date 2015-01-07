/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents.cmap;

import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class TypeDescriptorListener<K,V> implements ICMapListener<K, V>{
    private TypeDescriptorsContainer container = null;
    public TypeDescriptorListener(TypeDescriptorsContainer _container){
        this.container = _container;
    }
    @Override
    public void peerPut(K key, V value) {
        container.save();
    }
    @Override
    public void peerRemove(K key) {
        container.save();
    }
}
