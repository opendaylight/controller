/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;

public abstract class DatastoreAbstractWriter {
    protected final int outerListElem;
    protected final int innerListElem;
    protected final long writesPerTx;
    protected final StartTestInput.Operation oper;

    protected int txOk = 0;
    protected int txError = 0;


    public DatastoreAbstractWriter(StartTestInput.Operation oper,
                                   int outerListElem, int innerListElem, long writesPerTx) {
        this.outerListElem = outerListElem;
        this.innerListElem = innerListElem;
        this.writesPerTx = writesPerTx;
        this.oper = oper;
    }

    public abstract void createList();
    public abstract void executeList();

    public int getTxError() {
        return txError;
    }

    public int getTxOk() {
        return txOk;
    }

}
