/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.mountpoints;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.controller.sal.rest.doc.impl.BaseYangSwaggerGenerator;
import org.opendaylight.controller.sal.rest.doc.swagger.Api;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.Operation;
import org.opendaylight.controller.sal.rest.doc.swagger.Resource;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class MountPointSwagger extends BaseYangSwaggerGenerator implements MountProvisionListener {

    private static final String DATASTORES_REVISION = "-";
    private static final String DATASTORES_LABEL = "Datastores";

    private MountProvisionService mountService;
    private final Map<YangInstanceIdentifier, Long> instanceIdToLongId = new TreeMap<>(
            new Comparator<YangInstanceIdentifier>() {
                @Override
                public int compare(final YangInstanceIdentifier o1, final YangInstanceIdentifier o2) {
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });
    private final Map<Long, YangInstanceIdentifier> longIdToInstanceId = new HashMap<>();
    private final Object lock = new Object();

    private final AtomicLong idKey = new AtomicLong(0);

    private static AtomicReference<MountPointSwagger> selfRef = new AtomicReference<>();
    private SchemaService globalSchema;

    public Map<String, Long> getInstanceIdentifiers() {
        Map<String, Long> urlToId = new HashMap<>();
        synchronized (lock) {
            SchemaContext context = globalSchema.getGlobalContext();
            for (Entry<YangInstanceIdentifier, Long> entry : instanceIdToLongId.entrySet()) {
                String modName = findModuleName(entry.getKey(), context);
                urlToId.put(generateUrlPrefixFromInstanceID(entry.getKey(), modName),
                        entry.getValue());
            }
        }
        return urlToId;
    }

    public void setGlobalSchema(final SchemaService globalSchema) {
        this.globalSchema = globalSchema;
    }

    private String findModuleName(final YangInstanceIdentifier id, final SchemaContext context) {
        PathArgument rootQName = id.getPathArguments().iterator().next();
        for (Module mod : context.getModules()) {
            if (mod.getDataChildByName(rootQName.getNodeType()) != null) {
                return mod.getName();
            }
        }
        return null;
    }

    private String generateUrlPrefixFromInstanceID(final YangInstanceIdentifier key, final String moduleName) {
        StringBuilder builder = new StringBuilder();
        if (moduleName != null) {
            builder.append(moduleName);
            builder.append(':');
        }
        boolean first = true;
        for (PathArgument arg : key.getPathArguments()) {

            String name = arg.getNodeType().getLocalName();
            if (first) {
                first = false;
            } else {
                builder.append('/');
            }
            builder.append(name);
            if (arg instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
                NodeIdentifierWithPredicates nodeId = (NodeIdentifierWithPredicates) arg;
                for (Entry<QName, Object> entry : nodeId.getKeyValues().entrySet()) {
                    builder.append('/').append(entry.getValue());
                }
            }
        }

        return builder.append('/').toString();
    }

    private String getYangMountUrl(final YangInstanceIdentifier key) {
        String modName = findModuleName(key, globalSchema.getGlobalContext());
        return generateUrlPrefixFromInstanceID(key, modName) + "yang-ext:mount/";
    }

    public ResourceList getResourceList(final UriInfo uriInfo, final Long id) {
        YangInstanceIdentifier iid = getInstanceId(id);
        if (iid == null) {
            return null; // indicating not found.
        }
        SchemaContext context = getSchemaContext(iid);
        String urlPrefix = getYangMountUrl(iid);
        if (context == null) {
            return createResourceList();
        }
        List<Resource> resources = new LinkedList<>();
        Resource dataStores = new Resource();
        dataStores.setDescription("Provides methods for accessing the data stores.");
        dataStores.setPath(generatePath(uriInfo, DATASTORES_LABEL, DATASTORES_REVISION));
        resources.add(dataStores);
        ResourceList list = super.getResourceListing(uriInfo, context, urlPrefix);
        resources.addAll(list.getApis());
        list.setApis(resources);
        return list;
    }

    private YangInstanceIdentifier getInstanceId(final Long id) {
        YangInstanceIdentifier instanceId;
        synchronized (lock) {
            instanceId = longIdToInstanceId.get(id);
        }
        return instanceId;
    }

    private SchemaContext getSchemaContext(final YangInstanceIdentifier id) {

        if (id == null) {
            return null;
        }

        MountProvisionInstance mountPoint = mountService.getMountPoint(id);
        if (mountPoint == null) {
            return null;
        }

        SchemaContext context = mountPoint.getSchemaContext();
        if (context == null) {
            return null;
        }
        return context;
    }

    public ApiDeclaration getMountPointApi(final UriInfo uriInfo, final Long id, final String module, final String revision) {
        YangInstanceIdentifier iid = getInstanceId(id);
        SchemaContext context = getSchemaContext(iid);
        String urlPrefix = getYangMountUrl(iid);
        if (context == null) {
            return null;
        }

        if (DATASTORES_LABEL.equals(module) && DATASTORES_REVISION.equals(revision)) {
            return generateDataStoreApiDoc(uriInfo, urlPrefix);
        }
        return super.getApiDeclaration(module, revision, uriInfo, context, urlPrefix);
    }

    private ApiDeclaration generateDataStoreApiDoc(final UriInfo uriInfo, final String context) {

        ApiDeclaration declaration = super.createApiDeclaration(createBasePathFromUriInfo(uriInfo));
        List<Api> apis = new LinkedList<>();
        apis.add(createGetApi("config",
                "Queries the config (startup) datastore on the mounted hosted.", context));
        apis.add(createGetApi("operational",
                "Queries the operational (running) datastore on the mounted hosted.", context));
        apis.add(createGetApi("operations",
                "Queries the available operations (RPC calls) on the mounted hosted.", context));
        declaration.setApis(apis);
        return declaration;

    }

    private Api createGetApi(final String datastore, final String note, final String context) {
        Operation getConfig = new Operation();
        getConfig.setMethod("GET");
        getConfig.setNickname("GET " + datastore);
        getConfig.setNotes(note);

        Api api = new Api();
        api.setPath(getDataStorePath("/" + datastore + "/", context));
        api.setOperations(Collections.singletonList(getConfig));

        return api;
    }

    public void setMountService(final MountProvisionService mountService) {
        this.mountService = mountService;
    }

    @Override
    public void onMountPointCreated(final YangInstanceIdentifier path) {
        synchronized (lock) {
            Long idLong = idKey.incrementAndGet();
            instanceIdToLongId.put(path, idLong);
            longIdToInstanceId.put(idLong, path);
        }
    }

    @Override
    public void onMountPointRemoved(final YangInstanceIdentifier path) {
        synchronized (lock) {
            Long id = instanceIdToLongId.remove(path);
            longIdToInstanceId.remove(id);
        }
    }

    public static MountPointSwagger getInstance() {
        MountPointSwagger swagger = selfRef.get();
        if (swagger == null) {
            selfRef.compareAndSet(null, new MountPointSwagger());
            swagger = selfRef.get();
        }
        return swagger;
    }

}
