package Others;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by guoxj on 2017/6/20.
 */
public class TranCount {
    public static void main(String[] args){
        String sUrl = "jdbc:oracle:thin:@10.4.145.65:1521:esbdb";
        String sUser = "test";
        String sPasswd = "test";
        Connection conn = GetOraConn(sUrl, sUser, sPasswd);


    }

    //获取数据库连接句柄
    public static Connection GetOraConn(String sUrl, String sUser, String sPasswd) {
        String DRV = "oracle.jdbc.driver.OracleDriver";
        Connection conn = null;
        try {
            Class.forName(DRV); //加载oracle驱动程序
            conn = DriverManager.getConnection(sUrl, sUser, sPasswd);
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
}
