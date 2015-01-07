/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.codec;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public interface ISerializer {
    public void encode(Object value, byte[] byteArray, int location);

    public void encode(Object value, EncodeDataContainer ba);

    public Object decode(byte[] byteArray, int location, int length);

    public Object decode(EncodeDataContainer ba, int length);

    public String getShardName(Object obj);

    public Object getRecordKey(Object obj);
}
