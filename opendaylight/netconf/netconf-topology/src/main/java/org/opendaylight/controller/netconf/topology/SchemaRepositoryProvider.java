package org.opendaylight.controller.netconf.topology;

import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;

public interface SchemaRepositoryProvider{

    SharedSchemaRepository getSharedSchemaRepository();
}
