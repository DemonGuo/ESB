package Client;

import java.io.*;
import java.sql.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static Client.VersionMananger.GetRootPaths;

/**
 * Created by guoxj on 2017/6/1.
 */
public class DbProcess {
    //获取oracle配置
    public static Map<String, String> GetOraConfig(String sIP) {
        // 0-user 1-passwd 2-url
        Map<String, String> mOraConfig = new HashMap<String, String>();
        LocalConfig config = new LocalConfig();
        String sUser = config.GetItemConfig("orauser_" + sIP).split("/")[0];
        String sPasswd = config.GetItemConfig("orauser_" + sIP).split("/")[1];
        String sUrl = config.GetItemConfig("oraurl_" + sIP);
        mOraConfig.put("user", sUser);
        mOraConfig.put("passwd", sPasswd);
        mOraConfig.put("url", sUrl);

        return mOraConfig;
    }


    //获取数据库连接句柄
    public static Connection GetOraConn(String sIP) {
        String DRV = "oracle.jdbc.driver.OracleDriver";
        Map<String, String> mOraConfig = GetOraConfig(sIP);
        Connection conn = null;
        try {
            Class.forName(DRV); //加载oracle驱动程序
            conn = DriverManager.getConnection(mOraConfig.get("url"), mOraConfig.get("user"), mOraConfig.get("passwd"));
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return conn;
    }


    //关闭数据库连接句柄
    public static void CloseConn(Connection conn) throws SQLException {
        if(null != conn) {
            conn.close();
            conn = null;
        }
    }


    //写单个服务的insert select delete sql
    public static void WriteAll(OutputStreamWriter[] outFile, String sSlt, String sInst, String sDel) {
        try {
            OutputStreamWriter fosSlt = outFile[0];
            OutputStreamWriter fosInst = outFile[1];
            OutputStreamWriter fosDel = outFile[2];
            fosSlt.write(sSlt);
            fosInst.write(sInst);
            fosDel.write(sDel);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    //刷新缓存的insert select delete sql
    public static void FlushAll(OutputStreamWriter[] outFile) {
        try {
            for(OutputStreamWriter fos : outFile) {
                fos.flush();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    // 将oracle.sql.clob 类型转换为字符串
    public static String ClobToString(Clob clob) throws SQLException, IOException {
        Reader is = clob.getCharacterStream();
        BufferedReader br = new BufferedReader(is);
        String line = br.readLine();
        StringBuffer sb = new StringBuffer();
        while(line != null) {
            sb.append(line);
            line = br.readLine();
        }
        br.close();
        return sb.toString();
    }


    //获取 PROTOCOLBIND 表
    public static void GetTable_PROTOCOLBIND(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                             String sPROTOCOLID) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.PROTOCOLBIND t WHERE t.PROTOCOLID like ?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sPROTOCOLID + "%");

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.PROTOCOLBIND t WHERE t.PROTOCOLID=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("PROTOCOLID");
            preNew.setString(1, sId);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.PROTOCOLBIND t " +
                        "WHERE t.PROTOCOLID='%s';\n", sId);

                //insert sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.PROTOCOLBIND " +
                                "(PROTOCOLID, BINDTYPE, BINDURI) \n VALUES('%s', '%s', '%s');\n",
                        resultOld.getString("PROTOCOLID"), resultOld.getString("BINDTYPE"),
                        resultOld.getString("BINDURI"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.PROTOCOLBIND t " +
                        "WHERE t.PROTOCOLID='%s';\n", sId);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[ DstEnv ] not Exist : " + sId);
            }
        }
    }


    //获取 BINDMAP 表
    public static void GetTable_BINDMAP(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                        String sSERVICEID) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.BINDMAP t WHERE t.SERVICEID=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sSERVICEID);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.BINDMAP t WHERE t.SERVICEID=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("SERVICEID");
            preNew.setString(1, sId);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.BINDMAP t " +
                        "WHERE t.SERVICEID='%s';\n", sId);

                //insert sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.BINDMAP " +
                                "(SERVICEID, STYPE, LOCATION, VERSION, PROTOCOLID, MAPTYPE)\n" +
                                "VALUES('%s', '%s', '%s', '%s', '%s', '%s');\n\n",
                        resultOld.getString("SERVICEID"), resultOld.getString("STYPE"),
                        resultOld.getString("LOCATION"), resultOld.getString("VERSION"),
                        resultOld.getString("PROTOCOLID"), resultOld.getString("MAPTYPE"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.BINDMAP t " +
                        "WHERE t.SERVICEID='%s';\n", sId);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[ DstEnv BINDMAP ] not Exist : " + sId);
            }
        }

    }


    //获取 DATAADAPTER 表
    public static void GetTable_DATAADAPTER(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                            String sDATAADAPTERID) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.DATAADAPTER t WHERE t.DATAADAPTERID=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sDATAADAPTERID);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.DATAADAPTER t WHERE t.DATAADAPTERID=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("DATAADAPTERID");
            preNew.setString(1, sId);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.DATAADAPTER t " +
                        "WHERE t.DATAADAPTERID='%s';\n", sId);

                //insert sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.DATAADAPTER " +
                                "(DATAADAPTERID, DATAADAPTER, LOCATION, ADAPTERTYPE)\n" +
                                "VALUES('%s', '%s', '%s', '%s');\n\n",
                        resultOld.getString("DATAADAPTERID"), resultOld.getString("DATAADAPTER"),
                        resultOld.getString("LOCATION"), resultOld.getString("ADAPTERTYPE"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.DATAADAPTER t " +
                        "WHERE t.DATAADAPTERID='%s';\n", sId);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[ DstEnv DATAADAPTER ] not Exist : " + sId);
            }
        }

    }


    //获取 SERVICEINFO 表
    public static void GetTable_SERVICEINFO(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                            String sSERVICEID) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.SERVICEINFO t WHERE t.SERVICEID=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sSERVICEID);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.SERVICEINFO t WHERE t.SERVICEID=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("SERVICEID");
            preNew.setString(1, sId);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.SERVICEINFO t " +
                        "WHERE t.SERVICEID='%s';\n", sId);

                //insert sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.SERVICEINFO\n" +
                                "(SERVICEID, SERVICETYPE, CONTRIBUTION, PREPARED, GROUPNAME, " +
                                "LOCATION, DESCRIPTION, ADAPTERTYPE, ISCREATE)\n" +
                                "VALUES('%s', '%s', '%s', '%s', NULL, '%s', '%s', NULL, '%s');\n\n",
                        resultOld.getString("SERVICEID"), resultOld.getString("SERVICETYPE"),
                        resultOld.getString("CONTRIBUTION"), resultOld.getString("PREPARED"),
                        resultOld.getString("LOCATION"), resultOld.getString("DESCRIPTION"),
                        resultOld.getString("ISCREATE"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.SERVICEINFO t " +
                        "WHERE t.SERVICEID='%s';\n", sId);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[ DstEnv SERVICEINFO ] not Exist : " + sId);
            }
        }
    }


    //获取 SERVICES 表
    public static void GetTable_SERVICES(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                         String sNAME) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.SERVICES t WHERE t.NAME=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sNAME);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.SERVICES t WHERE t.NAME=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("NAME");
            preNew.setString(1, sId);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.SERVICES t " +
                        "WHERE t.NAME='%s';\n", sId);

                //insert sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.SERVICES " +
                                "(NAME, INADDRESSID, OUTADDRESSID, \"TYPE\", SESSIONCOUNT, " +
                                "DELIVERYMODE, NODEID, LOCATION)\n" +
                                "VALUES('%s', '%s', '%s', '%s', %d, '%s', '%s', '%s');\n\n",
                        resultOld.getString("NAME"), resultOld.getString("INADDRESSID"),
                        resultOld.getString("OUTADDRESSID"), resultOld.getString("TYPE"),
                        resultOld.getInt("SESSIONCOUNT"), resultOld.getString("DELIVERYMODE"),
                        resultOld.getString("NODEID"), resultOld.getString("LOCATION"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.SERVICES t " +
                        "WHERE t.NAME='%s';\n", sId);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[ DstEnv SERVICES ] not Exist : " + sId);
            }
        }
    }

    //获取 SERVICESYSTEM  表
    public static void GetTable_SERVICESYSTEM(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                              String sNAME) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.SERVICESYSTEM t WHERE t.NAME=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sNAME);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.SERVICESYSTEM t WHERE t.NAME=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("NAME");
            preNew.setString(1, sId);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.SERVICESYSTEM t " +
                        "WHERE t.NAME='%s';\n", sId);

                //insert sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.SERVICESYSTEM(NAME, DESCRIPTION)\n" +
                                "VALUES('%s', '%s');\n",
                        resultOld.getString("NAME"), resultOld.getString("DESCRIPTION"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.SERVICESYSTEM t " +
                        "WHERE t.NAME='%s';\n", sId);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[DstEnv SERVICESYSTEM ] not Exist : " + sId);
            }
        }
    }


    //获取 BUSSSERVICES 表
    public static void GetTable_BUSSSERVICES(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                             String sSERVICEID) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.BUSSSERVICES t WHERE t.SERVICEID=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sSERVICEID);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.BUSSSERVICES t WHERE t.SERVICEID=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("SERVICEID");
            preNew.setString(1, sId);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.BUSSSERVICES t " +
                        "WHERE t.SERVICEID='%s';\n", sId);

                //select sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.BUSSSERVICES " +
                                "(SERVICEID, CATEGORY, METHODNAME, ISARG, DESCRIPTION)\n" +
                                "VALUES('%s', '%s', '%s', '%s', '%s');\n\n",
                        resultOld.getString("SERVICEID"), resultOld.getInt("CATEGORY"),
                        resultOld.getInt("METHODNAME"), resultOld.getString("ISARG"),
                        resultOld.getInt("DESCRIPTION"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.BUSSSERVICES t " +
                        "WHERE t.SERVICEID='%s';\n", sId);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[DstEnv BUSSSERVICES ] not Exist : " + sId);
            }
        }
    }

    //获取 SERVICESYSTEMMAP 表
    public static void GetTable_SERVICESYSTEMMAP(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                                 String sSERVICEID, String sNAME) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.SERVICESYSTEMMAP t WHERE t.SERVICEID=? and t.NAME=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sSERVICEID);
        preOld.setString(2, sNAME);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.SERVICESYSTEMMAP t WHERE t.SERVICEID=? and t.NAME=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("SERVICEID");
            String sName = resultOld.getString("NAME");

            preNew.setString(1, sId);
            preNew.setString(2, sName);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.SERVICESYSTEMMAP t " +
                        "WHERE t.SERVICEID='%s' and t.NAME='%s';\n", sId, sName);

                //select sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.SERVICESYSTEMMAP " +
                                "(NAME, SERVICEID)\n" +
                                "VALUES('%s', '%s');\n\n",
                        resultOld.getString("NAME"), resultOld.getString("SERVICEID"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.SERVICESYSTEMMAP t " +
                        "WHERE t.SERVICEID='%s' and t.NAME='%s';\n", sId, sName);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[DstEnv SERVICESYSTEMMAP ] not Exist : " + sId);
            }
        }
    }

    //获取 DEPLOYMENTS 表
    public static void GetTable_DEPLOYMENTS(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                            String sNAME) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.DEPLOYMENTS t WHERE t.NAME=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sNAME);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.DEPLOYMENTS t WHERE t.NAME=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("NAME");
            preNew.setString(1, sId);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.DEPLOYMENTS t " +
                        "WHERE t.NAME='%s';\n", sId);

                String outInsertSql = String.format("INSERT INTO ESBDATA.DEPLOYMENTS\n" +
                                "(ID, LOCATION, FILEPATH, DEPLOYDATE, DESCRIPTION, NAME, " +
                                "FILECONTENT, USERNAME, VERSION)\n" +
                                "VALUES('%s', '%s', '%s', sysdate, '%s', '%s', NULL, '%s', '%s');\n\n",
                        resultOld.getString("ID"), resultOld.getString("LOCATION"),
                        resultOld.getString("FILEPATH"),
                        resultOld.getString("DESCRIPTION"), resultOld.getString("NAME"),
                        resultOld.getString("USERNAME"), resultOld.getString("VERSION"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.DEPLOYMENTS t " +
                        "WHERE t.NAME='%s';\n", sId);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[DstEnv DEPLOYMENTS ] not Exist : " + sId);
            }
        }
    }


    //获取 PROXYSERVICES 表
    public static void GetTable_PROXYSERVICES(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                              String sSERVICEID) throws SQLException, IOException {
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.PROXYSERVICES t WHERE t.SERVICEID=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sSERVICEID);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.PROXYSERVICES t WHERE t.SERVICEID=?";
        preNew = conDst.prepareStatement(sltNew);


        LocalConfig config = new LocalConfig();
        String sVerTemp = config.GetItemConfig("ver_temp");
        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sId = resultOld.getString("SERVICEID");
            preNew.setString(1, sId);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                String str_xpdl = ClobToString(resultOld.getClob("XPDL"));
                String str_cont = ClobToString(resultOld.getClob("CONTENTS"));

                resultNew = preNew.executeQuery();
                if(!resultNew.next()) {
                    //select sql
                    String selectSql = String.format("SELECT * FROM esbdata.PROXYSERVICES t " +
                            "WHERE t.SERVICEID='%s';\n", sId);

                    //insert sql
                    String sInsertSql = String.format("declare \n" +
                                    "  v_serviceid PROXYSERVICES.serviceid%%type;\n" +
                                    "  v_subservices PROXYSERVICES.subservices%%type;\n" +
                                    "  v_xpdl PROXYSERVICES.XPDL%%type;\n" +
                                    "  v_content PROXYSERVICES.contents%%type;\n" +
                                    "  v_proxytype PROXYSERVICES.proxytype%%type;\n" +
                                    "begin\n" +
                                    "  v_serviceid:='%s';\n" +
                                    "  v_subservices:='%s';\n" +
                                    "  v_xpdl:='%s';\n" +
                                    "  v_content:='%s';\n" +
                                    "  v_proxytype:='%s';\n" +
                                    "dbms_output.put_line('开始插入代理服务['||v_serviceid||']数据');\n" +
                                    "P_ADD_ProxyService(v_serviceid,v_subservices,v_xpdl,v_content,v_proxytype);\n" +
                                    "commit;\n" +
                                    "dbms_output.put_line('插入代理服务['||v_serviceid||']数据结束');\n" +
                                    "end;\n\n\n",
                            resultOld.getString("SERVICEID"), resultOld.getString("SUBSERVICES"),
                            str_xpdl, str_cont, resultOld.getString("PROXYTYPE"));
                    sInsertSql = sInsertSql.replace("\'null\'", "NULL");
                    String sServiceFile = String.format("%s/esbdb/%s.sql", VersionMananger.sRootPath.get(3), sId);
                    FileOutputStream fosService = new FileOutputStream(new File(sServiceFile));
                    OutputStreamWriter oswService = new OutputStreamWriter(fosService, "UTF-8");
                    oswService.write(sInsertSql);
                    oswService.close();

                    String outInsertSql = String.format("--组合服务[%s], 脚本单独文件生成.", sId);

                    //recover sql
                    String deleteSql = String.format("DELETE FROM esbdata.PROXYSERVICES t " +
                            "WHERE t.SERVICEID='%s';\n", sId);

                    WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                    System.out.println("[DstEnv DEPLOYMENTS ] not Exist : " + sId);
                }
            }
        }
    }


    //获取 ESB_DATA_DICT 表 （按系统比较）
    public static void GetTable_DICT(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile,
                                     String sDICT_NAME) throws SQLException, IOException {
        String sDes = "----ESB_DATA_DICT\n";
        WriteAll(outFile, sDes, sDes, sDes);
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.ESB_DATA_DICT t WHERE t.DICT_NAME=?";
        preOld = conSrc.prepareStatement(sltOld);
        preOld.setString(1, sDICT_NAME);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.ESB_DATA_DICT t WHERE t.DICT_NAME=? and t.ITEM_NAME=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String dictName = resultOld.getString("DICT_NAME");
            String itemName = resultOld.getString("ITEM_NAME");
            preNew.setString(1, dictName);
            preNew.setString(2, itemName);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.ESB_DATA_DICT t " +
                        "WHERE t.DICT_NAME='%s' AND t.ITEM_NAME='%s';\n", dictName, itemName);

                //insert sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.ESB_DATA_DICT " +
                                "(DICT_NAME, ITEM_NAME, ITEM_CNAME, ITEM_TYPE, " +
                                "ITEM_LENGTH, ITEM_SCALE, IS_PIN, CHG_TM)\n" +
                                "VALUES('%s', '%s', '%s', '%s', %d, %d, %d, NULL);\n\n",
                        resultOld.getString("DICT_NAME"), resultOld.getString("ITEM_NAME"),
                        resultOld.getString("ITEM_CNAME"), resultOld.getString("ITEM_TYPE"),
                        resultOld.getInt("ITEM_LENGTH"), resultOld.getInt("ITEM_SCALE"), resultOld.getInt("IS_PIN"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");


                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.ESB_DATA_DICT t " +
                        "WHERE t.DICT_NAME='%s' AND t.ITEM_NAME='%s';\n", dictName, itemName);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[DstEnv ESB_DATA_DICT ]not Exist : " + dictName + ", " + itemName);
            }
        }
    }


    //获取 BINDTYPEDEFINE 表 （全表比较）
    public static void GetTable_BINDTYPEDEFINE(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile)
            throws SQLException, IOException {
        String sDes = "----BINDTYPEDEFINE\n";
        WriteAll(outFile, sDes, sDes, sDes);
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.BINDTYPEDEFINE";
        preOld = conSrc.prepareStatement(sltOld);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.BINDTYPEDEFINE t WHERE t.TYPENAME=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sTypeName = resultOld.getString("TYPENAME");
            preNew.setString(1, sTypeName);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.BINDTYPEDEFINE t " +
                        "WHERE t.TYPENAME='%s';\n", sTypeName);

                //insert sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.BINDTYPEDEFINE " +
                                "(TYPENAME, CONTAINERTYPE, VMPATH, CONFIGIMPL, PARSERIMPL, INVOKERIMPL)\n" +
                                "VALUES('%s', %d, '%s', '%s', '%s', '%s');\n\n",
                        resultOld.getString("TYPENAME"), resultOld.getInt("CONTAINERTYPE"),
                        resultOld.getString("VMPATH"), resultOld.getInt("CONFIGIMPL"),
                        resultOld.getString("PARSERIMPL"), resultOld.getInt("INVOKERIMPL"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.BINDTYPEDEFINE t " +
                        "WHERE t.TYPENAME='%s';\n", sTypeName);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.println("[DstEnv BINDTYPEDEFINE] not Exist : " + sTypeName);
            }
        }
    }


    //获取 ESB_CODE_CONV 表 （全表比较）
    public static void GetTable_ESB_CODE_CONV(Connection conSrc, Connection conDst, OutputStreamWriter[] outFile)
            throws SQLException, IOException {
        String sDes = "----ESB_CODE_CONV\n";
        WriteAll(outFile, sDes, sDes, sDes);
        PreparedStatement preOld = null; //预编译语句对象
        ResultSet resultOld = null; //结果集
        String sltOld = "SELECT * FROM esbdata.ESB_CODE_CONV";
        preOld = conSrc.prepareStatement(sltOld);

        PreparedStatement preNew = null; //预编译语句对象
        ResultSet resultNew = null; //结果集
        String sltNew = "SELECT * FROM esbdata.ESB_CODE_CONV t WHERE t.SYS_ID=? and t.CODE_ID=? and t.SRC_VALUE=? and t.DEST_VALUE=?";
        preNew = conDst.prepareStatement(sltNew);

        resultOld = preOld.executeQuery();
        while(resultOld.next()) {
            String sSysId = resultOld.getString("SYS_ID");
            String sCodeId = resultOld.getString("CODE_ID");
            String sSrcValue = resultOld.getString("SRC_VALUE");
            String sDestValue = resultOld.getString("DEST_VALUE");
            preNew.setString(1, sSysId);
            preNew.setString(2, sCodeId);
            preNew.setString(3, sSrcValue);
            preNew.setString(4, sDestValue);
            resultNew = preNew.executeQuery();
            if(!resultNew.next()) {
                //select sql
                String selectSql = String.format("SELECT * FROM esbdata.ESB_CODE_CONV t WHERE t.SYS_ID='%s' and " +
                                "t.CODE_ID='%s' and t.SRC_VALUE='%s' and t.DEST_VALUE='%s';\n",
                        sSysId, sCodeId, sSrcValue, sDestValue);

                //insert sql
                String outInsertSql = String.format("INSERT INTO ESBDATA.ESB_CODE_CONV " +
                                "(SYS_ID, CODE_ID, SRC_VALUE, DEST_VALUE, REMARK)\n" +
                                "VALUES('%s', '%s', '%s', '%s', '%s');\n\n",
                        resultOld.getString("SYS_ID"), resultOld.getString("CODE_ID"),
                        resultOld.getString("SRC_VALUE"), resultOld.getString("DEST_VALUE"),
                        resultOld.getString("REMARK"));
                outInsertSql = outInsertSql.replace("\'null\'", "NULL");

                //recover sql
                String deleteSql = String.format("DELETE FROM esbdata.ESB_CODE_CONV t WHERE t.SYS_ID='%s' and " +
                                "t.CODE_ID='%s' and t.SRC_VALUE='%s' and t.DEST_VALUE='%s';\n",
                        sSysId, sCodeId, sSrcValue, sDestValue);

                WriteAll(outFile, selectSql, outInsertSql, deleteSql);
                System.out.printf("[DstEnv ESB_CODE_CONV] not Exist : %s %s %s %s\n", sSysId, sCodeId, sSrcValue, sDestValue);
            }
        }
    }


    //获取渠道端的数据库配置
    public static void GetChannelSql(Connection[] connAll, Map<String, ArrayList> mIn,
                                     OutputStreamWriter[] outFile) throws IOException, SQLException {
        System.out.println("获取渠道端的数据库配置 GetChannelSql");
        Connection conSrc = connAll[0];
        Connection conDst = connAll[1];
        for(String sConsumer : mIn.keySet()) {
            String sDes = String.format("----渠道[%s] Sql\n", sConsumer);
            WriteAll(outFile, sDes, sDes, sDes);
            System.out.printf(sDes);

            GetTable_PROTOCOLBIND(conSrc, conDst, outFile, sConsumer);
            GetTable_BINDMAP(conSrc, conDst, outFile, sConsumer);
            GetTable_DATAADAPTER(conSrc, conDst, outFile, sConsumer);
            GetTable_SERVICEINFO(conSrc, conDst, outFile, sConsumer);
            GetTable_SERVICES(conSrc, conDst, outFile, sConsumer);
        }
        FlushAll(outFile);
    }

    //获取服务端的数据库配置
    public static void GetProviderSql(Connection[] connAll, Map<String, ArrayList> mOut,
                                      OutputStreamWriter[] outFile) throws IOException, SQLException {
        System.out.println("获取服务端的数据库配置 GetProviderSql");
        Connection conSrc = connAll[0];
        Connection conDst = connAll[1];
        for(String sProvider : mOut.keySet()) {
            String sDes = String.format("----服务系统[%s] Sql\n", sProvider);
            WriteAll(outFile, sDes, sDes, sDes);
            System.out.printf(sDes);

            GetTable_PROTOCOLBIND(conSrc, conDst, outFile, sProvider);
            GetTable_SERVICESYSTEM(conSrc, conDst, outFile, sProvider);
        }
        FlushAll(outFile);
    }


    //获取服务的数据库配置
    public static void GetServiceSql(Connection[] connAll, Map<String, ArrayList> mOut,
                                     OutputStreamWriter[] outFile) throws IOException, SQLException {
        System.out.println("获取服务端的数据库配置 GetServiceSql");
        Connection conSrc = connAll[0];
        Connection conDst = connAll[1];
        String sDes = "";
        String sProvider = "";
        for(Map.Entry<String, ArrayList> one : mOut.entrySet()) {
            sProvider = one.getKey();
            ArrayList<String> alProvider = one.getValue();
            if(alProvider.size() < 1) {
                System.out.printf("服务系统[%s]不存在任何服务.\n", sProvider);
                continue;
            }
            sDes = String.format("\n\n----服务系统[%s]----", sProvider);
            WriteAll(outFile, sDes, sDes, sDes);

            for(String sSERVICEID : alProvider) {
                sDes = String.format("\n----服务[%s] Sql\n", sSERVICEID);
                WriteAll(outFile, sDes, sDes, sDes);

                System.out.printf(sDes);
                GetTable_DEPLOYMENTS(conSrc, conDst, outFile, sSERVICEID);
                GetTable_SERVICES(conSrc, conDst, outFile, sSERVICEID);
                GetTable_SERVICEINFO(conSrc, conDst, outFile, sSERVICEID);
                GetTable_DATAADAPTER(conSrc, conDst, outFile, sSERVICEID);
                GetTable_BUSSSERVICES(conSrc, conDst, outFile, sSERVICEID);
                GetTable_BINDMAP(conSrc, conDst, outFile, sSERVICEID);
                GetTable_SERVICESYSTEMMAP(conSrc, conDst, outFile, sSERVICEID, sProvider);
                if('6' == sSERVICEID.charAt(4)) {
                    GetTable_PROXYSERVICES(conSrc, conDst, outFile, sSERVICEID);
                }
            }
        }
        FlushAll(outFile);
    }

    //获取全表比较的sql
    public static void GetFullCompareSql(Connection[] connAll, Map<String, ArrayList> mIn, Map<String, ArrayList> mOut, OutputStreamWriter[] outFile) throws IOException, SQLException {
        System.out.println("获取全表比较的sql GetFullCompareSql");
        Connection conSrc = connAll[0];
        Connection conDst = connAll[1];
        System.out.println("----比较 ESB_DATA_DICT");
        for(String sConsumer : mIn.keySet()) {
            GetTable_DICT(conSrc, conDst, outFile, sConsumer);
        }
        for(String sProvider : mOut.keySet()) {
            GetTable_DICT(conSrc, conDst, outFile, sProvider);
        }
        GetTable_DICT(conSrc, conDst, outFile, "esb");

        System.out.println("----比较 BINDTYPEDEFINE");
        GetTable_BINDTYPEDEFINE(conSrc, conDst, outFile);

        System.out.println("----比较 ESB_CODE_CONV");
        GetTable_ESB_CODE_CONV(conSrc, conDst, outFile);
    }

    //获取数据库版本
    public static void GetSqlVer(String sSrcIP, String sDstIP, ArrayList<Map<String, ArrayList>> alAll) {
        //获取版本清单内容 0-in 1-out
        Connection[] connAll = {null, null};
        connAll[0] = GetOraConn(sSrcIP);
        connAll[1] = GetOraConn(sDstIP);
        Map<String, ArrayList> mIn = alAll.get(0);
        Map<String, ArrayList> mOut = alAll.get(1);
        OutputStreamWriter[] outFile = {null, null, null};
        String sRootDb = VersionMananger.sRootPath.get(3);
        try {
            outFile[0] = new OutputStreamWriter(new FileOutputStream(new File(sRootDb + "/select.sql")), "UTF-8");
            outFile[1] = new OutputStreamWriter(new FileOutputStream(new File(sRootDb + "/insert.sql")), "UTF-8");
            outFile[2] = new OutputStreamWriter(new FileOutputStream(new File(sRootDb + "/delete.sql")), "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            GetFullCompareSql(connAll, alAll.get(0), alAll.get(1), outFile);
            GetChannelSql(connAll, alAll.get(0), outFile);
            GetProviderSql(connAll, alAll.get(1), outFile);
            GetServiceSql(connAll, alAll.get(1), outFile);

            outFile[0].close();
            outFile[1].close();
            outFile[2].close();
            CloseConn(connAll[0]);
            CloseConn(connAll[1]);
        } catch(IOException e) {
            e.printStackTrace();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

}
