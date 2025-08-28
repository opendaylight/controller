/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dsbenchmark;

import java.util.Random;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatastoreAbstractWriter {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreAbstractWriter.class);

    protected final int outerListElem;
    protected final int innerListElem;
    protected final long writesPerTx;
    protected final StartTestInput.Operation oper;
    protected final StartTestInput.DataStore dataStore;
    protected final Random rn = new Random();

    protected int txOk = 0;
    protected int txError = 0;

    public DatastoreAbstractWriter(final StartTestInput.Operation oper,
            final int outerListElem, final int innerListElem, final long writesPerTx, final DataStore dataStore) {
        this.outerListElem = outerListElem;
        this.innerListElem = innerListElem;
        this.writesPerTx = writesPerTx;
        this.oper = oper;
        this.dataStore = dataStore;
        LOG.debug("DatastoreAbstractWriter created: {}", this);
    }

    public abstract void createList();

    public abstract void executeList();

    public int getTxError() {
        return txError;
    }

    public int getTxOk() {
        return txOk;
    }

    protected LogicalDatastoreType getDataStoreType() {
        return dataStore == DataStore.CONFIG || dataStore != DataStore.OPERATIONAL && !rn.nextBoolean()
            ? LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL;
    }
}
