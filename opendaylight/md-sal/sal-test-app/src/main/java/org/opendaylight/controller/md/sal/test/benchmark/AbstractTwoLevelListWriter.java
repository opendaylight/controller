/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;


import org.opendaylight.controller.md.sal.common.api.data.AsyncWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.TwoLevelListProperties;
import org.opendaylight.yangtools.concepts.Path;

abstract class AbstractTwoLevelListWriter<P extends Path<P>,D,I> extends ItemWriter<P, D> {

    private final long outerItems;
    private final long innerItems;
    private final I reusableInner;
    private long currentOuter = 0;
    private final TransactionWriter<P,D,AsyncWriteTransaction<P, D>> txWriter;


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public AbstractTwoLevelListWriter(TwoLevelListProperties properties, TransactionWriter<P, D,? extends AsyncWriteTransaction<P, D>> writer) {
        outerItems = properties.getOuterListItems();
        innerItems = properties.getInnerListItems();
        reusableInner = createInnerList(innerItems); //properties.getReuseInnerItems();
        txWriter = (TransactionWriter) writer;
    }

    private I getInnerList() {
        if(reusableInner == null) {
            return createInnerList(innerItems);
        }
        return reusableInner;
    }

    abstract I createInnerList(long innerItems);

    @Override
    boolean writeNext(AsyncWriteTransaction<P, D> tx) {
        long id = currentOuter++;

        P path = createOuterListItemPath(id);
        long startTime = System.nanoTime();
        D data = createOuterListItem(id,getInnerList());
        getConstructionStats().addDuration(System.nanoTime() - startTime);
        startTime = System.nanoTime();
        txWriter.write(tx, path, data);
        getWriteStats().addDuration(System.nanoTime() - startTime);
        return currentOuter < outerItems;
    }

    abstract D createOuterListItem(long id, I innerList);

    abstract P createOuterListItemPath(long id);

}
