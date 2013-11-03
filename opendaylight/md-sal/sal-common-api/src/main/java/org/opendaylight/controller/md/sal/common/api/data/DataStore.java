package org.opendaylight.controller.md.sal.common.api.data;

public interface DataStore<P, D> extends //
        DataReader<P, D>, //
        DataModificationTransactionFactory<P, D> {

    @Override
    public DataModification<P, D> beginTransaction();

    @Override
    public D readConfigurationData(P path);

    @Override
    public D readOperationalData(P path);

}
