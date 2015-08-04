/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.unknownextension;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntryTest;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;

public class UnknownExtensionTest extends ServiceInterfaceEntryTest {

    @Test
    public void testStopOnUnknownLanguageExtension() throws Exception {
        List<InputStream> yangISs = Lists.newArrayList(getClass()
                .getResourceAsStream("test-ifcWithUnknownExtension.yang"));
        yangISs.addAll(getConfigApiYangInputStreams());
        try {
            CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
            for (InputStream yangIS : yangISs) {
                reactor.addSource(new YangStatementSourceImpl(yangIS));
            }
            context = reactor.buildEffective();

            fail("Should throw IllegalArgumentException due to not found extension type definition");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Not found unknown statement"));
        } finally {
            for (InputStream is : yangISs) {
                is.close();
            }
        }
    }

}
