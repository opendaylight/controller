/*
 * Copyright (c) 2017 ZTE, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.subchannel.generic.api.procedure.client;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public interface ClientCatalog<K extends AbstractClient<T>,T> {
    K addClient(K client);
    K removeClient(ClientIdentify<T> id);
    K getClient(ClientIdentify<T> id);
    K getClient(T entity);
    K removeClient(T entity);
}
