package Client;

import com.jcraft.jsch.*;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import sun.security.krb5.Config;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static Client.DbProcess.GetSqlVer;
import static Client.FileProcess.CopySingleFile;

/**
 * Created by guoxj on 2017/5/26.
 */
public class VersionMananger {
    public static ArrayList<String> sRootPath = new ArrayList<String>();

    public static void main(String[] args) {
        //获取界面信息
        //版本清单
        String sXls = "D:/Project/Intellij_ws/ESBTools_pj/files/版本清单.XLS";
        //IP
        String sSrcIP = "10.4.32.40";
        String sDstIP = "10.4.32.41";
        LocalConfig config = new LocalConfig();
        String sLocalSvn = String.format("%s/SmartESB/configs", config.GetItemConfig("svn_" + sSrcIP));
        GetVersions(sSrcIP, sDstIP, sLocalSvn, sXls);
    }

    //获取各个root目录
    public static ArrayList<String> GetRootPaths(String sVerName) {
        LocalConfig config = new LocalConfig();
        Date d = new Date();
        System.out.println(d);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String sDate = sdf.format(d);
        SimpleDateFormat sdfTime = new SimpleDateFormat("yyyyMMddHHmm");
        String sTime = sdfTime.format(d);
        System.out.println("Version Create Time: " + sdfTime);

        String sVerTemp = config.GetItemConfig("ver_temp");
        sRootPath.add(0, String.format("%s/%s/%s/TLBank_SmartEsb_AP_%s_%s", sVerTemp, sDate, "localsvn", sVerName, sTime));
        sRootPath.add(1, String.format("%s/%s/%s/TLBank_SmartEsb_AP_%s_%s", sVerTemp, sDate, "remotesrc", sVerName, sTime));
        sRootPath.add(2, String.format("%s/%s/%s/TLBank_SmartEsb_AP_%s_%s", sVerTemp, sDate, "remotedst", sVerName, sTime));
        sRootPath.add(3, String.format("%s/%s/%s/TLBank_SmartEsb_DB_%s_%s/esbdb", sVerTemp, sDate, "remotedst", sVerName, sTime));
        return sRootPath;
    }


    // 创建打包后各版本文件存放目录
    public static void CreateVerDirs(String sVerName) {
        System.out.println("===创建打包后各版本文件存放目录 CreateVerDirs===");
        GetRootPaths(sVerName);
        for(String path : sRootPath) {
            File fPath = new File(path);
            if(!fPath.exists()) {
                fPath.mkdirs();
            }
        }
        return;
    }


