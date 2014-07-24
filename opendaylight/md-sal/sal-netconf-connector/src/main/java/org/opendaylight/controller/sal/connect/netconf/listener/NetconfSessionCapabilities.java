package org.opendaylight.controller.sal.connect.netconf.listener;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.net.URISyntaxException;
import java.net.URI;

import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfSessionCapabilities {
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

    private static final Logger LOG = LoggerFactory.getLogger(NetconfSessionCapabilities.class);
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
    private final Set<String> capabilities;

    private NetconfSessionCapabilities(final Set<String> capabilities, final Set<QName> moduleBasedCaps) {
        this.capabilities = Preconditions.checkNotNull(capabilities);
        this.moduleBasedCaps = Preconditions.checkNotNull(moduleBasedCaps);
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
        final Set<QName> moduleBasedCaps = new HashSet<>();

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
            /*
             * We have seen devices which mis-escape revision, but the revision may not
             * even be there. First check if there is a substring that matches revision.
             */
            String revision = REVISION_PARAM.from(queryParams);
            if (revision == null) {
                /*if (!Iterables.any(queryParams, CONTAINS_REVISION)) {
                	System.out.println("when there is no revision in queryparams");
                    continue;
                }*/
            	logger.debug("Netconf device was not reporting revision correctly, trying to get amp;revision=");
                revision = BROKEN_REVISON_PARAM.from(queryParams);
                if (revision == null) {
                	logger.warn("Netconf device returned revision incorrectly escaped for {}, ignoring it", capability);
                }
                final URI namespaceUri;
        	try {
        	    namespaceUri = new URI(namespace);
        	}catch (URISyntaxException ue) {
          		throw new IllegalArgumentException(String.format("Namespace '%s' is not a valid URI", namespace), ue);
        	}
               	moduleBasedCaps.add(QName.create(namespaceUri,null,moduleName));
               	continue;
            }
//          FIXME: do we really want to continue here?
//          moduleBasedCaps.add(QName.create(namespace, revision, moduleName));
//          continue;
//          }
            moduleBasedCaps.add(QName.create(namespace, revision, moduleName));
            continue;
        }
        return new NetconfSessionCapabilities(ImmutableSet.copyOf(capabilities), ImmutableSet.copyOf(moduleBasedCaps));
    }
}
