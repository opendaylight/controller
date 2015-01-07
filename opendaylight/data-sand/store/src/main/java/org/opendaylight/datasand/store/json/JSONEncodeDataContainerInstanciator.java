/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store.json;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.EncodeDataContainerFactory.EncodeDataContainerInstantiator;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.json.JsonEncodeDataContainer;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class JSONEncodeDataContainerInstanciator implements EncodeDataContainerInstantiator{
    @Override
    public EncodeDataContainer newEncodeDataContainer(Object data, Object key,TypeDescriptor _ts) {
        return new JsonEncodeDataContainer(_ts);
    }
}
