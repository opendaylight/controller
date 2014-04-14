package org.opendaylight.controller.datastore.ispn;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.datastore.infinispan.TreeCacheManagerSingleton;

import javax.transaction.Transaction;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author: syedbahm
 * Date: 4/2/14
 */
public class TreeManagerTest {
  String nodeName = "level";
  String []reportees = {"President","CEO","SVP","VP","Senior Director","Director","Senior Manage","Manager2","Manager1","IC"} ;
  Fqn fqn =null;
  private static TreeCacheManager tsm = TreeCacheManagerSingleton.get();



  @org.junit.Before
  public void setUp() throws Exception {
    fqn= Fqn.fromString("/");


  }

  @org.junit.After
  public void tearDown() throws Exception {

  }

  @Test
  public void addingNineLevelTreeTest() throws Exception{

    TreeCache tc = get9LevelTreeCache("add");


    //ok let us try getting the IC
    Fqn getFqn = Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7/level8/level9");
    Map<String,String> dataValue =  tc.getData(getFqn);

    Assert.assertEquals(dataValue.get("level9"),"IC");

  }

  public TreeCache get9LevelTreeCache(String cacheName) throws Exception {



    TreeCache<String,String> tc = tsm.getCache(cacheName);

    tc.start();

    //put 9 level tre
    tc.getCache().getAdvancedCache().getTransactionManager().begin();
    Fqn newFqn = fqn;
    for(int i = 1; i < 10; i++){
      newFqn = Fqn.fromRelativeFqn(fqn,Fqn.fromString(nodeName+i));
      tc.put(newFqn,nodeName+i,reportees[i]);
      fqn=newFqn;
    }
    tc.getCache().getAdvancedCache().getTransactionManager().commit();

    return tc;
  }


