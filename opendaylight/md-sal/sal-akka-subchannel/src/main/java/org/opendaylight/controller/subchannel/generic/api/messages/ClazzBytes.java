/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.messages;

import java.io.Serializable;

/**
 * Created by HanJie on 2017/2/21.
 *
 * @author Han Jie
 */
public class ClazzBytes implements Serializable {
    private Class<?> clazz;
    private byte[] data;

    public ClazzBytes(Class<?> clazz, byte[] data) {
        this.clazz = clazz;
        this.data = data;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public byte[] getData() {
        return data;
    }
}
