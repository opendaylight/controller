package org.opendaylight.controller.sal.connect.netconf;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceRpc;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds QNames for all yang modules reported by ietf-netconf-monitoring/state/schemas
 */
public final class NetconfStateSchemas {

    private static final Logger logger = LoggerFactory.getLogger(NetconfStateSchemas.class);

    /**
     * Factory for NetconfStateSchemas
     */
    public interface NetconfStateSchemasResolver {
        NetconfStateSchemas resolve(final NetconfDeviceRpc deviceRpc, final NetconfSessionPreferences remoteSessionCapabilities, final RemoteDeviceId id);
    }

    /**
     * Default implementation resolving schemas QNames from netconf-state
     */
    public static final class NetconfStateSchemasResolverImpl implements NetconfStateSchemasResolver {

        @Override
        public NetconfStateSchemas resolve(final NetconfDeviceRpc deviceRpc, final NetconfSessionPreferences remoteSessionCapabilities, final RemoteDeviceId id) {
            return NetconfStateSchemas.create(deviceRpc, remoteSessionCapabilities, id);
        }
    }

    public static final NetconfStateSchemas EMPTY = new NetconfStateSchemas(Collections.<RemoteYangSchema>emptySet());

    private static final YangInstanceIdentifier STATE_SCHEMAS_IDENTIFIER =
            YangInstanceIdentifier.builder().node(NetconfState.QNAME).node(Schemas.QNAME).build();
    private static final YangInstanceIdentifier DATA_STATE_SCHEMAS_IDENTIFIER =
            YangInstanceIdentifier.builder().node(NetconfMessageTransformUtil.NETCONF_DATA_QNAME)
                    .node(NetconfState.QNAME).node(Schemas.QNAME).build();

    private static final CompositeNode GET_SCHEMAS_RPC;
    static {
        final Node<?> filter = NetconfMessageTransformUtil.toFilterStructure(STATE_SCHEMAS_IDENTIFIER);
        GET_SCHEMAS_RPC
                = NodeFactory.createImmutableCompositeNode(NetconfMessageTransformUtil.NETCONF_GET_QNAME, null, Lists.<Node<?>>newArrayList(filter));
    }

    private final Set<RemoteYangSchema> availableYangSchemas;

    public NetconfStateSchemas(final Set<RemoteYangSchema> availableYangSchemas) {
        this.availableYangSchemas = availableYangSchemas;
    }

    public Set<RemoteYangSchema> getAvailableYangSchemas() {
        return availableYangSchemas;
    }

    public Set<QName> getAvailableYangSchemasQNames() {
        return Sets.newHashSet(Collections2.transform(getAvailableYangSchemas(), new Function<RemoteYangSchema, QName>() {
            @Override
            public QName apply(final RemoteYangSchema input) {
                return input.getQName();
            }
        }));
    }

    /**
     * Issue get request to remote device and parse response to find all schemas under netconf-state/schemas
     */
    private static NetconfStateSchemas create(final NetconfDeviceRpc deviceRpc, final NetconfSessionPreferences remoteSessionCapabilities, final RemoteDeviceId id) {
        if(remoteSessionCapabilities.isMonitoringSupported() == false) {
            logger.warn("{}: Netconf monitoring not supported on device, cannot detect provided schemas");
            return EMPTY;
        }

        final RpcResult<CompositeNode> schemasNodeResult;
        try {
            schemasNodeResult = deviceRpc.invokeRpc(NetconfMessageTransformUtil.NETCONF_GET_QNAME, GET_SCHEMAS_RPC).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(id + ": Interrupted while waiting for response to " + STATE_SCHEMAS_IDENTIFIER, e);
        } catch (final ExecutionException e) {
            logger.warn("{}: Unable to detect available schemas, get to {} failed", id, STATE_SCHEMAS_IDENTIFIER, e);
            return EMPTY;
        }

        if(schemasNodeResult.isSuccessful() == false) {
            logger.warn("{}: Unable to detect available schemas, get to {} failed, {}", id, STATE_SCHEMAS_IDENTIFIER, schemasNodeResult.getErrors());
            return EMPTY;
        }

        final CompositeNode schemasNode =
                (CompositeNode) NetconfMessageTransformUtil.findNode(schemasNodeResult.getResult(), DATA_STATE_SCHEMAS_IDENTIFIER);
        if(schemasNode == null) {
            logger.warn("{}: Unable to detect available schemas, get to {} was empty", id, STATE_SCHEMAS_IDENTIFIER);
            return EMPTY;
        }

        return create(id, schemasNode);
    }

