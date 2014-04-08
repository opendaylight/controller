/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class CrudTestUtil{

    private final static Logger log = Logger.getLogger(CrudTestUtil.class
            .getName());

    private CrudTestUtil(){
    }

    /**
     * BA test create
     * 
     * @param data
     * @param serviceBroker
     * @param path
     * @return dataObject
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <D extends DataObject, S extends org.opendaylight.controller.sal.binding.api.data.DataProviderService, I extends InstanceIdentifier>DataObject doCreateTest(
            final D data, final S serviceBroker, final I path)
            throws InterruptedException, ExecutionException{

        org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction modification = serviceBroker
                .beginTransaction();

        modification.putConfigurationData(path, data);
        RpcResult<TransactionStatus> result = modification.commit().get();

        assertNotNull("Result of the commit should not be null.", result);
        assertEquals("Successfully committed transaction shoul"
                + "d be equal to result of commit", TransactionStatus.COMMITED,
                result.getResult());

        DataObject obj = getDataObjectReader(modification, path);

        assertNotNull("Created object should not be null", obj);
        assertEquals("Created object should be equals to committed object.",
                obj, modification.readConfigurationData(path));

        log.info("Create object : " + data);

        return modification.readConfigurationData(path);
    }

    /**
     * BI test create
     * 
     * @param data
     * @param serviceBroker
     * @param path
     * @return composite node
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <D extends CompositeNode, S extends org.opendaylight.controller.sal.core.api.data.DataProviderService, I extends org.opendaylight.yangtools.yang.data.api.InstanceIdentifier>CompositeNode doCreateTest(
            final D data, final S serviceBroker, final I path)
            throws InterruptedException, ExecutionException{

        org.opendaylight.controller.sal.core.api.data.DataModificationTransaction modification = serviceBroker
                .beginTransaction();

        modification.putConfigurationData(path, data);
        RpcResult<TransactionStatus> result = modification.commit().get();

        assertNotNull("Result of the commit should not be null.", result);
        assertEquals(
                "Successfully committed transaction should be equal to result of commit.",
                TransactionStatus.COMMITED, result.getResult());

        CompositeNode readConfData = modification.readConfigurationData(path);
        assertNotNull("Created object should not be null", readConfData);
        assertEquals("Created object should be equals to committed object.",
                getCompositeNodeReader(modification, path),
                modification.readConfigurationData(path));

        log.info("Create object : " + data);
        return readConfData;
    }

    /**
     * BA test read
     * 
     * @param data
     * @param serviceBroker
     * @param path
     * @param modification
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <D extends DataObject, S extends org.opendaylight.controller.sal.binding.api.data.DataProviderService, I extends InstanceIdentifier>void doReadTest(
            final D data, final S serviceBroker, final I path)
            throws InterruptedException, ExecutionException{

        org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction modification = serviceBroker
                .beginTransaction();

        DataObject readedObject = serviceBroker.readConfigurationData(path);

        assertNotNull("Readed object should not be null. Object : " + data,
                readedObject);
        assertTrue("Readed object should be same type. Object : " + data,
                readedObject instanceof DataObject);
        log.info("Readed object : " + data);
    }

    /**
     * BI test read
     * 
     * @param data
     * @param serviceBroker
     * @param path
     * @param modification
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <D extends CompositeNode, S extends org.opendaylight.controller.sal.core.api.data.DataProviderService, I extends org.opendaylight.yangtools.yang.data.api.InstanceIdentifier>void doReadTest(
            final D data,
            final S serviceBroker,
            final I path,
            final org.opendaylight.controller.sal.core.api.data.DataModificationTransaction modification)
            throws InterruptedException, ExecutionException{

        CompositeNode readedCompositeNode = serviceBroker
                .readConfigurationData(path);

        assertNotNull("Readed object should not be null. Object : " + data,
                readedCompositeNode);
        assertTrue("Readed object should be same type. Object : " + data,
                readedCompositeNode instanceof CompositeNode);

        log.info("Readed object : " + data);
    }

    /**
     * BA test update
     * 
     * @param updateData
     * @param oldData
     * @param serviceBroker
     * @param path
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <D extends DataObject, S extends org.opendaylight.controller.sal.binding.api.data.DataProviderService, I extends InstanceIdentifier>void doUpdateTest(
            final D updateData, final D oldData, final S serviceBroker,
            final I path) throws InterruptedException, ExecutionException{

        org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction modification = serviceBroker
                .beginTransaction();

        assertNotNull("Readed object should not be null", oldData);
        assertNotNull("Updated object should not be null", updateData);
        DataObject brm = modification.readConfigurationData(path);
        assertTrue("Objetcs should not be equals.",
                !(updateData.equals(modification.readConfigurationData(path))));

        modification.removeConfigurationData(path);
        modification.putConfigurationData(path, updateData);

        assertTrue("Objetcs should not be equals.",
                !(oldData.equals(modification.readConfigurationData(path))));

        log.info("Updated object :" + updateData);
    }

    /**
     * BI test update
     * 
     * @param updateData
     * @param oldData
     * @param serviceBroker
     * @param path
     * @param modification
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <D extends CompositeNode, S extends org.opendaylight.controller.sal.core.api.data.DataProviderService, I extends org.opendaylight.yangtools.yang.data.api.InstanceIdentifier>void doUpdateTest(
            final D updateData,
            final D oldData,
            final S serviceBroker,
            final I path,
            final org.opendaylight.controller.sal.core.api.data.DataModificationTransaction modification)
            throws InterruptedException, ExecutionException{

        assertNotNull("Readed object should not be null", oldData);
        assertNotNull("Updated object should not be null", updateData);
        assertTrue("Objetcs should not be equals.",
                !(getCompositeNodeReader(modification, path)
                        .equals(modification.readConfigurationData(path))));

        modification.removeConfigurationData(path);

        doCreateTest(updateData, serviceBroker, path);

        log.info("Updated object :" + updateData);
    }

    /**
     * BA test remove
     * 
     * @param data
     * @param serviceBroker
     * @param path
     * @param modification
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <D extends DataObject, S extends org.opendaylight.controller.sal.binding.api.data.DataProviderService, I extends InstanceIdentifier>void doRemoveTest(
            final D data, final S serviceBroker, final I path)
            throws InterruptedException, ExecutionException{

        org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction modification = serviceBroker
                .beginTransaction();

        modification.removeConfigurationData(path);

        RpcResult<TransactionStatus> result = modification.commit().get();
        assertEquals(
                "Successfully committed transaction should be equal to result of commit.",
                TransactionStatus.COMMITED, result.getResult());

        DataObject a = modification.readConfigurationData(path);
        assertTrue("musis byt null", a == null);

        log.info("Remove object : " + data);

    }

    /**
     * BI test remove
     * 
     * @param data
     * @param serviceBroker
     * @param path
     * @param modification
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <D extends CompositeNode, S extends org.opendaylight.controller.sal.core.api.data.DataProviderService, I extends org.opendaylight.yangtools.yang.data.api.InstanceIdentifier>void doRemoveTest(
            final D data, final S serviceBroker, final I path)
            throws InterruptedException, ExecutionException{

        org.opendaylight.controller.sal.core.api.data.DataModificationTransaction modification = serviceBroker
                .beginTransaction();

        modification.removeConfigurationData(path);

        RpcResult<TransactionStatus> result = modification.commit().get();
        assertEquals(
                "Successfully commited transaction should be equal to result of commit.",
                TransactionStatus.COMMITED, result.getResult());

        log.info("Remove object : " + data);
    }

    /**
     * BA get configuration data
     * 
     * @param modification
     * @param path
     * @return DataObject
     */
    private static <I extends InstanceIdentifier>DataObject getDataObjectReader(
            final org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction modification,
            final I path){
        return modification.readConfigurationData(path);
    }

    /**
     * BI get configuration data
     * 
     * @param modification
     * @param path
     * @return CompositeNode
     */
    private static <I extends org.opendaylight.yangtools.yang.data.api.InstanceIdentifier>CompositeNode getCompositeNodeReader(
            final org.opendaylight.controller.sal.core.api.data.DataModificationTransaction modification,
            final I path){
        return modification.readConfigurationData(path);
    }

}
