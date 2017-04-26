/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging.messages;

import java.io.Serializable;

/**
 * Created by HanJie on 2017/4/18.
 *
 * @author Han Jie
 */
public class SerializedMessage implements Serializable {
    private static final long serialVersionUID = -5637205492308855073L;
    private Class<?> clazz;
    private byte[] data;

    public SerializedMessage(Class<?> clazz, byte[] data) {
        this.clazz = clazz;
        this.data = data.clone();
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public byte[] getData() {
        return data.clone();
    }

}

