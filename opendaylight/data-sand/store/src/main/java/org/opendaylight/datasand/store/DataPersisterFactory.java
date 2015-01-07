/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.store;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.store.bytearray.ByteArrayDataPersister;
import org.opendaylight.datasand.store.sqlite.SQLiteDataPersister;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class DataPersisterFactory {
    public static DataPersister newDataPersister(int encoderType, Shard _shard,Class<?> _type,TypeDescriptorsContainer _container){
        switch(encoderType){
            case EncodeDataContainer.ENCODER_TYPE_BYTE_ARRAY:
                return new ByteArrayDataPersister(_shard, _type, _container);
            case EncodeDataContainer.ENCODER_TYPE_SQLITE:
                return new SQLiteDataPersister(_shard, _type, _container);
        }
        return null;
    }
}
