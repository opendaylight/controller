package org.opendaylight.controller.sal.connect.netconf.listener;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yangtools.yang.common.QName;

public class NetconfSessionPreferencesTest {

    @Test
    public void testMerge() throws Exception {
        final List<String> caps1 = Lists.newArrayList(
                "namespace:1?module=module1&revision=2012-12-12",
                "namespace:2?module=module2&amp;revision=2012-12-12",
                "urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring?module=ietf-netconf-monitoring&amp;revision=2010-10-04",
                "urn:ietf:params:netconf:base:1.0",
                "urn:ietf:params:netconf:capability:rollback-on-error:1.0"
        );
        final NetconfSessionPreferences sessionCaps1 = NetconfSessionPreferences.fromStrings(caps1);
        assertCaps(sessionCaps1, 2, 3);

        final List<String> caps2 = Lists.newArrayList(
                "namespace:3?module=module3&revision=2012-12-12",
                "namespace:4?module=module4&revision=2012-12-12",
                "randomNonModuleCap"
        );
        final NetconfSessionPreferences sessionCaps2 = NetconfSessionPreferences.fromStrings(caps2);
        assertCaps(sessionCaps2, 1, 2);

        final NetconfSessionPreferences merged = sessionCaps1.replaceModuleCaps(sessionCaps2);
        assertCaps(merged, 2, 2 + 1 /*Preserved monitoring*/);
        for (final QName qName : sessionCaps2.getModuleBasedCaps()) {
            assertThat(merged.getModuleBasedCaps(), hasItem(qName));
        }
        assertThat(merged.getModuleBasedCaps(), hasItem(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING));

        assertThat(merged.getNonModuleCaps(), hasItem("urn:ietf:params:netconf:base:1.0"));
        assertThat(merged.getNonModuleCaps(), hasItem("urn:ietf:params:netconf:capability:rollback-on-error:1.0"));
    }

    @Test
    public void testCapabilityNoRevision() throws Exception {
        final List<String> caps1 = Lists.newArrayList(
                "namespace:2?module=module2",
                "namespace:2?module=module2&amp;revision=2012-12-12",
                "namespace:2?module=module1&amp;RANDOMSTRING;revision=2013-12-12",
                "namespace:2?module=module2&amp;RANDOMSTRING;revision=2013-12-12" // This one should be ignored(same as first), since revision is in wrong format
        );

        final NetconfSessionPreferences sessionCaps1 = NetconfSessionPreferences.fromStrings(caps1);
        assertCaps(sessionCaps1, 0, 3);
    }

    private void assertCaps(final NetconfSessionPreferences sessionCaps1, final int nonModuleCaps, final int moduleCaps) {
        assertEquals(nonModuleCaps, sessionCaps1.getNonModuleCaps().size());
        assertEquals(moduleCaps, sessionCaps1.getModuleBasedCaps().size());
    }
}
