/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkArgument;

import akka.actor.ActorSystem;
import com.google.common.annotations.Beta;
import com.google.common.collect.BiMap;
import java.util.concurrent.Executor;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.mdsal.dom.api.xpath.DOMXPathCallback;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.XPathAwareDOMStore;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Extends {@link DistributedDataStore} with the ability to evaluate XPath expressions, as specified by
 * {@link XPathAwareDOMStore}.
 *
 * @author Robert Varga
 */
@Beta
public class XPathAwareDistributedDataStore extends DistributedDataStore implements XPathAwareDOMStore {
    public XPathAwareDistributedDataStore(final ActorSystem actorSystem, final ClusterWrapper cluster,
            final Configuration configuration, final DatastoreContextFactory datastoreContextFactory,
            final DatastoreSnapshot restoreFromSnapshot) {
        super(actorSystem, cluster, configuration, datastoreContextFactory, restoreFromSnapshot);
    }

    @Override
    public void evaluate(final DOMStoreReadTransaction transaction, final YangInstanceIdentifier path,
            final String xpath, final BiMap<String, QNameModule> prefixMapping, final DOMXPathCallback callback,
            final Executor callbackExecutor) {
        checkArgument(transaction instanceof TransactionProxy, "Transaction %s does not come from this implementation",
            transaction);
        ((TransactionProxy) transaction).evaluate(path, xpath, prefixMapping, callback, callbackExecutor);
    }
}
