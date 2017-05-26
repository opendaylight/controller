/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.rpcservice.rev140701.RockTheHouseInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.rpcservice.rev140701.RockTheHouseInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.EncapsulatedRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.EncapsulatedRouteInGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.RoutedSimpleRouteInputBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class ContextExtractorTest {

    public interface Transitive extends EncapsulatedRouteInGrouping {

    }

    private static final InstanceIdentifier<?> TEST_ROUTE = InstanceIdentifier.create(Top.class);
    private static final Transitive TEST_GROUPING = new Transitive() {

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return Transitive.class;
        }

        @Override
        public EncapsulatedRoute getRoute() {
            return new EncapsulatedRoute(TEST_ROUTE);
        }
    };

    @Test
    public void testNonRoutedExtraction() {
        final ContextReferenceExtractor extractor = ContextReferenceExtractor.from(RockTheHouseInput.class);
        final RockTheHouseInput input = new RockTheHouseInputBuilder().build();
        final InstanceIdentifier<?> extractedValue = extractor.extract(input);
        assertNull(extractedValue);
    }

    @Test
    public void testRoutedSimpleExtraction() {
        final ContextReferenceExtractor extractor = ContextReferenceExtractor.from(RoutedSimpleRouteInput.class);
        final RoutedSimpleRouteInput input = new RoutedSimpleRouteInputBuilder().setRoute(TEST_ROUTE).build();
        final InstanceIdentifier<?> extractedValue = extractor.extract(input);
        assertSame(TEST_ROUTE,extractedValue);
    }

    @Test
    public void testRoutedEncapsulatedExtraction() {
        final ContextReferenceExtractor extractor = ContextReferenceExtractor.from(EncapsulatedRouteInGrouping.class);
        final InstanceIdentifier<?> extractedValue = extractor.extract(TEST_GROUPING);
        assertSame(TEST_ROUTE,extractedValue);

    }

    @Test
    public void testRoutedEncapsulatedTransitiveExtraction() {
        final ContextReferenceExtractor extractor = ContextReferenceExtractor.from(Transitive.class);
        final InstanceIdentifier<?> extractedValue = extractor.extract(TEST_GROUPING);
        assertSame(TEST_ROUTE,extractedValue);
    }
 }