    //获取配置版本
    public static void GetConfVer(String sSrcIP, String sDstIP, String sLocalSvn, ArrayList<Map<String, ArrayList>> alAll) {
        System.out.printf("\n=== 获取配置版本 [%s --> %s] ===\n", sSrcIP, sDstIP);
        String sVerSvnRoot = sRootPath.get(0);
        String sVerSrcRoot = sRootPath.get(1);
        String sVerDstRoot = sRootPath.get(2);
        try {
            //获取本地svn版本
            LocalConfig config = new LocalConfig();
//            String sLocalSvn = String.format("%s/SmartESB/configs", config.GetItemConfig("svn_" + sSrcIP));
            String sSvnDst = String.format("%s/SmartESB/configs", sVerSvnRoot);
            GetLocalVer(sLocalSvn, sSvnDst, alAll);

            //获取源服务器版本
            String sRemoteSrcPath = String.format("%s/SmartESB/configs", sVerSrcRoot);
            GetRemoteVer(sRemoteSrcPath, sSrcIP, alAll);

            //获取目标服务器版本
            String sRemoteDstPath = String.format("%s/SmartESB/configs", sVerDstRoot);
            GetRemoteVer(sRemoteDstPath, sDstIP, alAll);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    // 获取版本清单信息
    public static ArrayList<Map<String, ArrayList>> GetVerInfo(String sXls) throws IOException, BiffException {
        System.out.println("===获取版本清单信息 GetVerInfo===");
        LocalConfig config = new LocalConfig();
        Workbook wb = Workbook.getWorkbook(new File(sXls));
        Sheet sht = wb.getSheet("INDEX");
        int iRows = sht.getRows();
        int iStart = Integer.parseInt(config.GetItemConfig("esb_version_file_start"));
        Map<String, ArrayList> mIn = new HashMap<String, ArrayList>();
        Map<String, ArrayList> mOut = new HashMap<String, ArrayList>();
        ArrayList<Map<String, ArrayList>> alAll = new ArrayList();
        alAll.add(0, mIn);
        alAll.add(1, mOut);
        for(int row = iStart; row < iRows; row++) {
            // 消费者、服务者、服务号
            String consumerID = sht.getCell(1, row).getContents().trim();
            String providerID = sht.getCell(2, row).getContents().trim();
            String serviceID = sht.getCell(3, row).getContents().trim();
            if((consumerID.length() < 1) || (providerID.length() < 1) || (serviceID.length() < 1)) {
                System.out.printf("[info][line:%d]消费者、服务者、服务号其中有一个不存在\n", row);
                continue;
            }
            // 服务按消费系统归集
            ArrayList alIn = mIn.get(consumerID);
            if(null == alIn) {
                alIn = new ArrayList();
                alIn.add(serviceID);
                mIn.put(consumerID, alIn);
            } else if(!alIn.contains(serviceID)) {
                alIn.add(serviceID);
            }

            // 服务按服务系统归集
            ArrayList alOut = mOut.get(providerID);
            if(null == alOut) {
                alOut = new ArrayList();
                alOut.add(serviceID);
                mOut.put(providerID, alOut);
            } else if(!alOut.contains(serviceID)) {
                alOut.add(serviceID);
            }

        }
        return alAll;
    }


    // 打印版本信息
    public static void DisplayVerInfo(ArrayList<Map<String, ArrayList>> alAll) {
        System.out.println("===打印版本信息 DisplayVerInfo===");
        Map<String, ArrayList> mIn = alAll.get(0);
        Map<String, ArrayList> mOut = alAll.get(1);

        // 打印消费系统信息
        for(Map.Entry<String, ArrayList> one : mIn.entrySet()) {
            System.out.println("消费系统:" + one.getKey() + " :" + one.getValue());
        }

        // 打印服务系统信息
        for(Map.Entry<String, ArrayList> one : mOut.entrySet()) {
            System.out.println("消费系统:" + one.getKey() + " :" + one.getValue());
        }
    }

    // 拷贝公共文件
    public static void GetCommonFile(String sSrcRoot, String sDstRoot) throws IOException {
        // webservice 接入适配文件 serviceIdentify
        String sSrcFile = String.format("%s/in_conf/serviceIdentify.xml", sSrcRoot);
        String sDstFile = String.format("%s/in_conf/serviceIdentify.xml", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile)) {
            System.out.print("Copy File [serviceIdentify.xml] Failed.");
        }

        // 接入端系统信息文件 systemcode
        sSrcFile = String.format("%s/in_conf/frameworkdist/shared/systemcode.xml", sSrcRoot);
        sDstFile = String.format("%s/in_conf/frameworkdist/shared/systemcode.xml", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile)) {
            System.out.print("Copy File [in systemcode.xml] Failed.");
        }

        // webservice 接出适配文件
        sSrcFile = String.format("%s/out_conf/ws_operation.properties", sSrcRoot);
        sDstFile = String.format("%s/out_conf/ws_operation.properties", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile)) {
            System.out.print("Copy File [ws_operation.properties] Failed.");
        }

