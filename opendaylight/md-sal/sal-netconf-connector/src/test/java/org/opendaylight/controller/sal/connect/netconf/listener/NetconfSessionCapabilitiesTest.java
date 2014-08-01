package org.opendaylight.controller.sal.connect.netconf.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yangtools.yang.common.QName;

public class NetconfSessionCapabilitiesTest {

    @Test
    public void testMerge() throws Exception {
        final List<String> caps1 = Lists.newArrayList(
                "namespace:1?module=module1&revision=2012-12-12",
                "namespace:2?module=module2&amp;revision=2012-12-12",
                "urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring?module=ietf-netconf-monitoring&amp;revision=2010-10-04",
                "urn:ietf:params:netconf:base:1.0",
                "urn:ietf:params:netconf:capability:rollback-on-error:1.0"
        );
        final NetconfSessionCapabilities sessionCaps1 = NetconfSessionCapabilities.fromStrings(caps1);
        assertCaps(sessionCaps1, 2, 3);

        final List<String> caps2 = Lists.newArrayList(
                "namespace:3?module=module3&revision=2012-12-12",
                "namespace:4?module=module4&revision=2012-12-12",
                "randomNonModuleCap"
        );
        final NetconfSessionCapabilities sessionCaps2 = NetconfSessionCapabilities.fromStrings(caps2);
        assertCaps(sessionCaps2, 1, 2);

        final NetconfSessionCapabilities merged = sessionCaps1.replaceModuleCaps(sessionCaps2);
        assertCaps(merged, 2, 2 + 1 /*Preserved monitoring*/);
        for (final QName qName : sessionCaps2.getModuleBasedCaps()) {
            assertThat(merged.getModuleBasedCaps(), JUnitMatchers.hasItem(qName));
        }
        assertThat(merged.getModuleBasedCaps(), JUnitMatchers.hasItem(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING));

        assertThat(merged.getNonModuleCaps(), JUnitMatchers.hasItem("urn:ietf:params:netconf:base:1.0"));
        assertThat(merged.getNonModuleCaps(), JUnitMatchers.hasItem("urn:ietf:params:netconf:capability:rollback-on-error:1.0"));
    }

    private void assertCaps(final NetconfSessionCapabilities sessionCaps1, final int nonModuleCaps, final int moduleCaps) {
        assertEquals(nonModuleCaps, sessionCaps1.getNonModuleCaps().size());
        assertEquals(moduleCaps, sessionCaps1.getModuleBasedCaps().size());
    }
}
