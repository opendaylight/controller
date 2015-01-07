package org.opendaylight.datasand.store;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;
import org.opendaylight.datasand.codec.MD5Identifier;
import org.opendaylight.datasand.codec.ThreadPool;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.store.jdbc.JDBCResultSet;
import org.opendaylight.datasand.store.jdbc.JDBCServer;

public class ObjectDataStore {
    private Map<String, ShardLocation> location = new HashMap<String, ShardLocation>();
    private static int MAX_X_VECTOR = 10;
    private static int MAX_Y_VECTOR = 10;
    private static int MAX_Z_VECTOR = 10;
    private String dataLocation = null;
    private boolean shouldSortFields = false;
    private int X_VECTOR = 0;
    private int Y_VECTOR = 0;
    private int Z_VECTOR = 0;

    private int X_VECTOR_COUNT = 0;
    private int Y_VECTOR_COUNT = 0;
    private int Z_VECTOR_COUNT = 0;
    private boolean closed = false;
    private JDBCServer jdbcServer = new JDBCServer(this);
    private ThreadPool threadpool = new ThreadPool(4,"Object Store Database Threadpool", 2000);
    private TypeDescriptorsContainer typeContainer = null;

    public ObjectDataStore(String _dataLocation, boolean _shouldSortFields) {
        this.dataLocation = _dataLocation;
        this.shouldSortFields = _shouldSortFields;
        init();
        this.typeContainer = new TypeDescriptorsContainer(this.dataLocation);
    }

    public void deleteDatabase() {
        File f = new File(this.dataLocation);
        deleteDirectory(f);
    }

    public String getDataLocation(){
        return this.dataLocation;
    }

    public TypeDescriptorsContainer getTypeDescriptorsContainer(){
        return this.typeContainer;
    }

