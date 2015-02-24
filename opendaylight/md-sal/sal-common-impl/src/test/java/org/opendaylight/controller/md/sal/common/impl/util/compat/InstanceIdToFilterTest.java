/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.md.sal.common.impl.util.compat;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class InstanceIdToFilterTest {

    private static final String NS = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:normalization:test";
    private static final String REVISION = "2014-03-13";
    private static final QName ID = QName.create(NS, REVISION, "id");
    private SchemaContext ctx;

    private final YangInstanceIdentifier.NodeIdentifier rootContainer = new YangInstanceIdentifier.NodeIdentifier(QName.create(NS, REVISION, "test"));
    private final YangInstanceIdentifier.NodeIdentifier outerContainer = new YangInstanceIdentifier.NodeIdentifier(QName.create(NS, REVISION, "outer-container"));
    private final YangInstanceIdentifier.NodeIdentifier augmentedLeaf = new YangInstanceIdentifier.NodeIdentifier(QName.create(NS, REVISION, "augmented-leaf"));
    private final YangInstanceIdentifier.AugmentationIdentifier augmentation = new YangInstanceIdentifier.AugmentationIdentifier(Collections.singleton(augmentedLeaf.getNodeType()));

    private final YangInstanceIdentifier.NodeIdentifier outerList = new YangInstanceIdentifier.NodeIdentifier(QName.create(NS, REVISION, "outer-list"));
    private final YangInstanceIdentifier.NodeIdentifierWithPredicates outerListWithKey = new YangInstanceIdentifier.NodeIdentifierWithPredicates(QName.create(NS, REVISION, "outer-list"), ID, 1);
    private final YangInstanceIdentifier.NodeIdentifier choice = new YangInstanceIdentifier.NodeIdentifier(QName.create(NS, REVISION, "outer-choice"));
    private final YangInstanceIdentifier.NodeIdentifier leafFromCase = new YangInstanceIdentifier.NodeIdentifier(QName.create(NS, REVISION, "one"));

    private final YangInstanceIdentifier.NodeIdentifier leafList = new YangInstanceIdentifier.NodeIdentifier(QName.create(NS, REVISION, "ordered-leaf-list"));
    private final YangInstanceIdentifier.NodeWithValue leafListWithValue = new YangInstanceIdentifier.NodeWithValue(leafList.getNodeType(), "abcd");

    static SchemaContext createTestContext() throws IOException, YangSyntaxErrorException {
        final YangParserImpl parser = new YangParserImpl();
        return parser.parseSources(Collections2.transform(Collections.singletonList("/normalization-test.yang"), new Function<String, ByteSource>() {
            @Override
            public ByteSource apply(final String input) {
                return new ByteSource() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return InstanceIdToFilterTest.class.getResourceAsStream(input);
                    }
                };
            }
        }));
    }

    @Before
    public void setUp() throws Exception {
        ctx = createTestContext();

    }

    @Test
    public void testInAugment() throws Exception {
        final ContainerNode expectedFilter = Builders.containerBuilder().withNodeIdentifier(rootContainer).withChild(
                Builders.containerBuilder().withNodeIdentifier(outerContainer).withChild(
                        Builders.augmentationBuilder().withNodeIdentifier(augmentation).withChild(
                            Builders.leafBuilder().withNodeIdentifier(augmentedLeaf).build()
                        ).build()
                ).build()
        ).build();

        final NormalizedNode<?, ?> filter = InstanceIdToFilter.serialize(ctx, YangInstanceIdentifier.create(rootContainer, outerContainer, augmentation, augmentedLeaf));
        assertEquals(expectedFilter, filter);
    }

    @Test
    public void testListChoice() throws Exception {
        final ContainerNode expectedFilter = Builders.containerBuilder().withNodeIdentifier(rootContainer).withChild(
                Builders.mapBuilder().withNodeIdentifier(outerList).withChild(
                        Builders.mapEntryBuilder().withNodeIdentifier(outerListWithKey).withChild(
                                Builders.leafBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(ID)).withValue(1).build()
                        ).withChild(
                                Builders.choiceBuilder().withNodeIdentifier(choice).withChild(
                                        Builders.leafBuilder().withNodeIdentifier(leafFromCase).build()
                                ).build()
                        ).build()
                ).build()
        ).build();

        final NormalizedNode<?, ?> filter = InstanceIdToFilter.serialize(ctx, YangInstanceIdentifier.create(rootContainer, outerList, outerListWithKey, choice, leafFromCase));
        assertEquals(expectedFilter, filter);
    }

    @Test
    public void testLeafList() throws Exception {
        final ContainerNode expectedFilter = Builders.containerBuilder().withNodeIdentifier(rootContainer).withChild(
                Builders.orderedLeafSetBuilder().withNodeIdentifier(leafList).withChild(
                        Builders.leafSetEntryBuilder().withNodeIdentifier(leafListWithValue).withValue(leafListWithValue.getValue()).build()
                ).build()
        ).build();

        final NormalizedNode<?, ?> filter = InstanceIdToFilter.serialize(ctx, YangInstanceIdentifier.create(rootContainer, leafList, leafListWithValue));
        assertEquals(expectedFilter, filter);
    }
}