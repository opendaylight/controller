/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import java.io.File;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

final class EOSTestUtils {
    static final SchemaContext SCHEMA_CONTEXT = YangParserTestUtils.parseYangFiles(
        new File("src/main/yang/entity-owners.yang"));

    private EOSTestUtils() {
        // Hidden on purpose
    }
}
