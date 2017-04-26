/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging.messages;


/**
 * Created by HanJie on 2017/2/15.
 *
 * @author Han Jie
 */
public class SliceMessageTrackerTimeOut<T> extends AbstractSliceMessageTimeOut<T> {
    private static final long serialVersionUID = 5090255851521575030L;

    public SliceMessageTrackerTimeOut(T clientId, long messageId, int chunkIndex) {
        super(clientId,messageId,chunkIndex);
    }
}
