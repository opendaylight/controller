/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.messaging;

import javax.annotation.Nullable;

/**
 * Created by HanJie on 2017/4/10.
 *
 * @author Han Jie
 */
public interface SliceMessageCallback<V> {

    void onReceive(@Nullable V message);

    //TODO: more callback interfaces for interval keepalive, specific message along with the last chunk message
}
