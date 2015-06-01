/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Stopwatch;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.FinanceModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScaleTest {

    private static Logger LOG = LoggerFactory.getLogger(ScaleTest.class);

    @Test(timeout = 500000)
    @Ignore
    public void test(){
        TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create();
        dataTree.setSchemaContext(SchemaContextHelper.select(SchemaContextHelper.FINANCE_YANG));

        List<String> institutions = Arrays.asList("wellsfargo", "bofa", "chase", "citi", "ubc", "xyz", "fora", "tbd", "test1", "test2", "test3");
        List<String> subAccounts = Arrays.asList("savings", "checking", "creditcard1", "creditcard2","creditcard3", "creditcard4", "creditcard5", "cc6", "cc7", "cc8", "cc9", "cc10");
        Random inGen = new Random();
        Random acGen = new Random();

        for(long i=0;i<2000000;i++) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            DataTreeCandidateTip dataTreeCandidateTip = addTransaction(dataTree,
                    institutions.get(inGen.nextInt(institutions.size())),
                    subAccounts.get(acGen.nextInt(subAccounts.size())),
                    i);
            dataTree.commit(dataTreeCandidateTip);
            stopwatch.stop();
            long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            if( elapsed > 1000) {
                LOG.debug("Transaction {} took {} millis", i, elapsed);
            }
        }

    }

    private DataTreeCandidateTip addTransaction(TipProducingDataTree dataTree, String institutionName, String subAccounts, long txnNum) {
        DataTreeSnapshot snapshot = dataTree.takeSnapshot();

        DataTreeModification modification = snapshot.newModification();

        modification.merge(FinanceModel.BASE_PATH, FinanceModel.createCategorizedTransaction(institutionName, subAccounts, txnNum, "test", "test", 1000, "cat1", (short) 100));

        return dataTree.prepare(modification);
    }
}
