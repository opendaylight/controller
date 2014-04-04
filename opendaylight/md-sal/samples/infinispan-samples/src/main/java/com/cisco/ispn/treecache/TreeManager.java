package com.cisco.ispn.treecache;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;
import org.infinispan.tree.TreeCache;
import org.infinispan.tree.TreeCacheImpl;
import org.infinispan.util.concurrent.IsolationLevel;

/**
 * @author: syedbahm
 * Date: 4/1/14
 */
public class TreeManager {
  Configuration config = null;
  DefaultCacheManager dcm = null;

  TreeManager(){
    initTreeCache(CacheMode.LOCAL, IsolationLevel.REPEATABLE_READ, LockingMode.OPTIMISTIC,TransactionMode.TRANSACTIONAL,true);
  }
  /**
   * Initializes ISPN cache
   */
  public TreeManager initTreeCache(CacheMode cacheMode, IsolationLevel isolation, LockingMode lm,  TransactionMode transactionMode, boolean writeSkewCheck) {



     config = new ConfigurationBuilder()
                                      .clustering()
                                      .cacheMode(cacheMode)
                                      .transaction()
                                      .transactionManagerLookup(new JBossStandaloneJTAManagerLookup())
                                      .transactionMode(transactionMode)  //transaction or no transaction
                                      .lockingMode(lm) //optmistic or pessimistic -- ISPN default is optimistic
                                      .clustering()
                                      .locking().isolationLevel(isolation)  //read-committed or repeatable reads
                                      .writeSkewCheck(writeSkewCheck)
                                      .clustering()
                                      .invocationBatching().enable()//this is a requirement for tree cache
                                      .clustering()
                                      .versioning().scheme(VersioningScheme.SIMPLE).enable()
                                      .clustering()
                                      .build();


     dcm = new DefaultCacheManager(config);

     return this;

  }

  /**
   * Returns the treecache based on the cache configuration
   * @return
   */
  public TreeCache<String,String> getCache(String name){

    Cache flatCache = dcm.getCache(name);

    TreeCache<String,String> treeCache =  new TreeCacheImpl<String,String>(flatCache);

    return treeCache;

  }





}
