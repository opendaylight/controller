package org.opendaylight.controller.sal.connect.netconf.listener;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfSessionPreferences {

    private static final class ParameterMatcher {
        private final Predicate<String> predicate;
        private final int skipLength;

        ParameterMatcher(final String name) {
            predicate = new Predicate<String>() {
                @Override
                public boolean apply(final String input) {
                    return input.startsWith(name);
                }
            };

            this.skipLength = name.length();
        }

        private String from(final Iterable<String> params) {
            final Optional<String> o = Iterables.tryFind(params, predicate);
            if (!o.isPresent()) {
                return null;
            }

            return o.get().substring(skipLength);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(NetconfSessionPreferences.class);
    private static final ParameterMatcher MODULE_PARAM = new ParameterMatcher("module=");
    private static final ParameterMatcher REVISION_PARAM = new ParameterMatcher("revision=");
    private static final ParameterMatcher BROKEN_REVISON_PARAM = new ParameterMatcher("amp;revision=");
    private static final Splitter AMP_SPLITTER = Splitter.on('&');
    private static final Predicate<String> CONTAINS_REVISION = new Predicate<String>() {
        @Override
        public boolean apply(final String input) {
            return input.contains("revision=");
        }
    };

    private final Set<QName> moduleBasedCaps;
    private final Set<String> nonModuleCaps;

    private NetconfSessionPreferences(final Set<String> nonModuleCaps, final Set<QName> moduleBasedCaps) {
        this.nonModuleCaps = Preconditions.checkNotNull(nonModuleCaps);
        this.moduleBasedCaps = Preconditions.checkNotNull(moduleBasedCaps);
    }

    public Set<QName> getModuleBasedCaps() {
        return moduleBasedCaps;
    }

    public Set<String> getNonModuleCaps() {
        return nonModuleCaps;
    }

    public boolean containsNonModuleCapability(final String capability) {
        return nonModuleCaps.contains(capability);
    }

    public boolean containsModuleCapability(final QName capability) {
        return moduleBasedCaps.contains(capability);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("capabilities", nonModuleCaps)
                .add("moduleBasedCapabilities", moduleBasedCaps)
                .add("rollback", isRollbackSupported())
                .add("monitoring", isMonitoringSupported())
                .add("candidate", isCandidateSupported())
                .add("writableRunning", isRunningWritable())
                .toString();
    }

    public boolean isRollbackSupported() {
        return containsNonModuleCapability(NetconfMessageTransformUtil.NETCONF_ROLLBACK_ON_ERROR_URI.toString());
    }

    public boolean isCandidateSupported() {
        return containsNonModuleCapability(NetconfMessageTransformUtil.NETCONF_CANDIDATE_URI.toString());
    }

    public boolean isRunningWritable() {
        return containsNonModuleCapability(NetconfMessageTransformUtil.NETCONF_RUNNING_WRITABLE_URI.toString());
    }

    public boolean isNotificationsSupported() {
        return containsNonModuleCapability(NetconfMessageTransformUtil.NETCONF_NOTIFICATONS_URI.toString())
                || containsModuleCapability(NetconfMessageTransformUtil.IETF_NETCONF_NOTIFICATIONS);
    }

    public boolean isMonitoringSupported() {
        return containsModuleCapability(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING)
                || containsNonModuleCapability(NetconfMessageTransformUtil.IETF_NETCONF_MONITORING.getNamespace().toString());
    }

    public NetconfSessionPreferences addModuleCaps(final NetconfSessionPreferences netconfSessionModuleCapabilities) {
        final HashSet<QName> mergedCaps = Sets.newHashSetWithExpectedSize(moduleBasedCaps.size() + netconfSessionModuleCapabilities.getModuleBasedCaps().size());
        mergedCaps.addAll(moduleBasedCaps);
        mergedCaps.addAll(netconfSessionModuleCapabilities.getModuleBasedCaps());
        return new NetconfSessionPreferences(getNonModuleCaps(), mergedCaps);
    }

    public static NetconfSessionPreferences fromNetconfSession(final NetconfClientSession session) {
        return fromStrings(session.getServerCapabilities());
    }

    private static QName cachedQName(final String namespace, final String revision, final String moduleName) {
        return QName.cachedReference(QName.create(namespace, revision, moduleName));
    }

    private static QName cachedQName(final String namespace, final String moduleName) {
        return QName.cachedReference(QName.create(URI.create(namespace), null, moduleName).withoutRevision());
    }

    public static NetconfSessionPreferences fromStrings(final Collection<String> capabilities) {
        final Set<QName> moduleBasedCaps = new HashSet<>();
        final Set<String> nonModuleCaps = Sets.newHashSet(capabilities);

        for (final String capability : capabilities) {
            final int qmark = capability.indexOf('?');
            if (qmark == -1) {
                continue;
            }

            final String namespace = capability.substring(0, qmark);
            final Iterable<String> queryParams = AMP_SPLITTER.split(capability.substring(qmark + 1));
            final String moduleName = MODULE_PARAM.from(queryParams);
            if (moduleName == null) {
                continue;
            }

            String revision = REVISION_PARAM.from(queryParams);
            if (revision != null) {
                addModuleQName(moduleBasedCaps, nonModuleCaps, capability, cachedQName(namespace, revision, moduleName));
                continue;
            }

            /*
             * We have seen devices which mis-escape revision, but the revision may not
             * even be there. First check if there is a substring that matches revision.
             */
            if (Iterables.any(queryParams, CONTAINS_REVISION)) {

                LOG.debug("Netconf device was not reporting revision correctly, trying to get amp;revision=");
                revision = BROKEN_REVISON_PARAM.from(queryParams);
                if (revision == null) {
                    LOG.warn("Netconf device returned revision incorrectly escaped for {}, ignoring it", capability);
                    addModuleQName(moduleBasedCaps, nonModuleCaps, capability, cachedQName(namespace, moduleName));
                } else {
                    addModuleQName(moduleBasedCaps, nonModuleCaps, capability, cachedQName(namespace, revision, moduleName));
                }
                continue;
            }

            // Fallback, no revision provided for module
            addModuleQName(moduleBasedCaps, nonModuleCaps, capability, cachedQName(namespace, moduleName));
        }

        return new NetconfSessionPreferences(ImmutableSet.copyOf(nonModuleCaps), ImmutableSet.copyOf(moduleBasedCaps));
    }


    private static void addModuleQName(final Set<QName> moduleBasedCaps, final Set<String> nonModuleCaps, final String capability, final QName qName) {
        moduleBasedCaps.add(qName);
        nonModuleCaps.remove(capability);
    }

    private NetconfDeviceCapabilities capabilities = new NetconfDeviceCapabilities();

    public NetconfDeviceCapabilities getNetconfDeviceCapabilities() {
        return capabilities;
    }


}
