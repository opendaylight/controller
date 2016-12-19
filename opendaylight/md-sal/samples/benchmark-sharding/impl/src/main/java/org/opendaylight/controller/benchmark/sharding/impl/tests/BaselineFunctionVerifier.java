/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl.tests;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.opendaylight.controller.benchmark.sharding.impl.DomListBuilder;
import org.opendaylight.controller.benchmark.sharding.impl.ShardHelper;
import org.opendaylight.controller.benchmark.sharding.impl.ShardHelper.ShardData;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListeningException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeLoopException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies basic multi-sharding functionality by creating two shards and
 * attempting to write into them.
 *
 * @author jmedved
 */
public class BaselineFunctionVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinShardTest.class);

    private BaselineFunctionVerifier() {
        LOG.warn("BaselineFunctionVerifier created");
    }

    /**
     * Helper class that logs the content of the data that was pushed into
     * the data store.
     * @author jmedved
     */
    private static class DumpDataToLogListener implements DOMDataTreeListener {

        @Override
        public void onDataTreeChanged(final Collection<DataTreeCandidate> collection,
                final Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> map) {
            LOG.warn("Received onDataTreeChanged {}, data: {}", collection, map);
        }

        @Override
        public void onDataTreeFailed(final Collection<DOMDataTreeListeningException> collection) {
            LOG.error("Received onDataTreeFailed {}", collection);
        }

    }

    /**
     * Verifies the very basic sharding function: writing data into
     * two shards. Also serves as example code for writing entire
     * objects into the data store at once.
     *
     * @param shardHelper reference to shardHelper
     * @param dataTreeService reference to MD-SAL dataTreeService
     * @throws DOMDataTreeShardingConflictException when shard can't be created
     */
    public static void verifyBaselineFunctionality(final ShardHelper shardHelper,
            final DOMDataTreeService dataTreeService) throws DOMDataTreeShardingConflictException {
        LOG.info("Creating transaction chain tx1 for producer shardData[0]");

        final YangInstanceIdentifier yiId1 = AbstractShardTest.TEST_DATA_ROOT_YID.node(
            new NodeIdentifierWithPredicates(OuterList.QNAME, QName.create(OuterList.QNAME, "oid"), 1));
        final ShardData sd1 = shardHelper.createAndInitShard(LogicalDatastoreType.CONFIGURATION, yiId1);

        final DOMDataTreeCursorAwareTransaction tx1 = sd1.getProducer().createTransaction(false);
        final DOMDataTreeWriteCursor cursor1 = tx1.createCursor(sd1.getDOMDataTreeIdentifier());
        final YangInstanceIdentifier list1Yid =
            sd1.getDOMDataTreeIdentifier().getRootIdentifier().node(InnerList.QNAME);
        cursor1.write(list1Yid.getLastPathArgument(), DomListBuilder.buildInnerList(2, 10));
        cursor1.enter(new NodeIdentifier(InnerList.QNAME));
        cursor1.close();

        LOG.info("Submitting transaction tx1");
        try {
            tx1.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("tx1 failed, {}", e);
        }

        LOG.info("Creating transaction chain tx2 for producer shardData[1]");
        final YangInstanceIdentifier yiId2 =
                AbstractShardTest.TEST_DATA_ROOT_YID.node(new NodeIdentifierWithPredicates(OuterList.QNAME,
                QName.create(OuterList.QNAME, "oid"),
                2));
        ShardData sd2 = shardHelper.createAndInitShard(LogicalDatastoreType.CONFIGURATION, yiId2);
        final DOMDataTreeCursorAwareTransaction tx2 = sd2.getProducer().createTransaction(false);
        final DOMDataTreeWriteCursor cursor2 = tx2.createCursor(sd2.getDOMDataTreeIdentifier());

        LOG.info("Writing entire list2 to Shard2");
        cursor2.write(new NodeIdentifier(InnerList.QNAME), DomListBuilder.buildInnerList(2, 10));
        cursor2.close();

        LOG.info("Submitting transaction tx2");
        try {
            tx2.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("tx2 failed, {}", e);
        }

        try {
            dataTreeService.registerListener(new DumpDataToLogListener(),
                    Lists.newArrayList(sd1.getDOMDataTreeIdentifier(), sd2.getDOMDataTreeIdentifier()),
                    false, Collections.emptyList());
        } catch (final DOMDataTreeLoopException e) {
            throw new RuntimeException(e);
        }
    }
}