    /**
     * Parse response of get(netconf-state/schemas) to find all schemas under netconf-state/schemas
     */
    @VisibleForTesting
    protected static NetconfStateSchemas create(final RemoteDeviceId id, final CompositeNode schemasNode) {
        final Set<RemoteYangSchema> availableYangSchemas = Sets.newHashSet();

        for (final CompositeNode schemaNode : schemasNode.getCompositesByName(Schema.QNAME.withoutRevision())) {
            final Optional<RemoteYangSchema> fromCompositeNode = RemoteYangSchema.createFromCompositeNode(id, schemaNode);
            if(fromCompositeNode.isPresent()) {
                availableYangSchemas.add(fromCompositeNode.get());
            }
        }

        return new NetconfStateSchemas(availableYangSchemas);
    }

    public final static class RemoteYangSchema {
        private final QName qname;

        private RemoteYangSchema(final QName qname) {
            this.qname = qname;
        }

        public QName getQName() {
            return qname;
        }

        static Optional<RemoteYangSchema> createFromCompositeNode(final RemoteDeviceId id, final CompositeNode schemaNode) {
            Preconditions.checkArgument(schemaNode.getKey().equals(Schema.QNAME.withoutRevision()), "Wrong QName %s", schemaNode.getKey());

            QName childNode = NetconfMessageTransformUtil.IETF_NETCONF_MONITORING_SCHEMA_FORMAT.withoutRevision();

            String formatAsString = getSingleChildNodeValue(schemaNode, childNode).get();
            //This is HotFix for situations where format statement in netconf-monitoring might be passed with prefix.
            if (formatAsString.contains(":")) {
                String[] prefixedString = formatAsString.split(":");
                //FIXME: might be good idea to check prefix against model namespace
                formatAsString = prefixedString[1];
            }
            if(formatAsString.equals(Yang.QNAME.getLocalName()) == false) {
                logger.debug("{}: Ignoring schema due to unsupported format: {}", id, formatAsString);
                return Optional.absent();
            }

            childNode = NetconfMessageTransformUtil.IETF_NETCONF_MONITORING_SCHEMA_LOCATION.withoutRevision();
            final Set<String> locationsAsString = getAllChildNodeValues(schemaNode, childNode);
            if(locationsAsString.contains(Schema.Location.Enumeration.NETCONF.toString()) == false) {
                logger.debug("{}: Ignoring schema due to unsupported location: {}", id, locationsAsString);
                return Optional.absent();
            }

            childNode = NetconfMessageTransformUtil.IETF_NETCONF_MONITORING_SCHEMA_NAMESPACE.withoutRevision();
            final String namespaceAsString = getSingleChildNodeValue(schemaNode, childNode).get();

            childNode = NetconfMessageTransformUtil.IETF_NETCONF_MONITORING_SCHEMA_VERSION.withoutRevision();
            // Revision does not have to be filled
            final Optional<String> revisionAsString = getSingleChildNodeValue(schemaNode, childNode);

            childNode = NetconfMessageTransformUtil.IETF_NETCONF_MONITORING_SCHEMA_IDENTIFIER.withoutRevision();
            final String moduleNameAsString = getSingleChildNodeValue(schemaNode, childNode).get();

            final QName moduleQName = revisionAsString.isPresent()
                    ? QName.create(namespaceAsString, revisionAsString.get(), moduleNameAsString)
                    : QName.create(URI.create(namespaceAsString), null, moduleNameAsString).withoutRevision();

            return Optional.of(new RemoteYangSchema(moduleQName));
        }

        private static Set<String> getAllChildNodeValues(final CompositeNode schemaNode, final QName childNodeQName) {
            final Set<String> extractedValues = Sets.newHashSet();
            for (final SimpleNode<?> childNode : schemaNode.getSimpleNodesByName(childNodeQName)) {
                extractedValues.add(getValueOfSimpleNode(childNodeQName, childNode).get());
            }
            return extractedValues;
        }

        private static Optional<String> getSingleChildNodeValue(final CompositeNode schemaNode, final QName childNode) {
            final SimpleNode<?> node = schemaNode.getFirstSimpleByName(childNode);
            return getValueOfSimpleNode(childNode, node);
        }

        private static Optional<String> getValueOfSimpleNode(final QName childNode, final SimpleNode<?> node) {
            Preconditions.checkNotNull(node, "Child node %s not present", childNode);
            final Object value = node.getValue();
            return value == null ? Optional.<String>absent() : Optional.of(value.toString().trim());
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final RemoteYangSchema that = (RemoteYangSchema) o;

            if (!qname.equals(that.qname)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return qname.hashCode();
        }
    }
}
