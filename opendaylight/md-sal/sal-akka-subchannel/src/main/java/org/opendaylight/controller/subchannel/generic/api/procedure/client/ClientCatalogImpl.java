/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.procedure.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class ClientCatalogImpl<K extends AbstractClient<T>,T> implements ClientCatalog<K,T> {
    private Map<ClientIdentify<T>,K> clients = new HashMap<>();

    @Override
    public K addClient(K client)
    {
        clients.put(client.getClientIdentify(), client);
        return client;
    }

    @Override
    public K removeClient(ClientIdentify<T> id){
        return clients.remove(id);
    }

    @Override
    public K getClient(ClientIdentify<T> id){
        return clients.get(id);
    }

    @Override
    public K getClient(T entity) {
        return clients.get(new ClientIdentify<T>(entity));
    }

    @Override
    public K removeClient(T entity) {
        return clients.remove(new ClientIdentify<T>(entity));
    }


}
