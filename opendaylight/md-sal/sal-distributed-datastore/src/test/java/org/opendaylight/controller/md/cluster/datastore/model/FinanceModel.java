/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.cluster.datastore.model;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class FinanceModel {

    public static final QName BASE_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test:finance", "2015-05-29",
            "finance");

    public static final QName FINANCE_QNAME = QName.create(BASE_QNAME, "finance");
    public static final YangInstanceIdentifier BASE_PATH = YangInstanceIdentifier.of(BASE_QNAME);
    public static final YangInstanceIdentifier FINANCE_PATH = BASE_PATH.node(FINANCE_QNAME);


    public static final QName ACCOUNTS_QNAME = QName.create(FINANCE_QNAME, "accounts");
    public static final QName ACCOUNT_NAME_QNAME = QName.create(FINANCE_QNAME, "institution-name");

    public static final QName SUB_ACCOUNTS_QNAME = QName.create(ACCOUNTS_QNAME, "sub-accounts");
    public static final QName SUB_ACCOUNTS_NAME_QNAME = QName.create(SUB_ACCOUNTS_QNAME, "account-name");

    public static final QName TRANSACTIONS_QNAME = QName.create(SUB_ACCOUNTS_QNAME, "transactions");
    public static final QName TRANSACTION_DATE_QNAME = QName.create(TRANSACTIONS_QNAME, "date");
    public static final QName TRANSACTION_TYPE_QNAME = QName.create(TRANSACTIONS_QNAME, "type");
    public static final QName TRANSACTION_DESC_QNAME = QName.create(TRANSACTIONS_QNAME, "desc");
    public static final QName TRANSACTION_VALUE_QNAME = QName.create(TRANSACTIONS_QNAME, "value");

    public static final QName CATEGORIES_QNAME = QName.create(TRANSACTIONS_QNAME, "categories");
    public static final QName CATEGORY_NAME_QNAME = QName.create(CATEGORIES_QNAME, "name");
    public static final QName CATEGORY_PERCENT_QNAME = QName.create(CATEGORIES_QNAME, "percentage");


    public static QName qName(String fullPath){
        String[] strings = fullPath.split("/");

        QName parentQName = BASE_QNAME;

        for(String s : strings){
            parentQName = QName.create(parentQName, s);
        }

        return parentQName;
    }

    public static LeafNode<Object> leaf(QName qName, Object value){
        return Builders.leafBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(qName))
                .withValue(value)
                .build();
    }

    public static YangInstanceIdentifier.NodeIdentifierWithPredicates keyId(QName parent, QName key, Object value){
        return new YangInstanceIdentifier.NodeIdentifierWithPredicates(parent, key, value);
    }

    public static YangInstanceIdentifier.NodeIdentifier nodeId(QName name){
        return new YangInstanceIdentifier.NodeIdentifier(name);
    }


    public static NormalizedNode<?,?> createCategorizedTransaction(String institutionName,
                                                 String accountName,
                                                 long txnDate,
                                                 String txnType,
                                                 String txnDesc, long txnValue, String catName, short catPercentage){


        // Create a category
        MapEntryNode categoryEntry = Builders.mapEntryBuilder()
                .withNodeIdentifier(keyId(CATEGORIES_QNAME, CATEGORY_NAME_QNAME, catName))
                .withChild(leaf(CATEGORY_NAME_QNAME, catName))
                .withChild(leaf(CATEGORY_PERCENT_QNAME, catPercentage))
                .build();

        MapNode categoriesList = Builders.mapBuilder()
                .withNodeIdentifier(nodeId(CATEGORIES_QNAME))
                .withChild(categoryEntry)
                .build();

        // Create a transaction
        MapEntryNode transactionEntry = Builders.mapEntryBuilder()
                .withNodeIdentifier(keyId(TRANSACTIONS_QNAME, TRANSACTION_DATE_QNAME, txnDate))
                .withChild(leaf(TRANSACTION_DATE_QNAME, txnDate))
                .withChild(leaf(TRANSACTION_TYPE_QNAME, txnType))
                .withChild(leaf(TRANSACTION_DESC_QNAME, txnDesc))
                .withChild(leaf(TRANSACTION_VALUE_QNAME, txnValue))
                .withChild(categoriesList)
                .build();

        MapNode transactionsList = Builders.mapBuilder()
                .withNodeIdentifier(nodeId(TRANSACTIONS_QNAME))
                .withChild(transactionEntry)
                .build();


        // Create a sub-account
        MapEntryNode subAccountEntry = Builders.mapEntryBuilder()
                .withNodeIdentifier(keyId(SUB_ACCOUNTS_QNAME, SUB_ACCOUNTS_NAME_QNAME, accountName))
                .withChild(leaf(SUB_ACCOUNTS_NAME_QNAME, accountName))
                .withChild(transactionsList)
                .build();

        MapNode subAccountsList = Builders.mapBuilder()
                .withNodeIdentifier(nodeId(SUB_ACCOUNTS_QNAME))
                .withChild(subAccountEntry)
                .build();

        // Create an account
        MapEntryNode accountsEntry = Builders.mapEntryBuilder()
                .withNodeIdentifier(keyId(ACCOUNTS_QNAME, ACCOUNT_NAME_QNAME, institutionName))
                .withChild(leaf(ACCOUNT_NAME_QNAME, institutionName))
                .withChild(subAccountsList)
                .build();

        MapNode accountsList = Builders.mapBuilder()
                .withNodeIdentifier(nodeId(ACCOUNTS_QNAME))
                .withChild(accountsEntry)
                .build();

        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(FINANCE_QNAME))
                .withChild(accountsList)
                .build();

    }
}
