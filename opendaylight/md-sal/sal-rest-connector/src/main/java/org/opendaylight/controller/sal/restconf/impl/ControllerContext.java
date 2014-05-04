/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Response.Status;

import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.rest.impl.RestUtil;
import org.opendaylight.controller.sal.rest.impl.RestconfProvider;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerContext implements SchemaContextListener {
    private final static Logger LOG = LoggerFactory.getLogger( ControllerContext.class );

    private final static ControllerContext INSTANCE = new ControllerContext();

    private final static String NULL_VALUE = "null";

    private final static String MOUNT_MODULE = "yang-ext";

    private final static String MOUNT_NODE = "mount";

    public final static String MOUNT = "yang-ext:mount";

    private final static String URI_ENCODING_CHAR_SET = "ISO-8859-1";

    private final BiMap<URI, String> uriToModuleName = HashBiMap.<URI, String> create();

    private final Map<String, URI> moduleNameToUri = uriToModuleName.inverse();

    private final Map<QName, RpcDefinition> qnameToRpc = new ConcurrentHashMap<>();

    private volatile SchemaContext globalSchema;
    private volatile MountService mountService;

    public void setGlobalSchema( final SchemaContext globalSchema ) {
        this.globalSchema = globalSchema;
    }

    public void setMountService( final MountService mountService ) {
        this.mountService = mountService;
    }

    private ControllerContext() {
    }

    public static ControllerContext getInstance() {
        return ControllerContext.INSTANCE;
    }

    private void checkPreconditions() {
        if( globalSchema == null ) {
            throw new ResponseException( Status.SERVICE_UNAVAILABLE, RestconfProvider.NOT_INITALIZED_MSG );
        }
    }

    public void setSchemas( final SchemaContext schemas ) {
        this.onGlobalContextUpdated( schemas );
    }

    public InstanceIdWithSchemaNode toInstanceIdentifier( final String restconfInstance ) {
        return this.toIdentifier( restconfInstance, false );
    }

    public InstanceIdWithSchemaNode toMountPointIdentifier( final String restconfInstance ) {
        return this.toIdentifier( restconfInstance, true );
    }

    private InstanceIdWithSchemaNode toIdentifier( final String restconfInstance,
                                                   final boolean toMountPointIdentifier ) {
        this.checkPreconditions();

        Iterable<String> split = Splitter.on( "/" ).split( restconfInstance );
        final ArrayList<String> encodedPathArgs = Lists.<String> newArrayList( split );
        final List<String> pathArgs = this.urlPathArgsDecode( encodedPathArgs );
        this.omitFirstAndLastEmptyString( pathArgs );
        if( pathArgs.isEmpty() ) {
            return null;
        }

        String first = pathArgs.iterator().next();
        final String startModule = ControllerContext.toModuleName( first );
        if( startModule == null ) {
            throw new ResponseException( Status.BAD_REQUEST,
                    "First node in URI has to be in format \"moduleName:nodeName\"" );
        }

        InstanceIdentifierBuilder builder = InstanceIdentifier.builder();
        Module latestModule = this.getLatestModule( globalSchema, startModule );
        InstanceIdWithSchemaNode iiWithSchemaNode = this.collectPathArguments( builder, pathArgs,
                                                           latestModule, null, toMountPointIdentifier );

        if( iiWithSchemaNode == null ) {
            throw new ResponseException( Status.BAD_REQUEST, "URI has bad format" );
        }

        return iiWithSchemaNode;
    }

    private List<String> omitFirstAndLastEmptyString( final List<String> list ) {
        if( list.isEmpty() ) {
            return list;
        }

        String head = list.iterator().next();
        if( head.isEmpty() ) {
            list.remove( 0 );
        }

        if( list.isEmpty() ) {
            return list;
        }

        String last = list.get( list.size() - 1 );
        if( last.isEmpty() ) {
            list.remove( list.size() - 1 );
        }

        return list;
    }

    private Module getLatestModule( final SchemaContext schema, final String moduleName ) {
        Preconditions.checkArgument( schema != null );
        Preconditions.checkArgument( moduleName != null && !moduleName.isEmpty() );

        Predicate<Module> filter = new Predicate<Module>() {
            @Override
            public boolean apply( Module m ) {
                return Objects.equal( m.getName(), moduleName );
            }
        };

        Iterable<Module> modules = Iterables.filter( schema.getModules(), filter );
        return this.filterLatestModule( modules );
    }

    private Module filterLatestModule( final Iterable<Module> modules ) {
        Module latestModule = modules.iterator().hasNext() ? modules.iterator().next() : null;
        for( final Module module : modules ) {
            if( module.getRevision().after( latestModule.getRevision() ) ) {
                latestModule = module;
            }
        }
        return latestModule;
    }

    public Module findModuleByName( final String moduleName ) {
        this.checkPreconditions();
        Preconditions.checkArgument( moduleName != null && !moduleName.isEmpty() );
        return this.getLatestModule( globalSchema, moduleName );
    }

    public Module findModuleByName( final MountInstance mountPoint, final String moduleName ) {
        Preconditions.checkArgument( moduleName != null && mountPoint != null );

        final SchemaContext mountPointSchema = mountPoint.getSchemaContext();
        return mountPointSchema == null ? null : this.getLatestModule( mountPointSchema, moduleName );
    }

    public Module findModuleByNamespace( final URI namespace ) {
        this.checkPreconditions();
        Preconditions.checkArgument( namespace != null );

        final Set<Module> moduleSchemas = globalSchema.findModuleByNamespace( namespace );
        return moduleSchemas == null ? null : this.filterLatestModule( moduleSchemas );
    }

    public Module findModuleByNamespace( final MountInstance mountPoint, final URI namespace ) {
        Preconditions.checkArgument( namespace != null && mountPoint != null );

        final SchemaContext mountPointSchema = mountPoint.getSchemaContext();
        Set<Module> moduleSchemas = mountPointSchema == null ? null :
                                                mountPointSchema.findModuleByNamespace( namespace );
        return moduleSchemas == null ? null : this.filterLatestModule( moduleSchemas );
    }

    public Module findModuleByNameAndRevision( final QName module ) {
        this.checkPreconditions();
        Preconditions.checkArgument( module != null && module.getLocalName() != null &&
                                     module.getRevision() != null );

        return globalSchema.findModuleByName( module.getLocalName(), module.getRevision() );
    }

    public Module findModuleByNameAndRevision( final MountInstance mountPoint, final QName module ) {
        this.checkPreconditions();
        Preconditions.checkArgument( module != null && module.getLocalName() != null &&
                                     module.getRevision() != null && mountPoint != null );

        SchemaContext schemaContext = mountPoint.getSchemaContext();
        return schemaContext == null ? null :
                       schemaContext.findModuleByName( module.getLocalName(), module.getRevision() );
    }

    public DataNodeContainer getDataNodeContainerFor( final InstanceIdentifier path ) {
        this.checkPreconditions();

        final List<PathArgument> elements = path.getPath();
        PathArgument head = elements.iterator().next();
        final QName startQName = head.getNodeType();
        final Module initialModule = globalSchema.findModuleByNamespaceAndRevision(
                startQName.getNamespace(), startQName.getRevision() );
        DataNodeContainer node = initialModule;
        for( final PathArgument element : elements ) {
            QName _nodeType = element.getNodeType();
            final DataSchemaNode potentialNode = ControllerContext.childByQName( node, _nodeType );
            if( potentialNode == null || !this.isListOrContainer( potentialNode ) ) {
                return null;
            }
            node = (DataNodeContainer) potentialNode;
        }

        return node;
    }

    public String toFullRestconfIdentifier( final InstanceIdentifier path ) {
        this.checkPreconditions();

        final List<PathArgument> elements = path.getPath();
        final StringBuilder builder = new StringBuilder();
        PathArgument head = elements.iterator().next();
        final QName startQName = head.getNodeType();
        final Module initialModule = globalSchema.findModuleByNamespaceAndRevision(
                startQName.getNamespace(), startQName.getRevision() );
        DataNodeContainer node = initialModule;
        for( final PathArgument element : elements ) {
            QName _nodeType = element.getNodeType();
            final DataSchemaNode potentialNode = ControllerContext.childByQName( node, _nodeType );
            if( !this.isListOrContainer( potentialNode ) ) {
                return null;
            }
            node = ((DataNodeContainer) potentialNode);
            builder.append( this.convertToRestconfIdentifier( element, node ) );
        }

        return builder.toString();
    }

    public String findModuleNameByNamespace( final URI namespace ) {
        this.checkPreconditions();

        String moduleName = this.uriToModuleName.get( namespace );
        if( moduleName == null ) {
            final Module module = this.findModuleByNamespace( namespace );
            if( module != null ) {
                moduleName = module.getName();
                this.uriToModuleName.put( namespace, moduleName );
            }
        }

        return moduleName;
    }

    public String findModuleNameByNamespace( final MountInstance mountPoint, final URI namespace ) {
        final Module module = this.findModuleByNamespace( mountPoint, namespace );
        return module == null ? null : module.getName();
    }

    public URI findNamespaceByModuleName( final String moduleName ) {
        URI namespace = this.moduleNameToUri.get( moduleName );
        if( namespace == null ) {
            Module module = this.findModuleByName( moduleName );
            if( module != null ) {
                URI _namespace = module.getNamespace();
                namespace = _namespace;
                this.uriToModuleName.put( namespace, moduleName );
            }
        }
        return namespace;
    }

    public URI findNamespaceByModuleName( final MountInstance mountPoint, final String moduleName ) {
        final Module module = this.findModuleByName( mountPoint, moduleName );
        return module == null ? null : module.getNamespace();
    }

    public Set<Module> getAllModules( final MountInstance mountPoint ) {
        this.checkPreconditions();

        SchemaContext schemaContext = mountPoint == null ? null : mountPoint.getSchemaContext();
        return schemaContext == null ? null : schemaContext.getModules();
    }

    public Set<Module> getAllModules() {
        this.checkPreconditions();
        return globalSchema.getModules();
    }

    public CharSequence toRestconfIdentifier( final QName qname ) {
        this.checkPreconditions();

        String module = this.uriToModuleName.get( qname.getNamespace() );
        if( module == null ) {
            final Module moduleSchema = globalSchema.findModuleByNamespaceAndRevision(
                                                       qname.getNamespace(), qname.getRevision() );
            if( moduleSchema == null ) {
                return null;
            }

            this.uriToModuleName.put( qname.getNamespace(), moduleSchema.getName() );
            module = moduleSchema.getName();
        }

        StringBuilder builder = new StringBuilder();
        builder.append( module );
        builder.append( ":" );
        builder.append( qname.getLocalName() );
        return builder.toString();
    }

    public CharSequence toRestconfIdentifier( final MountInstance mountPoint, final QName qname ) {
        if( mountPoint == null ) {
            return null;
        }

        SchemaContext schemaContext = mountPoint.getSchemaContext();

        final Module moduleSchema = schemaContext.findModuleByNamespaceAndRevision(
                                                       qname.getNamespace(), qname.getRevision() );
        if( moduleSchema == null ) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append( moduleSchema.getName() );
        builder.append( ":" );
        builder.append( qname.getLocalName() );
        return builder.toString();
    }

    private static DataSchemaNode childByQName( final ChoiceNode container, final QName name ) {
        for( final ChoiceCaseNode caze : container.getCases() ) {
            final DataSchemaNode ret = ControllerContext.childByQName( caze, name );
            if( ret != null ) {
                return ret;
            }
        }

        return null;
    }

    private static DataSchemaNode childByQName( final ChoiceCaseNode container, final QName name ) {
        return container.getDataChildByName( name );
    }

    private static DataSchemaNode childByQName( final ContainerSchemaNode container, final QName name ) {
        return ControllerContext.dataNodeChildByQName( container, name );
    }

    private static DataSchemaNode childByQName( final ListSchemaNode container, final QName name ) {
        return ControllerContext.dataNodeChildByQName( container, name );
    }

    private static DataSchemaNode childByQName( final Module container, final QName name ) {
        return ControllerContext.dataNodeChildByQName( container, name );
    }

    private static DataSchemaNode childByQName( final DataSchemaNode container, final QName name ) {
        return null;
    }

    private static DataSchemaNode dataNodeChildByQName( final DataNodeContainer container, final QName name ) {
        DataSchemaNode ret = container.getDataChildByName( name );
        if( ret == null ) {
            for( final DataSchemaNode node : container.getChildNodes() ) {
                if( (node instanceof ChoiceCaseNode) ) {
                    final ChoiceCaseNode caseNode = ((ChoiceCaseNode) node);
                    DataSchemaNode childByQName = ControllerContext.childByQName( caseNode, name );
                    if( childByQName != null ) {
                        return childByQName;
                    }
                }
            }
        }
        return ret;
    }

    private String toUriString( final Object object ) throws UnsupportedEncodingException {
        return object == null ? "" :
                       URLEncoder.encode( object.toString(), ControllerContext.URI_ENCODING_CHAR_SET );
    }

    private InstanceIdWithSchemaNode collectPathArguments( final InstanceIdentifierBuilder builder,
            final List<String> strings, final DataNodeContainer parentNode, final MountInstance mountPoint,
            final boolean returnJustMountPoint ) {
        Preconditions.<List<String>> checkNotNull( strings );

        if( parentNode == null ) {
            return null;
        }

        if( strings.isEmpty() ) {
            return new InstanceIdWithSchemaNode( builder.toInstance(),
                                                 ((DataSchemaNode) parentNode), mountPoint );
        }

        String head = strings.iterator().next();
        final String nodeName = this.toNodeName( head );
        final String moduleName = ControllerContext.toModuleName( head );

        DataSchemaNode targetNode = null;
        if( !Strings.isNullOrEmpty( moduleName ) ) {
            if( Objects.equal( moduleName, ControllerContext.MOUNT_MODULE ) &&
                Objects.equal( nodeName, ControllerContext.MOUNT_NODE ) ) {
                if( mountPoint != null ) {
                    throw new ResponseException( Status.BAD_REQUEST,
                                         "Restconf supports just one mount point in URI." );
                }

                if( mountService == null ) {
                    throw new ResponseException( Status.SERVICE_UNAVAILABLE,
                                "MountService was not found. Finding behind mount points does not work." );
                }

                final InstanceIdentifier partialPath = builder.toInstance();
                final MountInstance mount = mountService.getMountPoint( partialPath );
                if( mount == null ) {
                    LOG.debug( "Instance identifier to missing mount point: {}", partialPath );
                    throw new ResponseException( Status.BAD_REQUEST,
                                                 "Mount point does not exist." );
                }

                final SchemaContext mountPointSchema = mount.getSchemaContext();
                if( mountPointSchema == null ) {
                    throw new ResponseException( Status.BAD_REQUEST,
                                       "Mount point does not contain any schema with modules." );
                }

                if( returnJustMountPoint ) {
                    InstanceIdentifier instance = InstanceIdentifier.builder().toInstance();
                    return new InstanceIdWithSchemaNode( instance, mountPointSchema, mount );
                }

                if( strings.size() == 1 ) {
                    InstanceIdentifier instance = InstanceIdentifier.builder().toInstance();
                    return new InstanceIdWithSchemaNode( instance, mountPointSchema, mount );
                }

                final String moduleNameBehindMountPoint = toModuleName(  strings.get( 1 ) );
                if( moduleNameBehindMountPoint == null ) {
                    throw new ResponseException( Status.BAD_REQUEST,
                            "First node after mount point in URI has to be in format \"moduleName:nodeName\"" );
                }

                final Module moduleBehindMountPoint = this.getLatestModule( mountPointSchema,
                                                                            moduleNameBehindMountPoint );
                if( moduleBehindMountPoint == null ) {
                    throw new ResponseException( Status.BAD_REQUEST,
                                                 "URI has bad format. \"" + moduleName +
                                                 "\" module does not exist in mount point." );
                }

                List<String> subList = strings.subList( 1, strings.size() );
                return this.collectPathArguments( InstanceIdentifier.builder(), subList, moduleBehindMountPoint,
                                                  mount, returnJustMountPoint );
            }

            Module module = null;
            if( mountPoint == null ) {
                module = this.getLatestModule( globalSchema, moduleName );
                if( module == null ) {
                    throw new ResponseException( Status.BAD_REQUEST,
                            "URI has bad format. \"" + moduleName + "\" module does not exist." );
                }
            }
            else {
                SchemaContext schemaContext = mountPoint.getSchemaContext();
                module = schemaContext == null ? null :
                                          this.getLatestModule( schemaContext, moduleName );
                if( module == null ) {
                    throw new ResponseException( Status.BAD_REQUEST,
                                        "URI has bad format. \"" + moduleName +
                                        "\" module does not exist in mount point." );
                }
            }

            targetNode = this.findInstanceDataChildByNameAndNamespace(
                                          parentNode, nodeName, module.getNamespace() );;
            if( targetNode == null ) {
                throw new ResponseException( Status.BAD_REQUEST,
                            "URI has bad format. Possible reasons:\n" +
                            "1. \"" + head + "\" was not found in parent data node.\n" +
                            "2. \"" + head + "\" is behind mount point. Then it should be in format \"/" +
                            MOUNT + "/" + head + "\"." );
            }
        } else {
            final List<DataSchemaNode> potentialSchemaNodes =
                                          this.findInstanceDataChildrenByName( parentNode, nodeName );
            if( potentialSchemaNodes.size() > 1 ) {
                final StringBuilder strBuilder = new StringBuilder();
                for( final DataSchemaNode potentialNodeSchema : potentialSchemaNodes ) {
                    strBuilder.append( "   " )
                              .append( potentialNodeSchema.getQName().getNamespace() )
                              .append( "\n" );
                }

                throw new ResponseException( Status.BAD_REQUEST,
                        "URI has bad format. Node \"" + nodeName +
                        "\" is added as augment from more than one module. " +
                        "Therefore the node must have module name and it has to be in format \"moduleName:nodeName\"." +
                        "\nThe node is added as augment from modules with namespaces:\n" +
                        strBuilder.toString() );
            }

            if( potentialSchemaNodes.isEmpty() ) {
                throw new ResponseException( Status.BAD_REQUEST, "URI has bad format. \"" + nodeName +
                                             "\" was not found in parent data node.\n" );
            }

            targetNode = potentialSchemaNodes.iterator().next();
        }

        if( !this.isListOrContainer( targetNode ) ) {
            throw new ResponseException( Status.BAD_REQUEST,
                                         "URI has bad format. Node \"" + head +
                                         "\" must be Container or List yang type." );
        }

        int consumed = 1;
        if( (targetNode instanceof ListSchemaNode) ) {
            final ListSchemaNode listNode = ((ListSchemaNode) targetNode);
            final int keysSize = listNode.getKeyDefinition().size();
            if( (strings.size() - consumed) < keysSize ) {
                throw new ResponseException( Status.BAD_REQUEST, "Missing key for list \"" +
                                             listNode.getQName().getLocalName() + "\"." );
            }

            final List<String> uriKeyValues = strings.subList( consumed, consumed + keysSize );
            final HashMap<QName, Object> keyValues = new HashMap<QName, Object>();
            int i = 0;
            for( final QName key : listNode.getKeyDefinition() ) {
                {
                    final String uriKeyValue = uriKeyValues.get( i );
                    if( uriKeyValue.equals( NULL_VALUE ) ) {
                        throw new ResponseException( Status.BAD_REQUEST,
                                    "URI has bad format. List \"" + listNode.getQName().getLocalName() +
                                    "\" cannot contain \"null\" value as a key." );
                    }

                    this.addKeyValue( keyValues, listNode.getDataChildByName( key ),
                                      uriKeyValue, mountPoint );
                    i++;
                }
            }

            consumed = consumed + i;
            builder.nodeWithKey( targetNode.getQName(), keyValues );
        }
        else {
            builder.node( targetNode.getQName() );
        }

        if( (targetNode instanceof DataNodeContainer) ) {
            final List<String> remaining = strings.subList( consumed, strings.size() );
            return this.collectPathArguments( builder, remaining,
                              ((DataNodeContainer) targetNode), mountPoint, returnJustMountPoint );
        }

        return new InstanceIdWithSchemaNode( builder.toInstance(), targetNode, mountPoint );
    }

    public DataSchemaNode findInstanceDataChildByNameAndNamespace( final DataNodeContainer container,
            final String name, final URI namespace ) {
        Preconditions.<URI> checkNotNull( namespace );

        final List<DataSchemaNode> potentialSchemaNodes = this.findInstanceDataChildrenByName( container, name );

        Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply( DataSchemaNode node ) {
                return Objects.equal( node.getQName().getNamespace(), namespace );
            }
        };

        Iterable<DataSchemaNode> result = Iterables.filter( potentialSchemaNodes, filter );
        return Iterables.getFirst( result, null );
    }

    public List<DataSchemaNode> findInstanceDataChildrenByName( final DataNodeContainer container,
                                                                final String name ) {
        Preconditions.<DataNodeContainer> checkNotNull( container );
        Preconditions.<String> checkNotNull( name );

        List<DataSchemaNode> instantiatedDataNodeContainers = new ArrayList<DataSchemaNode>();
        this.collectInstanceDataNodeContainers( instantiatedDataNodeContainers, container, name );
        return instantiatedDataNodeContainers;
    }

    private void collectInstanceDataNodeContainers( final List<DataSchemaNode> potentialSchemaNodes,
            final DataNodeContainer container, final String name ) {

        Set<DataSchemaNode> childNodes = container.getChildNodes();

        Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply( DataSchemaNode node ) {
                return Objects.equal( node.getQName().getLocalName(), name );
            }
        };

        Iterable<DataSchemaNode> nodes = Iterables.filter( childNodes, filter );

        // Can't combine this loop with the filter above because the filter is lazily-applied
        // by Iterables.filter.
        for( final DataSchemaNode potentialNode : nodes ) {
            if( this.isInstantiatedDataSchema( potentialNode ) ) {
                potentialSchemaNodes.add( potentialNode );
            }
        }

        Iterable<ChoiceNode> choiceNodes = Iterables.<ChoiceNode> filter( container.getChildNodes(),
                                                                          ChoiceNode.class );

        final Function<ChoiceNode, Set<ChoiceCaseNode>> choiceFunction =
                new Function<ChoiceNode, Set<ChoiceCaseNode>>() {
            @Override
            public Set<ChoiceCaseNode> apply( final ChoiceNode node ) {
                return node.getCases();
            }
        };

        Iterable<Set<ChoiceCaseNode>> map = Iterables.<ChoiceNode, Set<ChoiceCaseNode>> transform(
                                                                           choiceNodes, choiceFunction );

        final Iterable<ChoiceCaseNode> allCases = Iterables.<ChoiceCaseNode> concat( map );
        for( final ChoiceCaseNode caze : allCases ) {
            this.collectInstanceDataNodeContainers( potentialSchemaNodes, caze, name );
        }
    }

    public boolean isInstantiatedDataSchema( final DataSchemaNode node ) {
        return node instanceof LeafSchemaNode || node instanceof LeafListSchemaNode ||
               node instanceof ContainerSchemaNode || node instanceof ListSchemaNode;
    }

    private void addKeyValue( final HashMap<QName, Object> map, final DataSchemaNode node,
                              final String uriValue, final MountInstance mountPoint ) {
        Preconditions.<String> checkNotNull( uriValue );
        Preconditions.checkArgument( (node instanceof LeafSchemaNode) );

        final String urlDecoded = urlPathArgDecode( uriValue );
        final TypeDefinition<? extends Object> typedef = ((LeafSchemaNode) node).getType();
        Codec<Object, Object> codec = RestCodec.from( typedef, mountPoint );

        Object decoded = codec == null ? null : codec.deserialize( urlDecoded );
        String additionalInfo = "";
        if( decoded == null ) {
            TypeDefinition<? extends Object> baseType = RestUtil.resolveBaseTypeFrom( typedef );
            if( (baseType instanceof IdentityrefTypeDefinition) ) {
                decoded = this.toQName( urlDecoded );
                additionalInfo = "For key which is of type identityref it should be in format module_name:identity_name.";
            }
        }

        if( decoded == null ) {
            throw new ResponseException( Status.BAD_REQUEST, uriValue + " from URI can\'t be resolved. " +
                                         additionalInfo );
        }

        map.put( node.getQName(), decoded );
    }

    private static String toModuleName( final String str ) {
        Preconditions.<String> checkNotNull( str );
        if( str.contains( ":" ) ) {
            final String[] args = str.split( ":" );
            if( args.length == 2 ) {
                return args[0];
            }
        }
        return null;
    }

    private String toNodeName( final String str ) {
        if( str.contains( ":" ) ) {
            final String[] args = str.split( ":" );
            if( args.length == 2 ) {
                return args[1];
            }
        }
        return str;
    }

    private QName toQName( final String name ) {
        final String module = toModuleName( name );
        final String node = this.toNodeName( name );
        Set<Module> modules = globalSchema.getModules();

        final Comparator<Module> comparator = new Comparator<Module>() {
            @Override
            public int compare( final Module o1, final Module o2 ) {
                return o1.getRevision().compareTo( o2.getRevision() );
            }
        };

        List<Module> sorted = new ArrayList<Module>( modules );
        Collections.<Module> sort( new ArrayList<Module>( modules ), comparator );

        final Function<Module, QName> transform = new Function<Module, QName>() {
            @Override
            public QName apply( final Module m ) {
                return QName.create( m.getNamespace(), m.getRevision(), m.getName() );
            }
        };

        final Predicate<QName> findFirst = new Predicate<QName>() {
            @Override
            public boolean apply( final QName qn ) {
                return Objects.equal( module, qn.getLocalName() );
            }
        };

        Optional<QName> namespace = FluentIterable.from( sorted )
                                                  .transform( transform )
                                                  .firstMatch( findFirst );
        return namespace.isPresent() ? QName.create( namespace.get(), node ) : null;
    }

    private boolean isListOrContainer( final DataSchemaNode node ) {
        return node instanceof ListSchemaNode || node instanceof ContainerSchemaNode;
    }

    public RpcDefinition getRpcDefinition( final String name ) {
        final QName validName = this.toQName( name );
        return validName == null ? null : this.qnameToRpc.get( validName );
    }

    @Override
    public void onGlobalContextUpdated( final SchemaContext context ) {
        if( context != null ) {
            this.qnameToRpc.clear();
            this.setGlobalSchema( context );
            Set<RpcDefinition> _operations = context.getOperations();
            for( final RpcDefinition operation : _operations ) {
                {
                    this.qnameToRpc.put( operation.getQName(), operation );
                }
            }
        }
    }

    public List<String> urlPathArgsDecode( final List<String> strings ) {
        try {
            List<String> decodedPathArgs = new ArrayList<String>();
            for( final String pathArg : strings ) {
                String _decode = URLDecoder.decode( pathArg, URI_ENCODING_CHAR_SET );
                decodedPathArgs.add( _decode );
            }
            return decodedPathArgs;
        }
        catch( UnsupportedEncodingException e ) {
            throw new ResponseException( Status.BAD_REQUEST,
                    "Invalid URL path '" + strings + "': " + e.getMessage() );
        }
    }

    public String urlPathArgDecode( final String pathArg ) {
        if( pathArg != null ) {
            try {
                return URLDecoder.decode( pathArg, URI_ENCODING_CHAR_SET );
            }
            catch( UnsupportedEncodingException e ) {
                throw new ResponseException( Status.BAD_REQUEST,
                        "Invalid URL path arg '" + pathArg + "': " + e.getMessage() );
            }
        }

        return null;
    }

    private CharSequence convertToRestconfIdentifier( final PathArgument argument,
                                                      final DataNodeContainer node ) {
        if( argument instanceof NodeIdentifier && node instanceof ContainerSchemaNode ) {
            return convertToRestconfIdentifier( (NodeIdentifier) argument, (ContainerSchemaNode) node );
        }
        else if( argument instanceof NodeIdentifierWithPredicates && node instanceof ListSchemaNode ) {
            return convertToRestconfIdentifier( (NodeIdentifierWithPredicates) argument, (ListSchemaNode) node );
        }
        else if( argument != null && node != null ) {
            throw new IllegalArgumentException(
                                     "Conversion of generic path argument is not supported" );
        }
        else {
            throw new IllegalArgumentException( "Unhandled parameter types: "
                    + Arrays.<Object> asList( argument, node ).toString() );
        }
    }

    private CharSequence convertToRestconfIdentifier( final NodeIdentifier argument,
                                                      final ContainerSchemaNode node ) {
        StringBuilder builder = new StringBuilder();
        builder.append( "/" );
        QName nodeType = argument.getNodeType();
        builder.append( this.toRestconfIdentifier( nodeType ) );
        return builder.toString();
    }

    private CharSequence convertToRestconfIdentifier( final NodeIdentifierWithPredicates argument,
                                                      final ListSchemaNode node ) {
        QName nodeType = argument.getNodeType();
        final CharSequence nodeIdentifier = this.toRestconfIdentifier( nodeType );
        final Map<QName, Object> keyValues = argument.getKeyValues();

        StringBuilder builder = new StringBuilder();
        builder.append( "/" );
        builder.append( nodeIdentifier );
        builder.append( "/" );

        List<QName> keyDefinition = node.getKeyDefinition();
        boolean hasElements = false;
        for( final QName key : keyDefinition ) {
            if( !hasElements ) {
                hasElements = true;
            }
            else {
                builder.append( "/" );
            }

            try {
                builder.append( this.toUriString( keyValues.get( key ) ) );
            } catch( UnsupportedEncodingException e ) {
                LOG.error( "Error parsing URI: " + keyValues.get( key ), e );
                return null;
            }
        }

        return builder.toString();
    }

    private static DataSchemaNode childByQName( final Object container, final QName name ) {
        if( container instanceof ChoiceCaseNode ) {
            return childByQName( (ChoiceCaseNode) container, name );
        }
        else if( container instanceof ChoiceNode ) {
            return childByQName( (ChoiceNode) container, name );
        }
        else if( container instanceof ContainerSchemaNode ) {
            return childByQName( (ContainerSchemaNode) container, name );
        }
        else if( container instanceof ListSchemaNode ) {
            return childByQName( (ListSchemaNode) container, name );
        }
        else if( container instanceof DataSchemaNode ) {
            return childByQName( (DataSchemaNode) container, name );
        }
        else if( container instanceof Module ) {
            return childByQName( (Module) container, name );
        }
        else {
            throw new IllegalArgumentException( "Unhandled parameter types: "
                    + Arrays.<Object> asList( container, name ).toString() );
        }
    }
}
