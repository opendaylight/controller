package org.opendaylight.controller.sal.connect.netconf.listener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public final class NetconfSessionCapabilities {

    private static final Logger logger = LoggerFactory.getLogger(NetconfSessionCapabilities.class);

    private final Set<String> capabilities;

    private final Set<QName> moduleBasedCaps;

    private NetconfSessionCapabilities(final Set<String> capabilities, final Set<QName> moduleBasedCaps) {
        this.capabilities = capabilities;
        this.moduleBasedCaps = moduleBasedCaps;
    }

    public Set<QName> getModuleBasedCaps() {
        return moduleBasedCaps;
    }

    public boolean containsCapability(final String capability) {
        return capabilities.contains(capability);
    }

    public boolean containsCapability(final QName capability) {
        return moduleBasedCaps.contains(capability);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("capabilities", capabilities)
                .add("rollback", isRollbackSupported())
                .add("monitoring", isMonitoringSupported())
                .toString();
    }

    public boolean isRollbackSupported() {
        return containsCapability(NetconfMessageTransformUtil.NETCONF_ROLLBACK_ON_ERROR_URI.toString());
    }

    public boolean isMonitoringSupported() {
        return containsCapability(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING)
                || containsCapability(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING.getNamespace().toString());
    }

    public static NetconfSessionCapabilities fromNetconfSession(final NetconfClientSession session) {
        return fromStrings(session.getServerCapabilities());
    }

    public static NetconfSessionCapabilities fromStrings(final Collection<String> capabilities) {
        final Set<QName> moduleBasedCaps = Sets.newHashSet();

        for (final String capability : capabilities) {
            if(isModuleBasedCapability(capability)) {
                final String[] parts = capability.split("\\?");
                final String namespace = parts[0];
                final FluentIterable<String> queryParams = FluentIterable.from(Arrays.asList(parts[1].split("&")));

                String revision = getStringAndTransform(queryParams, "revision=", "revision=");

                final String moduleName = getStringAndTransform(queryParams, "module=", "module=");

                if (revision == null) {
                    logger.debug("Netconf device was not reporting revision correctly, trying to get amp;revision=");
                    revision = getStringAndTransform(queryParams, "amp;revision=", "amp;revision=");

                    if (revision == null) {
                        logger.warn("Netconf device returned revision incorrectly escaped for {}", capability);
                    }
                }
                moduleBasedCaps.add(QName.create(namespace, revision, moduleName));
            }
        }

        return new NetconfSessionCapabilities(Sets.newHashSet(capabilities), moduleBasedCaps);
    }

    private static boolean isModuleBasedCapability(final String capability) {
        return capability.contains("?") && capability.contains("module=") && capability.contains("revision=");
    }

    private static String getStringAndTransform(final Iterable<String> queryParams, final String match,
                                                final String substringToRemove) {
        final Optional<String> found = Iterables.tryFind(queryParams, new Predicate<String>() {
            @Override
            public boolean apply(final String input) {
                return input.startsWith(match);
            }
        });

        return found.isPresent() ? found.get().replaceAll(substringToRemove, "") : null;
    }

}
