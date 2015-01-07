package org.opendaylight.datasand.store;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.opendaylight.datasand.store.jdbc.DataSandJDBCDriver;

public class ClientPOJOTest {
    public static void main(String args[]){
        DataSandJDBCDriver driver = new DataSandJDBCDriver();
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try{
            conn = driver.connect("128.107.142.54", null);
            st = conn.createStatement();
            String sql = "Select TestString,TestBoolean,TestLong,TestShort,TestIndex from PojoObject;";
            rs = st.executeQuery(sql);
            int count = 0;
            while(rs.next()){
                count++;
                if(count%1000==0)
                    System.out.println("Count="+count);
            }
            System.out.println("Finish="+count);
        }catch(Exception err){
            err.printStackTrace();
        }
        if(rs!=null) try{rs.close();}catch(Exception err){err.printStackTrace();}
        if(st!=null) try{st.close();}catch(Exception err){err.printStackTrace();}
        if(conn!=null) try{conn.close();}catch(Exception err){err.printStackTrace();}
    }
}
