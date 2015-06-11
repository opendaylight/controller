package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Maps;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfig;
import org.opendaylight.controller.netconf.util.xml.XmlElement;

public class SimpleIdentityRefAttributeReadingStrategyTest {

    @Test
    public void testReadIdRef() throws Exception {
        final Map<String, Map<Date, EditConfig.IdentityMapping>> identityMapping = Maps.newHashMap();
        final EditConfig.IdentityMapping value = new EditConfig.IdentityMapping();
        final Date rev = new Date();
        identityMapping.put("namespace", Collections.singletonMap(rev, value));
        identityMapping.put("inner", Collections.singletonMap(rev, value));
        final SimpleIdentityRefAttributeReadingStrategy key = new SimpleIdentityRefAttributeReadingStrategy(null, "key", identityMapping);

        String read = key.readElementContent(XmlElement.fromString("<el xmlns=\"namespace\">local</el>"));
        assertEquals(org.opendaylight.yangtools.yang.common.QName.create(URI.create("namespace"), rev, "local").toString(), read);

        read = key.readElementContent(XmlElement.fromString("<el xmlns:a=\"inner\" xmlns=\"namespace\">a:local</el>"));
        assertEquals(org.opendaylight.yangtools.yang.common.QName.create(URI.create("inner"), rev, "local").toString(), read);

        read = key.readElementContent(XmlElement.fromString("<top xmlns=\"namespace\"><el>local</el></top>").getOnlyChildElement());
        assertEquals(org.opendaylight.yangtools.yang.common.QName.create(URI.create("namespace"), rev, "local").toString(), read);
    }
}