  public TreeCache modifyTreeCache(int  levelIndex, String key, String value)throws Exception{
      TreeCache tc = get9LevelTreeCache("modify");
    Fqn fqnToModify = Fqn.fromString("/");

    for(int i = 1 ; i <=levelIndex; ++i ){
      fqnToModify = fqnToModify.fromRelativeFqn(fqnToModify, Fqn.fromString("level"+i));
    }


    tc.getCache().getAdvancedCache().getTransactionManager().begin();
    tc.put(fqnToModify,key,value);
    tc.getCache().getAdvancedCache().getTransactionManager().commit();

    return tc;

  }
  @Test
  public void modifyTreeCacheTest()throws Exception {
    TreeCache tc =  modifyTreeCache(9,"SoftwareEngineer","SE");

    Map<String,String>data = tc.getData(Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7/level8/level9"));
    Assert.assertEquals(data.get("SoftwareEngineer"), "SE");

  }

  @Test
  public void repeateableReadsWhileModifyTest () throws Exception {
    final TreeCache tc = get9LevelTreeCache("repeatableReadWhileModify");
    final TreeCache tcOriginal = tc;

    //This thread is the main thread that does the repeatable reads

    //ExecutorThread is the one that will be modifying the level2/ level3/ and keep on changing the value every 5 seconds.

    ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
    es.scheduleAtFixedRate(new Runnable(){
      @Override
      public void run() {
        try{
          tc.getCache().getAdvancedCache().getTransactionManager().begin();
          tc.put(Fqn.fromString("/level1/level2/level3"),"keystone","milestone");
          tc.put(Fqn.fromString("/level1/level2/"),"milestone","keystone");
          tc.getCache().getAdvancedCache().getTransactionManager().commit();
        }catch(Exception e){
             e.printStackTrace();
        }
      }
    },100,200, TimeUnit.MILLISECONDS);

    tcOriginal.getCache().getAdvancedCache().getTransactionManager().begin();
    Map<String,String>data;
    for(int i =0; i < 10; i++) {
     data = tcOriginal.getData(Fqn.fromString("/level1/level2/level3"));
     Assert.assertEquals(data.get("level3"),"VP");
      data = tcOriginal.getData(Fqn.fromString("/level1/level2"));
      Assert.assertEquals(data.get("level2"),"SVP");
      Thread.sleep(100);

    }
    tcOriginal.getCache().getAdvancedCache().getTransactionManager().commit();
    //now getting after the transasctions have committed
    TreeCache tcNew = tsm.getCache("repeatableReadWhileModify");

    data = tcOriginal.getData(Fqn.fromString("/level1/level2/level3"));
    Assert.assertEquals(data.get("keystone"),"milestone");

    data = tcOriginal.getData(Fqn.fromString("/level1/level2/"));
    Assert.assertEquals(data.get("milestone"),"keystone");

    es.shutdownNow();

  }



  @Test
  public void repeateableReadsWhileAddTest () throws Exception {
    final TreeCache tc = get9LevelTreeCache("repeatableReadsWhileAdd");
    final TreeCache tcOriginal = tc;

    //This thread is the main thread that does the repeatable reads

    //ExecutorThread is the one that will be modifying the level2/ level3/ and keep on changing the value every 5 seconds.

    ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
    es.scheduleAtFixedRate(new Runnable(){
      @Override
      public void run() {
        try{
          tc.getCache().getAdvancedCache().getTransactionManager().begin();
          tc.put(Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7/level8/level9/level10"),"level10","Junior Engineer");
          tc.getCache().getAdvancedCache().getTransactionManager().commit();
        }catch(Exception e){
          e.printStackTrace();
        }
      }
    },1,200, TimeUnit.MILLISECONDS);
    //so that the executor starts and adds a new node
    Thread.sleep(10);
    tcOriginal.getCache().getAdvancedCache().getTransactionManager().begin();
    Map<String,String>data;
    for(int i =0; i < 5; i++) {
      data = tcOriginal.getData(Fqn.fromString("/level1/level2/level3"));
      Assert.assertEquals(data.get("level3"),"VP");
      data = tcOriginal.getData(Fqn.fromString("/level1/level2"));
      Assert.assertEquals(data.get("level2"),"SVP");
      data = tcOriginal.getData(Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7/level8/level9/level10"));
      Assert.assertEquals(data.get("level10"),"Junior Engineer");

      Thread.sleep(100);

    }
    tcOriginal.getCache().getAdvancedCache().getTransactionManager().commit();
    //now getting after the transasctions have committed
    TreeCache tcNew = tsm.getCache("repeatableReadsWhileAdd");

    data = tcNew.getData(Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7/level8/level9/level10"));
    Assert.assertEquals(data.get("level10"),"Junior Engineer");

    es.shutdownNow();


  }

  @Test
  public void repeateableReadsWhileDeleteTest () throws Exception {
    final TreeCache tc = get9LevelTreeCache("repeatableReadsWhileDelete");
    final TreeCache tcOriginal = tc;
    final ArrayList<String> removedFQN = new ArrayList<String>();



    //This thread is the main thread that does the repeatable reads

    //ExecutorThread is the one that will be  5 seconds.

    ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
    es.scheduleAtFixedRate(new Runnable(){
      @Override
      public void run() {

        try{
          Random random = new Random();
          int i = random.nextInt(10);
          //System.out.println(i);
          if(i > 5){

          //let us form the Fqn string
            StringBuffer fqn = new StringBuffer();
            for(int j=1; j <=i; j++ ){
              fqn.append("/").append("level"+j);
            }
          tc.getCache().getAdvancedCache().getTransactionManager().begin();

          tc.removeNode(Fqn.fromString(fqn.toString()));
          tc.getCache().getAdvancedCache().getTransactionManager().commit();
          Assert.assertNull(tc.getNode(Fqn.fromString(fqn.toString())));
            removedFQN.add(fqn.toString());
          }
        }catch(Exception e){
          e.printStackTrace();
        }
      }
    },10,100, TimeUnit.MILLISECONDS);

    tcOriginal.getCache().getAdvancedCache().getTransactionManager().begin();
    Map<String,String>data;
    for(int i =0; i < 10; i++) {
      data = tcOriginal.getData(Fqn.fromString("/level1/level2/level3/level4/level5/level6"));
      Assert.assertEquals(data.get("level6"),this.reportees[6]);

      data = tcOriginal.getData(Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7"));
      Assert.assertEquals(data.get("level7"),this.reportees[7]);

      data = tcOriginal.getData(Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7/level8"));
      Assert.assertEquals(data.get("level8"),this.reportees[8]);

      data = tcOriginal.getData(Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7/level8/level9"));
      Assert.assertEquals(data.get("level9"),this.reportees[9]);


      Thread.sleep(100);

    }
    tcOriginal.getCache().getAdvancedCache().getTransactionManager().commit();
    //so that the other thread comes back
    Thread.sleep(1000);
    //now getting after the transasctions have committed
    TreeCache tcNew = tsm.getCache("repeatableReadsWhileDelete");
    for(String fqn:removedFQN) {
    data = tcNew.getData(Fqn.fromString(fqn));
    Assert.assertNull(data);
    }
    es.shutdownNow();


  }

  //test to check the suspend and resume transaction functionality
  @Test
  public void testResumeSuspendTransaction () throws Exception{
    final TreeCache tc = get9LevelTreeCache("resumeSuspendTransaction");

    //ok here we will start a transaction and then resume the same in another thread and commit
    //This thread is the main thread that does the adding of the node within transaction but doesn't commit
    //ExecutorThread is the one that will be  commit

    tc.getCache().getAdvancedCache().getTransactionManager().begin();
    tc.put(Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7/level8/level9/level10/level11"),"level11","Intern");

    final Transaction transaction = tc.getCache().getAdvancedCache().getTransactionManager().suspend();

    final TreeCache tcOriginal = tc;



    ExecutorService es = Executors.newSingleThreadExecutor();
    es.execute(new Runnable() {
      @Override
      public void run() {

        try {
          tcOriginal.getCache().getAdvancedCache().getTransactionManager().resume(transaction);
          tcOriginal.getCache().getAdvancedCache().getTransactionManager().commit();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    //giving some time for the other thread to commit the transaction
    Thread.sleep(1000);

    Map<String,String>data;
    data = tcOriginal.getData(Fqn.fromString("/level1/level2/level3/level4/level5/level6/level7/level8/level9/level10/level11"));
    Assert.assertEquals(data.get("level11"),"Intern");
    es.shutdownNow();


  }

  @BeforeClass
  public static void start() throws Exception {


  }
}
