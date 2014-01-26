/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.bugfix;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;








import static org.junit.Assert.*;

public class UnionSerializationTest extends AbstractDataServiceTest {
    
    public static final String PREFIX_STRING = "192.168.0.1/32";
    
    
    @Test
    public void testPrefixSerialization() throws Exception {
        
        Ipv4Prefix ipv4prefix = new Ipv4Prefix(PREFIX_STRING);
        IpPrefix ipPrefix = new IpPrefix(ipv4prefix);
        Prefix prefix = new PrefixBuilder().setPrefix(ipPrefix).build();
        
        CompositeNode serialized = testContext.getBindingToDomMappingService().toDataDom(prefix);
        assertNotNull(serialized);
        assertNotNull(serialized.getFirstSimpleByName(Prefix.QNAME));
        assertEquals(PREFIX_STRING, serialized.getFirstSimpleByName(Prefix.QNAME).getValue());
        
        Prefix deserialized = (Prefix) testContext.getBindingToDomMappingService().dataObjectFromDataDom(InstanceIdentifier.builder().node(Prefix.class).build(), serialized);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getPrefix());
        assertNotNull(deserialized.getPrefix().getIpv4Prefix());
        assertEquals(PREFIX_STRING, deserialized.getPrefix().getIpv4Prefix().getValue());
    }

}