    public static void deleteDirectory(File dir) {
        File files[] = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    deleteDirectory(file);
                else
                    file.delete();
            }
        }
        dir.delete();
    }

    public void write(Object _object, int parentRecordIndex) {
        TypeDescriptor ctype = typeContainer.getTypeDescriptorByClass(typeContainer.getElementClass(_object));
        ISerializer s = ctype.getSerializer();
        String blockKey = s.getShardName(_object);
        if (blockKey == null) {
            blockKey = "Default";
        }
        ShardLocation gl = getGLocation(blockKey);
        gl.writeObject(_object, ctype.getMD5IDForObject(_object), parentRecordIndex);
    }

    public void update(Object _object, int parentRecordIndex,int recordIndex) {
        TypeDescriptor ctype = typeContainer.getTypeDescriptorByClass(typeContainer.getElementClass(_object));
        ISerializer s = ctype.getSerializer();
        String blockKey = s.getShardName(_object);
        if (blockKey == null) {
            blockKey = "Default";
        }
        ShardLocation gl = getGLocation(blockKey);
        gl.updateObject(_object,recordIndex, ctype.getMD5IDForObject(_object), parentRecordIndex);
    }

    public Object readNoChildren(Class<?> clazz, int index) {
        ShardLocation gl = getGLocation("Default");
        TypeDescriptor table = typeContainer.getTypeDescriptorByClass(clazz);
        return gl.readObjectNoChildren(table, index);
    }

    public Object deleteNoChildren(Class<?> clazz, int index) {
        ShardLocation gl = getGLocation("Default");
        TypeDescriptor table = typeContainer.getTypeDescriptorByClass(clazz);
        return gl.deleteObjectNoChildren(table, index);
    }

    public ObjectWithInfo readWithLocation(TypeDescriptor table, int index) {
        ShardLocation gl = getGLocation("Default");
        Object o = gl.readObject(table.getTypeClass(), index);
        return new ObjectWithInfo(gl,table, o,index);
    }

    public ObjectWithInfo readNoChildrenWithLocation(TypeDescriptor table, int index) {
        ShardLocation gl = getGLocation("Default");
        Object o = gl.readObjectNoChildren(table, index);
        return new ObjectWithInfo(gl,table, o,index);
    }

    public ObjectWithInfo deleteNoChildrenWithLocation(TypeDescriptor table, int index) {
        ShardLocation gl = getGLocation("Default");
        Object o = gl.deleteObjectNoChildren(table, index);
        return new ObjectWithInfo(gl,table, o,index);
    }

    public static class ObjectWithInfo {
        private ShardLocation bLocation = null;
        private Object object = null;
        private int recordIndex = -1;
        private TypeDescriptor table = null;

        public ObjectWithInfo(ShardLocation loc,TypeDescriptor tbl, Object o, int recIndex){
            this.bLocation = loc;
            this.object = o;
            this.table = tbl;
            this.recordIndex = recIndex;
        }

        public ShardLocation getbLocation() {
            return bLocation;
        }

        public Object getObject() {
            return object;
        }

        public int getRecordIndex() {
            return recordIndex;
        }

        public TypeDescriptor getTable() {
            return table;
        }

        public ObjectWithInfo getParenInfo(){
            DataPersister df = this.bLocation.getDataFile(this.table.getTypeClass());
            int index = df.getParentIndex(this.recordIndex);
            if(index!=-1){
                TypeDescriptor parentTable = this.table.getParent();
                Object parentObject = this.bLocation.readObjectNoChildren(parentTable, index);
                return new ObjectWithInfo(this.bLocation,parentTable,parentObject,index);
            }
            return null;
        }
    }

    public Object delete(Object objectKey,Class<?> clazz){
        ShardLocation gl = getGLocation("Default");
        TypeDescriptor td = typeContainer.getTypeDescriptorByObject(objectKey);
        EncodeDataContainer ba = new EncodeDataContainer(1024,typeContainer);
        td.getSerializer().encode(objectKey, ba);
        return gl.deleteObject(clazz, MD5Identifier.createX(ba.getData()));
    }

    public Object read(Object objectKey,Class<?> clazz){
        ShardLocation gl = getGLocation("Default");
        TypeDescriptor td = typeContainer.getTypeDescriptorByObject(objectKey);
        EncodeDataContainer ba = new EncodeDataContainer(1024,typeContainer);
        td.getSerializer().encode(objectKey, ba);
        return gl.readObject(clazz, MD5Identifier.createX(ba.getData()));
    }

    public Object read(Class<?> clazz, int index) {
        ShardLocation gl = getGLocation("Default");
        return gl.readObject(clazz, index);
    }

    public Object delete(Class<?> clazz, int index) {
        ShardLocation gl = getGLocation("Default");
        return gl.deleteObject(clazz, index);
    }

    public Object readAllGraphBySingleNode(Class<?> clazz,int index){
        ShardLocation gl = getGLocation("Default");
        return gl.readAllObjectFromNode(clazz, index);
    }

    private void init() {
        AttributeDescriptor.IS_SERVER_SIDE = true;
        File dbDir = new File(this.dataLocation);
        if (dbDir.exists()) {
            File xDirs[] = dbDir.listFiles();
            if (xDirs != null) {
                for (File xDir : xDirs) {
                    int xID = xDir.getName().indexOf("X-");
                    if (xID != -1) {
                        int xx = Integer.parseInt(xDir.getName().substring(
                                xID + 2));
                        if (X_VECTOR < xx)
                            X_VECTOR = xx;
                        File yDirs[] = xDir.listFiles();
                        if (yDirs != null) {
                            for (File yDir : yDirs) {
                                int yID = yDir.getName().indexOf("Y-");
                                if (yID != -1) {
                                    int yy = Integer.parseInt(yDir.getName()
                                            .substring(yID + 2));
                                    if (Y_VECTOR < yy)
                                        Y_VECTOR = yy;
                                    File zDirs[] = yDir.listFiles();
                                    if (zDirs != null) {
                                        for (File zDir : zDirs) {
                                            int zID = zDir.getName().indexOf(
                                                    "Z-");
                                            if (zID != -1) {
                                                int zz = Integer.parseInt(zDir
                                                        .getName().substring(
                                                                zID + 2));
                                                if (Z_VECTOR < zz)
                                                    Z_VECTOR = zz;
                                                File objects[] = zDir
                                                        .listFiles();
                                                if (objects != null) {
                                                    for (File obj : objects) {
                                                        ShardLocation newLoc = new ShardLocation(xx,yy,zz,ShardLocation.readBlockKey(obj.getPath()),this);
                                                        this.location.put(newLoc.getBlockKey(),newLoc);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }else{
            dbDir.mkdirs();
        }
    }

    private ShardLocation getGLocation(String blockKey) {
        synchronized (location) {
            ShardLocation loc = location.get(blockKey);
            if (loc == null) {
                Z_VECTOR_COUNT++;
                if (Z_VECTOR_COUNT > MAX_Z_VECTOR) {
                    Z_VECTOR++;
                    Z_VECTOR_COUNT = 0;
                    Y_VECTOR_COUNT++;
                }

                if (Y_VECTOR_COUNT > MAX_Y_VECTOR) {
                    Z_VECTOR = 0;
                    Y_VECTOR++;
                    Y_VECTOR_COUNT = 0;
                    X_VECTOR_COUNT++;
                }

                if (X_VECTOR_COUNT > MAX_X_VECTOR) {
                    Z_VECTOR = 0;
                    Y_VECTOR = 0;
                    X_VECTOR++;
                }
                loc = new ShardLocation(X_VECTOR, Y_VECTOR, Z_VECTOR,blockKey,this);
                location.put(blockKey, loc);
            }
            return loc;
        }
    }

    public JDBCResultSet executeSql(String sql){
        JDBCResultSet rs = new JDBCResultSet(sql);
        try{
            JDBCServer.execute(rs, this);
            return rs;
        }catch(Exception err){
            err.printStackTrace();
        }
        return null;
    }

    public void executeSql(String sql, PrintStream out, boolean toCsv) {
        JDBCResultSet rs = new JDBCResultSet(sql);
        try {
            int count = 0;
            JDBCServer.execute(rs, this);
            boolean isFirst = true;
            int loc = rs.getFieldsInQuery().size() - 1;
            int totalWidth = 0;
            if(this.shouldSortFields){
                rs.sortFieldsInQuery();
            }
            for (AttributeDescriptor c : rs.getFieldsInQuery()) {
                if (isFirst) {
                    isFirst = false;
                    if (toCsv) {
                        out.print("\"");
                    }
                }

                if (!toCsv) {
                    out.print("|");
                }

                out.print(c.getColumnName());

                if (!toCsv) {
                    int cw = c.getCharWidth();
                    int cnw = c.getColumnName().length();
                    if (cnw > cw) {
                        c.setCharWidth(cnw);
                    }
                    int gap = cw - cnw;
                    for (int i = 0; i < gap; i++) {
                        out.print(" ");
                    }
                }

                totalWidth += c.getCharWidth() + 1;

                if (loc > 0) {
                    if (toCsv) {
                        out.print("\",\"");
                    }
                }
                loc--;
            }

            if (toCsv) {
                out.println("\"");
            } else {
                totalWidth++;
                out.println("|");
                for (int i = 0; i < totalWidth; i++) {
                    out.print("-");
                }
                out.println();
            }

            while (rs.next()) {
                isFirst = true;
                loc = rs.getFieldsInQuery().size() - 1;
                for (AttributeDescriptor c : rs.getFieldsInQuery()) {
                    if (isFirst) {
                        isFirst = false;
                        if (toCsv) {
                            out.print("\"");
                        }
                    }

                    if (!toCsv) {
                        out.print("|");
                    }

                    Object sValue = rs.getObject(c.toString());
                    if (sValue == null) {
                        sValue = "";
                    }
                    if(sValue instanceof byte[]){
                        byte[] data = (byte[])sValue;
                        sValue = "[";
                        for(int i=0;i<data.length;i++){
                            sValue= sValue.toString()+data[i];
                        }
                        sValue = sValue+"]";
                    }
                    out.print(sValue);

                    int cw = c.getCharWidth();
                    int vw = sValue.toString().length();
                    int gap = cw - vw;
                    for (int i = 0; i < gap; i++) {
                        out.print(" ");
                    }

                    if (loc > 0) {
                        if (toCsv) {
                            out.print("\",\"");
                        }
                    }
                    loc--;
                }
                if (toCsv) {
                    out.println("\"");
                } else {
                    out.println("|");
                }
                count++;
            }
            out.println("Total Number Of Records=" + count);
        } catch (Exception err) {
            err.printStackTrace(out);
        }
    }

    public static class NETask implements Runnable {

        private JDBCResultSet rs = null;
        private TypeDescriptor mainTable = null;
        private ObjectDataStore db = null;

        public NETask(JDBCResultSet _rs, TypeDescriptor _main, ObjectDataStore _db) {
            this.rs = _rs;
            this.mainTable = _main;
            this.db = _db;
        }

        public void run() {
            for (int i = rs.fromIndex; i < rs.toIndex; i++) {
                ObjectWithInfo recInfo = null;
                if(rs.getCollectedDataType()==JDBCResultSet.COLLECT_TYPE_RECORDS){
                    recInfo = db.readNoChildrenWithLocation(mainTable, i);
                }else{
                    recInfo = db.readWithLocation(mainTable, i);
                }
                rs.addRecords(recInfo, true);
            }
            synchronized (rs) {
                rs.numberOfTasks--;
                if (rs.numberOfTasks == 0) {
                    rs.setFinished(true);
                    rs.notifyAll();
                }
            }
        }
    }

    public void execute(JDBCResultSet rs) {
        TypeDescriptor table = rs.getMainTable();
        NETask task = new NETask(rs, table, this);
        rs.numberOfTasks = 1;
        threadpool.addTask(task);
    }

    public void commit() {
        for (ShardLocation bl : this.location.values()) {
            bl.save();
        }
    }

    public void close() {
        this.commit();
        this.closed = true;
        for (ShardLocation bl : this.location.values()) {
            bl.close();
        }
        this.jdbcServer.close();
    }

    public boolean isClosed() {
        return this.closed;
    }
}
