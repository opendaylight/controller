/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.config.facade.xml.mapping.IdentityMapping;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;

public class SimpleIdentityRefAttributeReadingStrategyTest {

    @Test
    public void testReadIdRef() throws Exception {
        final Map<String, Map<Optional<Revision>, IdentityMapping>> identityMapping = Maps.newHashMap();
        final IdentityMapping value = new IdentityMapping();
        final Optional<Revision> rev = Optional.of(Revision.of("2017-10-10"));
        identityMapping.put("namespace", Collections.singletonMap(rev, value));
        identityMapping.put("inner", Collections.singletonMap(rev, value));
        final SimpleIdentityRefAttributeReadingStrategy key = new SimpleIdentityRefAttributeReadingStrategy(null, "key",
                identityMapping);

        String read = key.readElementContent(XmlElement.fromString("<el xmlns=\"namespace\">local</el>"));
        assertEquals(QName.create(URI.create("namespace"), rev.toJavaUtil(), "local").toString(), read);

        read = key.readElementContent(XmlElement.fromString("<el xmlns:a=\"inner\" xmlns=\"namespace\">a:local</el>"));
        assertEquals(QName.create(URI.create("inner"), rev.toJavaUtil(), "local").toString(), read);

        read = key.readElementContent(
            XmlElement.fromString("<top xmlns=\"namespace\"><el>local</el></top>").getOnlyChildElement());
        assertEquals(QName.create(URI.create("namespace"), rev.toJavaUtil(), "local").toString(), read);
    }
}
