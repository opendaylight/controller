/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.subchannel.api;

import akka.util.Timeout;
import scala.concurrent.Future;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public interface SubChannel<T> {
    void post(T receiver, Object message, T replyTo);
    Future<Object> request(T receiver, Object message,Timeout timeout);
}
