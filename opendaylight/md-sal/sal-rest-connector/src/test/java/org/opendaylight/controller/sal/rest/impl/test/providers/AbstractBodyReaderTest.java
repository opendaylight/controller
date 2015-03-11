/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.impl.test.providers;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.rest.api.RestconfConstants;
import org.opendaylight.controller.sal.rest.impl.AbstractIdentifierAwareJaxRsProvider;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * sal-rest-connector
 * org.opendaylight.controller.sal.rest.impl.test.providers
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 7, 2015
 */
public abstract class AbstractBodyReaderTest {

    protected final static ControllerContext controllerContext = ControllerContext.getInstance();
    protected final MediaType mediaType;
    private static Field uriField;

    public AbstractBodyReaderTest () throws NoSuchFieldException, SecurityException {
        uriField = AbstractIdentifierAwareJaxRsProvider.class.getDeclaredField("uriInfo");
        uriField.setAccessible(true);
        mediaType = getMediaType();
    }

    abstract MediaType getMediaType();

    protected static SchemaContext schemaContextLoader(final String yangPath, final SchemaContext schemaContext) {
        return TestRestconfUtils.loadSchemaContext(yangPath, schemaContext);
    }

    protected static <T extends AbstractIdentifierAwareJaxRsProvider> void mockBodyReader(
            final String identifier, final T normalizedNodeProvider) throws NoSuchFieldException,
            SecurityException, IllegalArgumentException, IllegalAccessException {
        final UriInfo uriInfoMock = mock(UriInfo.class);
        final MultivaluedMap<String, String> pathParm = new MultivaluedHashMap<>(1);
        pathParm.put(RestconfConstants.IDENTIFIER, Collections.singletonList(identifier));
        when(uriInfoMock.getPathParameters()).thenReturn(pathParm);
        when(uriInfoMock.getPathParameters(false)).thenReturn(pathParm);
        when(uriInfoMock.getPathParameters(true)).thenReturn(pathParm);
        uriField.set(normalizedNodeProvider, uriInfoMock);
    }

    protected static void checkMountPointNormalizedNodeContext(final NormalizedNodeContext nnContext) {
        checkNormalizedNodeContext(nnContext);
        assertNotNull(nnContext.getInstanceIdentifierContext().getMountPoint());
    }

    protected static void checkNormalizedNodeContext(final NormalizedNodeContext nnContext) {
        assertNotNull(nnContext);
        assertNotNull(nnContext.getData());
        assertNotNull(nnContext.getInstanceIdentifierContext());
        assertNotNull(nnContext.getInstanceIdentifierContext().getInstanceIdentifier());
        assertNotNull(nnContext.getInstanceIdentifierContext().getSchemaContext());
        assertNotNull(nnContext.getInstanceIdentifierContext().getSchemaNode());
    }
}
