/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants.MODULE_TYPE_Q_NAME;
import static org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants.SERVICE_TYPE_Q_NAME;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class SchemaContextTest extends AbstractYangTest {

    IdentitySchemaNode findIdentityByQName(Module module, QName qName) {
        Map<QName, IdentitySchemaNode> mapIdentitiesByQNames = mapIdentitiesByQNames(module);
        IdentitySchemaNode found = mapIdentitiesByQNames.get(qName);
        assertNotNull(found);
        return found;
    }

    @Test
    public void testReadingIdentities_threadsModule() {

        IdentitySchemaNode serviceType = findIdentityByQName(configModule,
                SERVICE_TYPE_Q_NAME);

        Map<String /* identity name */, Optional<QName>> expectedIdentitiesToBases = ImmutableMap
                .of("eventbus", Optional.<QName>absent(), "threadfactory", Optional.<QName>absent(), "threadpool",
                        Optional.<QName>absent(), "scheduled-threadpool", Optional.<QName>absent());

        assertThat(threadsModule.getIdentities().size(),
                is(expectedIdentitiesToBases.size()));
        assertAllIdentitiesAreExpected(threadsModule, expectedIdentitiesToBases);

        IdentitySchemaNode eventBusSchemaNode = null;
        for (IdentitySchemaNode id : threadsModule.getIdentities()) {
            String localName = id.getQName().getLocalName();

            if (localName.equals("eventbus")) {
                eventBusSchemaNode = id;
            }
            // all except scheduled-threadpool should have base set to
            // serviceType
            if (localName.equals("scheduled-threadpool") == false) {
                assertEquals(serviceType, id.getBaseIdentity());
            }
        }
        assertNotNull(eventBusSchemaNode);
        // check unknown schma nodes
        List<UnknownSchemaNode> unknownSchemaNodes = eventBusSchemaNode
                .getUnknownSchemaNodes();
        assertEquals(1, unknownSchemaNodes.size());
        UnknownSchemaNode usn = unknownSchemaNodes.get(0);
        assertEquals("com.google.common.eventbus.EventBus", usn.getQName()
                .getLocalName());
        assertEquals(ConfigConstants.JAVA_CLASS_EXTENSION_QNAME,
                usn.getNodeType());
    }

    private void assertAllIdentitiesAreExpected(
            Module module,
            Map<String /* identity name */, Optional<QName>> expectedIdentitiesToBases) {
        Map<String /* identity name */, Optional<QName>> copyOfExpectedNames = new HashMap<>(
                expectedIdentitiesToBases);
        for (IdentitySchemaNode id : module.getIdentities()) {
            String localName = id.getQName().getLocalName();
            assertTrue("Unexpected identity " + localName,
                    copyOfExpectedNames.containsKey(localName));
            Optional<QName> maybeExpectedBaseQName = copyOfExpectedNames
                    .remove(localName);
            if (maybeExpectedBaseQName.isPresent()) {
                assertEquals("Unexpected base identity of " + localName,
                        maybeExpectedBaseQName.get(), id.getBaseIdentity()
                                .getQName());
            }
        }
        assertEquals("Expected identities not found " + copyOfExpectedNames,
                Collections.EMPTY_MAP, copyOfExpectedNames);
    }

    @Test
    public void testReadingIdentities_threadsJavaModule() {
        Map<String /* identity name */, Optional<QName>> expectedIdentitiesToBases = ImmutableMap
                .of("eventbus", Optional.of(MODULE_TYPE_Q_NAME), "async-eventbus", Optional.of(MODULE_TYPE_Q_NAME),
                        "threadfactory-naming", Optional.of(MODULE_TYPE_Q_NAME), "threadpool-dynamic",
                        Optional.of(MODULE_TYPE_Q_NAME), "thread-rpc-context", Optional.<QName>absent());
        assertAllIdentitiesAreExpected(threadsJavaModule,
                expectedIdentitiesToBases);
    }

}