        // 接入端系统信息文件 systemcode
        sSrcFile = String.format("%s/out_conf/frameworkdist/shared/systemcode.xml", sSrcRoot);
        sDstFile = String.format("%s/out_conf/frameworkdist/shared/systemcode.xml", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile)) {
            System.out.print("Copy File [out systemcode.xml] Failed.");
        }

        // 信贷转换信息文件 loanTranCode.properites
        sSrcFile = String.format("%s/out_conf/frameworkdist/shared/loanTranCode.properites", sSrcRoot);
        sDstFile = String.format("%s/out_conf/frameworkdist/shared/loanTranCode.properites", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile)) {
            System.out.print("Copy File [out loanTranCode.properites] Failed.");
        }

    }

    // 拷贝远程公共文件
    public static void GetCommonFile(String sSrcRoot, String sDstRoot, ChannelSftp sFtp) throws IOException {
        // webservice 接入适配文件 serviceIdentify
        String sSrcFile = String.format("%s/in_conf/serviceIdentify.xml", sSrcRoot);
        String sDstFile = String.format("%s/in_conf/serviceIdentify.xml", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile, sFtp)) {
            System.out.print("Copy File [serviceIdentify.xml] Failed.");
        }

        // 接入端系统信息文件 systemcode
        sSrcFile = String.format("%s/in_conf/frameworkdist/shared/systemcode.xml", sSrcRoot);
        sDstFile = String.format("%s/in_conf/frameworkdist/shared/systemcode.xml", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile, sFtp)) {
            System.out.print("Copy File [in systemcode.xml] Failed.");
        }

        // webservice 接出适配文件
        sSrcFile = String.format("%s/out_conf/ws_operation.properties", sSrcRoot);
        sDstFile = String.format("%s/out_conf/ws_operation.properties", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile, sFtp)) {
            System.out.print("Copy File [ws_operation.properties] Failed.");
        }

        // 接入端系统信息文件 systemcode
        sSrcFile = String.format("%s/out_conf/frameworkdist/shared/systemcode.xml", sSrcRoot);
        sDstFile = String.format("%s/out_conf/frameworkdist/shared/systemcode.xml", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile, sFtp)) {
            System.out.print("Copy File [out systemcode.xml] Failed.");
        }

        // 信贷转换信息文件 loanTranCode.properites
        sSrcFile = String.format("%s/out_conf/frameworkdist/shared/loanTranCode.properites", sSrcRoot);
        sDstFile = String.format("%s/out_conf/frameworkdist/shared/loanTranCode.properites", sDstRoot);
        if(!CopySingleFile(sSrcFile, sDstFile, sFtp)) {
            System.out.print("Copy File [out loanTranCode.properites] Failed.");
        }

    }


    // 获取本地版本
    public static void GetLocalVer(String sSrcRoot, String sDstRoot, ArrayList<Map<String, ArrayList>> alAll) throws IOException {
        System.out.println("\n===获取版本 GetLocalVer===");
        Map<String, ArrayList> mIn = alAll.get(0);
        Map<String, ArrayList> mOut = alAll.get(1);
        String sInPath = "in_conf/frameworkdist/channel";
        String sOutPath = "out_conf/frameworkdist/channel";

        //拷贝公共文件


        String key = null;
        // 拷贝in_conf中的配置文件
        for(Map.Entry<String, ArrayList> one : mIn.entrySet()) {
            key = one.getKey();
            ArrayList alConsumer = one.getValue();
            String sSrcFile = String.format("%s/%s/identify/identify_config_%s.xml", sSrcRoot, sInPath, key);
            String sDstFile = String.format("%s/%s/identify/identify_config_%s.xml", sDstRoot, sInPath, key);
            // Copy indentify文件-localsvn remotesrc remotedst
            if(!CopySingleFile(sSrcFile, sDstFile)) {
                continue;
            }

            // 解析identify文件内容,Copy convert 和 mapping文件
            String sUnStdSrcSvn = null;
            String sUnStdDstSvn = null;

            for(int i = 0; i < alConsumer.size(); i++) {
                String sServiceID = (String) alConsumer.get(i);
                String[] sUnStdCfg = GetUnStandConfig(sSrcFile, "in", sServiceID);

                //拷贝convert文件
                if(sUnStdCfg[0].length() > 0) {
                    sUnStdSrcSvn = String.format("%s/%s/convert/%s/consumer_convert_%s.xml", sSrcRoot, sInPath, key, sUnStdCfg[0]);
                    sUnStdDstSvn = String.format("%s/%s/convert/%s/consumer_convert_%s.xml", sDstRoot, sInPath, key, sUnStdCfg[0]);
                    if(!CopySingleFile(sUnStdSrcSvn, sUnStdDstSvn)) {
                        continue;
                    }
                }

                //拷贝mapping文件
                if(sUnStdCfg[1].length() > 0) {
                    //拷贝 mapping_syshead 文件
                    sUnStdSrcSvn = String.format("%s/%s/mapping/%s/mapping_syshead.xml", sSrcRoot, sInPath, key);
                    sUnStdDstSvn = String.format("%s/%s/mapping/%s/mapping_syshead.xml", sDstRoot, sInPath, key);
                    if(!CopySingleFile(sUnStdSrcSvn, sUnStdDstSvn)) {
                        continue;
                    }

                    //拷贝 consumer_mapping 文件
                    sUnStdSrcSvn = String.format("%s/%s/mapping/%s/consumer_mapping_%s.xml", sSrcRoot, sInPath, key, sUnStdCfg[0]);
                    sUnStdDstSvn = String.format("%s/%s/mapping/%s/consumer_mapping_%s.xml", sDstRoot, sInPath, key, sUnStdCfg[0]);
                    if(!CopySingleFile(sUnStdSrcSvn, sUnStdDstSvn)) {
                        continue;
                    }

                    //拷贝 mapping_syshead_INIT 文件
                    if("CCFS".equals(key)) {
                        sUnStdSrcSvn = String.format("%s/%s/mapping/%s/mapping_syshead_INIT.xml", sSrcRoot, sInPath, key);
                        sUnStdDstSvn = String.format("%s/%s/mapping/%s//mapping_syshead_INIT.xml", sDstRoot, sInPath, key);
                        if(!CopySingleFile(sUnStdSrcSvn, sUnStdDstSvn)) {
                            continue;
                        }
                    }
                }
            }
            // Copy 其他文件

            if("DW".equals(key)) {
                // 需要拷贝convert mapping 目录
                System.out.println("数据仓库服务需要手动获取配置.");
            }
        }

        // 拷贝out_conf中的配置文件
        for(Map.Entry<String, ArrayList> one : mOut.entrySet()) {
            key = one.getKey();
            ArrayList alProvider = one.getValue();
            String sSrcFile = String.format("%s/%s/identify/system_identify_%s.xml", sSrcRoot, sOutPath, key);
            String sDstFile = String.format("%s/%s/identify/system_identify_%s.xml", sDstRoot, sOutPath, key);
            // Copy indentify文件-localsvn remotesrc remotedst
            if(!CopySingleFile(sSrcFile, sDstFile)) {
                continue;
            }

            // 解析identify文件内容,Copy convert 和 mapping文件
            String sUnStdSrcSvn = null;
            String sUnStdDstSvn = null;

            for(int i = 0; i < alProvider.size(); i++) {
                String sServiceID = (String) alProvider.get(i);
                String[] sUnStdCfg = GetUnStandConfig(sSrcFile, "out", sServiceID);
                //拷贝convert文件
                if(sUnStdCfg[0].length() > 0) {
                    //拷贝provider_convert 文件
                    sUnStdSrcSvn = String.format("%s/%s/convert/%s/provider_convert_%s.xml", sSrcRoot, sOutPath, key, sUnStdCfg[0]);
                    sUnStdDstSvn = String.format("%s/%s/convert/%s/provider_convert_%s.xml", sDstRoot, sOutPath, key, sUnStdCfg[0]);
                    if(!CopySingleFile(sUnStdSrcSvn, sUnStdDstSvn)) {
                        continue;
                    }
                }

                //拷贝mapping文件
                if(sUnStdCfg[1].length() > 0) {
                    //拷贝 mapping_syshead 文件
                    sUnStdSrcSvn = String.format("%s/%s/mapping/%s/mapping_syshead.xml", sSrcRoot, sOutPath, key);
                    sUnStdDstSvn = String.format("%s/%s/mapping/%s/mapping_syshead.xml", sDstRoot, sOutPath, key);
                    if(!CopySingleFile(sUnStdSrcSvn, sUnStdDstSvn)) {
                        continue;
                    }

                    //拷贝 provider_mapping 文件
                    sUnStdSrcSvn = String.format("%s/%s/mapping/%s/provider_mapping_%s.xml", sSrcRoot, sOutPath, key, sUnStdCfg[0]);
                    sUnStdDstSvn = String.format("%s/%s/mapping/%s/provider_mapping_%s.xml", sDstRoot, sOutPath, key, sUnStdCfg[0]);
                    if(!CopySingleFile(sUnStdSrcSvn, sUnStdDstSvn)) {
                        continue;
                    }

                    //拷贝 mapping_syshead_INIT 文件
                    if("CCFS".equals(key)) {
                        sUnStdSrcSvn = String.format("%s/%s/mapping/%s/mapping_syshead_INIT.xml", sSrcRoot, sOutPath, key);
                        sUnStdDstSvn = String.format("%s/%s/mapping/%s//mapping_syshead_INIT.xml", sDstRoot, sOutPath, key);
                        if(!CopySingleFile(sUnStdSrcSvn, sUnStdDstSvn)) {
                            continue;
                        }
                    }
                }
            }
            // Copy 其他文件

            if("DW".equals(key)) {
                // 需要拷贝convert mapping 目录
                System.out.println("数据仓库服务需要手动获取配置.");
            }
        }
    }


    public static void GetRemoteVer(String sDstRoot, String sSrcIP, ArrayList<Map<String, ArrayList>> alAll) throws IOException {
        System.out.printf("\n===获取版本 GetRemoteVer [%s]===\n", sSrcIP);
        LocalConfig config = new LocalConfig();

        int port = 22;
        String sSrcUser = config.GetItemConfig("esb_" + sSrcIP).split("/")[0];
        String sSrcPasswd = config.GetItemConfig("esb_" + sSrcIP).split("/")[1];
        ChannelSftp sFtpChannel = ConnectToServer(sSrcIP, sSrcUser, sSrcPasswd, port);
        if(null == sFtpChannel) {
            System.out.printf("连接ftp[%s],失败\n", sSrcIP);
        }

        String sInPath = "in_conf/frameworkdist/channel";
        String sOutPath = "out_conf/frameworkdist/channel";
        String sSrcRoot = "/home/esb/SmartESB/configs";

        Map<String, ArrayList> mIn = alAll.get(0);
        Map<String, ArrayList> mOut = alAll.get(1);

        String key = null;
        // 拷贝in_conf中的配置文件
        for(Map.Entry<String, ArrayList> one : mIn.entrySet()) {
            key = one.getKey();
            ArrayList alConsumer = one.getValue();
            String sSrcFile = String.format("%s/%s/identify/identify_config_%s.xml", sSrcRoot, sInPath, key);
            String sDstFile = String.format("%s/%s/identify/identify_config_%s.xml", sDstRoot, sInPath, key);

            // Copy indentify文件- remote
            if(!CopySingleFile(sSrcFile, sDstFile, sFtpChannel)) {
                continue;
            }

            // 解析identify文件内容,Copy convert 和 mapping文件
            String sUnStdSrcRemote = null;
            String sUnStdDst = null;

            for(int i = 0; i < alConsumer.size(); i++) {
                String sServiceID = (String) alConsumer.get(i);
                String[] sUnStdCfg = GetUnStandConfig(sDstFile, "in", sServiceID);
                //拷贝convert文件
                if(sUnStdCfg[0].length() > 0) {
                    //拷贝provider_convert 文件
                    sUnStdSrcRemote = String.format("%s/%s/convert/%s/consumer_convert_%s.xml", sSrcRoot, sInPath, key, sUnStdCfg[0]);
                    sUnStdDst = String.format("%s/%s/convert/%s/consumer_convert_%s.xml", sDstRoot, sInPath, key, sUnStdCfg[0]);
                    if(!CopySingleFile(sUnStdSrcRemote, sUnStdDst, sFtpChannel)) {
                        continue;
                    }
                }

                //拷贝mapping文件
                if(sUnStdCfg[1].length() > 0) {
                    //拷贝 mapping_syshead 文件
                    sUnStdSrcRemote = String.format("%s/%s/mapping/%s/mapping_syshead.xml", sSrcRoot, sInPath, key);
                    sUnStdDst = String.format("%s/%s/mapping/%s/mapping_syshead.xml", sDstRoot, sInPath, key);
                    if(!CopySingleFile(sUnStdSrcRemote, sUnStdDst, sFtpChannel)) {
                        continue;
                    }

                    //拷贝 consumer_mapping 文件
                    sUnStdSrcRemote = String.format("%s/%s/mapping/%s/consumer_mapping_%s.xml", sSrcRoot, sInPath, key, sUnStdCfg[0]);
                    sUnStdDst = String.format("%s/%s/mapping/%s/consumer_mapping_%s.xml", sDstRoot, sInPath, key, sUnStdCfg[0]);
                    if(!CopySingleFile(sUnStdSrcRemote, sUnStdDst, sFtpChannel)) {
                        continue;
                    }


                    //拷贝 mapping_syshead_INIT 文件
                    if("CCFS".equals(key)) {
                        sUnStdSrcRemote = String.format("%s/%s/mapping/%s/mapping_syshead_INIT.xml", sSrcRoot, sInPath, sUnStdCfg[0]);
                        sUnStdDst = String.format("%s/%s/mapping/%s/mapping_syshead_INIT.xml", sDstRoot, sInPath, sUnStdCfg[0]);
                        if(!CopySingleFile(sUnStdSrcRemote, sUnStdDst, sFtpChannel)) {
                            continue;
                        }
                    }
                }
            }
            // Copy 其他文件

            if("DW".equals(key)) {
                // 需要拷贝convert mapping 目录
                System.out.println("数据仓库服务需要手动获取配置.");
            }
        }

        // 拷贝out_conf中的配置文件
        for(Map.Entry<String, ArrayList> one : mOut.entrySet()) {
            key = one.getKey();
            ArrayList alProvider = one.getValue();
            String sSrcFile = String.format("%s/%s/identify/system_identify_%s.xml", sSrcRoot, sOutPath, key);
            String sDstFile = String.format("%s/%s/identify/system_identify_%s.xml", sDstRoot, sOutPath, key);

            // Copy indentify文件- remote
            if(!CopySingleFile(sSrcFile, sDstFile, sFtpChannel)) {
                continue;
            }

            // 解析identify文件内容,Copy convert 和 mapping文件
            String sUnStdSrcRemote = null;
            String sUnStdDst = null;

            for(int i = 0; i < alProvider.size(); i++) {
                String sServiceID = (String) alProvider.get(i);
                String[] sUnStdCfg = GetUnStandConfig(sDstFile, "out", sServiceID);
                //拷贝convert文件
                if(sUnStdCfg[0].length() > 0) {
                    //拷贝provider_convert 文件
                    sUnStdSrcRemote = String.format("%s/%s/convert/%s/provider_convert_%s.xml", sSrcRoot, sOutPath, key, sUnStdCfg[0]);
                    sUnStdDst = String.format("%s/%s/convert/%s/provider_convert_%s.xml", sDstRoot, sOutPath, key, sUnStdCfg[0]);
                    if(!CopySingleFile(sUnStdSrcRemote, sUnStdDst, sFtpChannel)) {
                        continue;
                    }
                }

                //拷贝mapping文件
                if(sUnStdCfg[1].length() > 0) {
                    //拷贝 mapping_syshead 文件
                    sUnStdSrcRemote = String.format("%s/%s/mapping/%s/mapping_syshead.xml", sSrcRoot, sOutPath, key);
                    sUnStdDst = String.format("%s/%s/mapping/%s/mapping_syshead.xml", sDstRoot, sOutPath, key);
                    if(!CopySingleFile(sUnStdSrcRemote, sUnStdDst, sFtpChannel)) {
                        continue;
                    }

                    //拷贝 provider_mapping 文件
                    sUnStdSrcRemote = String.format("%s/%s/mapping/%s/provider_mapping_%s.xml", sSrcRoot, sOutPath, key, sUnStdCfg[0]);
                    sUnStdDst = String.format("%s/%s/mapping/%s/provider_mapping_%s.xml", sDstRoot, sOutPath, key, sUnStdCfg[0]);
                    if(!CopySingleFile(sUnStdSrcRemote, sUnStdDst, sFtpChannel)) {
                        continue;
                    }


                    //拷贝 mapping_syshead_INIT 文件
                    if("CCFS".equals(key)) {
                        sUnStdSrcRemote = String.format("%s/%s/mapping/%s/mapping_syshead_INIT.xml", sSrcRoot, sOutPath, sUnStdCfg[0]);
                        sUnStdDst = String.format("%s/%s/mapping/%s/mapping_syshead_INIT.xml", sDstRoot, sOutPath, sUnStdCfg[0]);
                        if(!CopySingleFile(sUnStdSrcRemote, sUnStdDst, sFtpChannel)) {
                            continue;
                        }
                    }
                }
            }
            // Copy 其他文件

            if("DW".equals(key)) {
                // 需要拷贝convert mapping 目录
                System.out.println("数据仓库服务需要手动获取配置.");
            }
        }

        try {
            CloseSftp(sFtpChannel);
        } catch(JSchException e) {
            e.printStackTrace();
        }

    }


    //获取配置中对应服务id 的相关id 和 mapping-id
    public static String[] GetUnStandConfig(String sFile, String sDirection, String sServiceID) {
        // convert-id mapping-id service-id
        FileInputStream fis = null;
        BufferedReader brFile = null;
        String[] unstandConfig = {"", "", ""};
        try {
            fis = new FileInputStream(sFile);
            brFile = new BufferedReader(new InputStreamReader(fis));
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            return unstandConfig;
        }

        try {
            for(String line = brFile.readLine(); line != null; line = brFile.readLine()) {
                line = line.trim();

                if(line.startsWith("</") || (line.split("\"").length < 5)) {
                    continue;
                }

                // identify_config_***.xml 接入配置文件以tran标签开始,找到指定服务的配置
                if(("in".equals(sDirection)) && line.startsWith("<tran") && line.contains(sServiceID)) {
                    String[] lineParts = line.split("\"");

                    //convert-id
                    unstandConfig[0] = lineParts[1].length() > 0 ? lineParts[1] : "";
                    //mapping-id
                    unstandConfig[1] = lineParts[3].length() > 0 ? lineParts[3] : "";
                    //service-id
                    unstandConfig[2] = lineParts[5].length() > 0 ? lineParts[5] : "";
                    //如果conver-id 和 service-id相等，则说明不用拷贝convert文件
                    if(unstandConfig[0].equals(unstandConfig[2])) {
                        unstandConfig[0] = "";
                    }
                    System.out.printf("in_conf, convert-id=%s, mapping-id=%s, service-id=%s\n", unstandConfig[0], unstandConfig[1], unstandConfig[2]);

                    break;

                }

                // system_identify_***.xml 接出配置文件以service标签开始,找到指定服务的配置
                if(("out".equals(sDirection)) && (line.startsWith("<service") && line.contains(sServiceID))) {
                    String[] lineParts = line.split("\"");

                    //convert-id
                    unstandConfig[0] = lineParts[5].length() > 0 ? lineParts[5] : "";
                    //mapping-id
                    unstandConfig[1] = lineParts[3].length() > 0 ? lineParts[3] : "";
                    //service-id
                    unstandConfig[2] = lineParts[1].length() > 0 ? lineParts[1] : "";
                    //如果conver-id 和 service-id相等，则说明不用拷贝convert文件
                    if(unstandConfig[0].equals(unstandConfig[2])) {
                        unstandConfig[0] = "";
                    }
                    System.out.printf("out_conf, convert-id=%s, mapping-id=%s, service-id=%s\n", unstandConfig[0], unstandConfig[1], unstandConfig[2]);

                    break;
                }
            }

        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }

        try {
            brFile.close();
            fis.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

        return unstandConfig;
    }

    //获取远程ssh链接
    public static ChannelSftp ConnectToServer(String sIP, String sUser, String sPasswd, int port) {
        ChannelSftp sftp = null;
        Session sshSession = null;
        try {
            JSch jsch = new JSch();
            System.out.println("开始建立远程Sftp服务.Start to Create Session......");
            jsch.getSession(sUser, sIP, port);
            sshSession = jsch.getSession(sUser, sIP, port);
            sshSession.setPassword(sPasswd);
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(sshConfig);
            sshSession.connect();
            //            System.out.println("远程会话Session connected.");
            //            System.out.println("sftp start Opening Channel.");
            Channel channel = sshSession.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;
            //            System.out.println("Connected to " + ip + ".");
            return sftp;
        } catch(Exception e) {
            System.out.printf("连接远程服务器异常.ip=%s,port=%d,user=%s\n", sIP, port, sUser);
            e.printStackTrace();
            return sftp;
        }

    }


    //关闭远程ssh链接
    public static void CloseSftp(ChannelSftp sFtp) throws JSchException {
        System.out.println("关闭sftp链接......");
        Session sshSession = sFtp.getSession();
        //关闭ftp链接
        if(sFtp != null) {
            sFtp.disconnect();
            sFtp.exit();
            sFtp = null;
        }

        //关闭会话
        if(sshSession != null) {
            sshSession.disconnect();
            sshSession = null;
        }
    }


    //获取全量版本
    public static void GetVersions(String sSrcIP, String sDstIP, String sLocalSvn, String sXls) {
        try {
            //获取版本清单内容
            ArrayList<Map<String, ArrayList>> alAll = GetVerInfo(sXls);
//            String sLocalSvn = String.format("%s/SmartESB/configs", config.GetItemConfig("svn_" + sSrcIP));
            //打印版本信息
            DisplayVerInfo(alAll);
            //创建版本文件夹
            Set<String> setKey = alAll.get(0).keySet();
            String sChannel = setKey.iterator().next();
            System.out.println("Version Name : " + sChannel);
            CreateVerDirs(sChannel);
            GetConfVer(sSrcIP, sDstIP, sLocalSvn, alAll);
            System.out.println("\n==获取配置文件版本完成==");
            GetSqlVer(sSrcIP, sDstIP, alAll);
            System.out.println("\n==获取数据库脚本完成==");
        } catch(IOException e) {
            e.printStackTrace();
        } catch(BiffException e) {
            e.printStackTrace();
        }
    }


    //遍历目录生成相对路径文件列 sRel初始值为"/"
    public static ArrayList<String> GetRelPaths(String sRoot, String sRel, ArrayList<String> alRelPaths) {
        File fDir = new File(sRoot);
        if(fDir.isDirectory()) {
            String[] sSub = fDir.list();
            //递归目录中的子目录下
            for(int i = 0; i < sSub.length; i++) {
                File childFile = new File(sRoot + "/" + sSub[i]);
                if(childFile.isFile()) {
                    alRelPaths.add(0, sRel + "/" + sSub[i]);
                } else {
                    alRelPaths = GetRelPaths(sRoot + "/" + sSub[i], sRel + "/" + sSub[i], alRelPaths);
                }
            }
        }

        return alRelPaths;
    }


    //打印相对路径列表
    public static void DisRelPaths(ArrayList<String> alRelPaths) {
        System.out.println("----打印相对路径列表");
        for(int i = 0; i < alRelPaths.size(); i++) {
            System.out.println(alRelPaths.get(i));
        }
    }


    //生成sh脚本
    public static void CreateVerAPScript(String sAPPath) throws IOException {
        String sUpdateFile = sAPPath + "/update.sh";
        String sRecoverFile = sAPPath + "/recover.sh";
        String sBackupFile = sAPPath + "/backup.sh";
        FileOutputStream fosBackup = new FileOutputStream(new File(sBackupFile));
        FileOutputStream fosUpdate = new FileOutputStream(new File(sUpdateFile));
        FileOutputStream fosRecover = new FileOutputStream(new File(sRecoverFile));
        int iSize = sAPPath.split("_").length;
        String sTime = sAPPath.split("_")[iSize - 1];

        String sDes = String.format("echo ====begin to backup %s====\nmkdir -p ./backup\n\n", sTime);
        fosBackup.write(sDes.getBytes());

        sDes = String.format("echo ====begin to update %s====\n", sTime);
        fosUpdate.write(sDes.getBytes());

        sDes = String.format("echo ====begin to recover %s====\n", sTime);
        fosRecover.write(sDes.getBytes());

        ArrayList<String> alAPRelPaths = new ArrayList<String>();
        alAPRelPaths = GetRelPaths(sAPPath, "", alAPRelPaths);
        DisRelPaths(alAPRelPaths);

        for(int i = 0; i < alAPRelPaths.size(); i++) {
            if(!alAPRelPaths.get(i).startsWith("/SmartESB")) {
                continue;
            }
            String sBackupTmp = String.format("cp --parents -rf /home/esb%s  ./backup\n" +
                            "ls -lrt ./backup/home/esb%s\n\n",
                    alAPRelPaths.get(i), alAPRelPaths.get(i));
            String sUpdateTmp = String.format("cp --parents -rf .%s /home/esb\n" +
                            "ls -lrt /home/esb%s\n\n",
                    alAPRelPaths.get(i), alAPRelPaths.get(i));
            String sRecoverTmp = String.format("cp --parents -rf ./backup/home/esb%s  /home/esb\n" +
                            "ls -lrt /home/esb%s\n\n",
                    alAPRelPaths.get(i), alAPRelPaths.get(i));
            fosBackup.write(sBackupTmp.getBytes());
            fosUpdate.write(sUpdateTmp.getBytes());
            fosRecover.write(sRecoverTmp.getBytes());
        }

        fosBackup.close();
        fosUpdate.close();
        fosRecover.close();
    }


    //生成pdc脚本
    public static void CreateVerDBScript(String sDbPath) throws IOException {
        String sUpdateFile = sDbPath + "/update.pdc";
        String sRecoverFile = sDbPath + "/recover.pdc";
        FileOutputStream fosUpdate = new FileOutputStream(new File(sUpdateFile));
        FileOutputStream fosRecover = new FileOutputStream(new File(sRecoverFile));
        ArrayList<String> alDbRelPaths = new ArrayList<String>();
        alDbRelPaths = GetRelPaths(sDbPath, "", alDbRelPaths);
        DisRelPaths(alDbRelPaths);
        for(int i = 0; i < alDbRelPaths.size(); i++) {
            if(!alDbRelPaths.get(i).startsWith("/esbdb")) {
                continue;
            }
            String sTmp = String.format("prompt %s\n@%s\n\n", alDbRelPaths.get(i), alDbRelPaths.get(i));
            if(sTmp.contains("delete.sql")) {
                fosRecover.write(sTmp.getBytes());
            } else if(sTmp.contains("select.sql")) {
                continue;
            } else {
                fosUpdate.write(sTmp.getBytes());
            }
        }
        String sDes = "exit;\n";
        fosUpdate.write(sDes.getBytes());
        fosRecover.write(sDes.getBytes());

        fosUpdate.close();
        fosRecover.close();
    }


    //生成版本脚本
    public static void CreateScripts(String sVerPath) throws IOException {
        File fVerPath = new File(sVerPath);
        String[] sSub = fVerPath.list();
        String sUpdateAllAP = sVerPath + "/UpdateVersion.sh";
        String sRecoverAllAP = sVerPath + "/RecoverVersion.sh";
        String sDes = null;
        FileOutputStream fosUpdateAll = new FileOutputStream(new File(sUpdateAllAP));
        FileOutputStream fosRecoverAll = new FileOutputStream(new File(sRecoverAllAP));
        Arrays.sort(sSub);
        //生成sh脚本
        for(int i = 0; i < sSub.length; i++) {
            String sTmp = sVerPath + "/" + sSub[i];
            File sChild = new File(sTmp);
            if(sChild.isFile()) {
                continue;
            }
            if(sSub[i].startsWith("TLBank_SmartEsb_AP_")) {
                sDes = String.format("echo =====update version %s =====\n" +
                                "cd ./%s/ \n" + "sh backup.sh \n" + "sh update.sh \n" + "cd ../\n\n",
                        sSub[i], sSub[i]);
                fosUpdateAll.write(sDes.getBytes());
                CreateVerAPScript(sTmp);

            } else if(sSub[i].startsWith("TLBank_SmartEsb_DB_")) {
                //生成pdc脚本
                CreateVerDBScript(sTmp);
            } else {

            }
        }

        // recover.sh 要按时间排序倒过来执行
        for(int i = sSub.length - 1; i >= 0; i--) {
            String sTmp = sVerPath + "/" + sSub[i];
            File sChild = new File(sTmp);
            if((!sChild.isFile()) && (sSub[i].startsWith("TLBank_SmartEsb_AP_"))) {
                sDes = String.format("echo =====recover version %s =====\n" +
                                "cd ./%s/ \n" + "sh recover.sh \n" + "cd ../\n\n",
                        sSub[i], sSub[i]);
                fosRecoverAll.write(sDes.getBytes());
            }
        }

        fosUpdateAll.close();
        fosRecoverAll.close();
    }
}


class Readfile implements Runnable {
    String sConfig = null;

    public Readfile(String sConfig) {
        this.sConfig = sConfig;
    }

    public void run() {
        String cmd = new File("").getAbsolutePath().replace("\\", "/") + "/tools/notepad";
        System.out.println(cmd + " " + sConfig);
        try {
            Process pro = Runtime.getRuntime().exec(cmd + " " + sConfig);
            pro.waitFor();
        } catch(IOException ep) {
            System.out.printf("打开文件allConfig.properties异常:" + ep);
        } catch(InterruptedException ep) {
            System.out.printf("打开文件allConfig.properties异常:" + ep);
        }
    }
}
