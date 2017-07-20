/*
 * Created by JFormDesigner on Tue Jul 12 15:22:58 CST 2016
 */

package Client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.GroupLayout;
import javax.swing.border.*;

import static Client.VersionMananger.GetRootPaths;
import static Client.VersionMananger.GetVersions;
import static java.lang.Thread.sleep;


/**
 * @author unknown
 */
public class TestESB extends JFrame {
    public static void main(String[] args){
        TestESB test = new TestESB();
//        String cmd = "chcp 65001";
//        System.out.println(cmd);
//        try {
//            Runtime.getRuntime().exec(cmd);
//            System.out.println(cmd);
//        } catch(IOException e) {
//            e.printStackTrace();
//        }

        test.setVisible(true);
        test.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public TestESB() {
        initComponents();
        LocalConfig config = new LocalConfig();
        tf_XlsDict.setText(config.GetItemConfig("tl_dict_path"));
        tf_ShtDict.setText(config.GetItemConfig("tl_sheet_dict"));
        tf_XlsCh.setText(config.GetItemConfig("xls_dict_ch"));
        tf_ShtCh.setText(config.GetItemConfig("sheet_dict_ch"));
        tf_XlsOut.setText(config.GetItemConfig("xls_dict_en"));
        tf_ShtOut.setText(config.GetItemConfig("sheet_dict_en"));

        // 设置默认版本清单位置
        String verXls = new File("").getAbsolutePath() + config.GetItemConfig("esb_version_file");
        t_ExcelPath.setText(verXls.replace("\\", "/"));

        tSrcIP.setText(config.GetItemConfig("src_ip"));
        tDstIP.setText(config.GetItemConfig("dst_ip"));

        t_SvnPath.setText(config.GetItemConfig("svn_10.4.32.40"));

//        t_VerPathLast.setText(config.GetItemConfig("ver_finnal"));
    }

    private void list_EnvActionPerformed(ActionEvent e) {
        // TODO add your code here
        int envIndex = list_Env.getSelectedIndex();
        String[] envIP = {"10.4.16.231", "10.4.24.167", "10.4.32.41"};

        if(envIndex < envIP.length) {
            t_IP.setText(envIP[envIndex]);
            t_HeadLen.setText("8");
            t_RevLen.setText("8");
            t_Address.setText("http://" + envIP[envIndex] + ":38080/***");
        } else {
            System.out.printf("系统列表值异常.最大值:%d, 当前值:%d\n", envIP.length, envIndex);
        }
    }

    private void btn_ActionPerformed(ActionEvent e) {
        // TODO add your code here
        String reqXml = new File("").getAbsolutePath().replace("\\", "/") + "/files/req.xml";
        Readfile rf = new Readfile(reqXml);
        Thread thread = new Thread(rf);
        thread.start();
    }

    private void btn_TCPActionPerformed(ActionEvent e) {
        // TODO add your code here
        int headlen = Integer.parseInt(t_HeadLen.getText().toString());
        int revlen = Integer.parseInt(t_RevLen.getText().toString());
        int times = Integer.parseInt(t_Times.getText().toString());
        int timeout = Integer.parseInt(t_TimeOut.getText().toString());
        String ip = t_IP.getText();
        String port = t_PORT.getText();
        String ipRegex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
        if((null == ip) || (ip.length() < 8) || (!ip.matches(ipRegex))) {
            System.out.println("参数有误,IP地址不合法,IP=" + ip);
            return;
        }

        if((null == port) || (port.length() < 1) || (!port.matches("[1-9][\\d]*"))) {
            System.out.println("参数有误,PORT不合法,PORT=" + port);
            return;
        }
        System.out.printf("发送TCP/IP报文. IP:%s, PORT:%s \n", ip, port);

        // 发送报文
        String reqXml = new File("").getAbsolutePath().replace("\\", "/") + "/files/req.xml";
        TestTcp t_dev = new TestTcp();
        t_dev.Init(ip, Integer.parseInt(port), timeout, headlen, revlen, reqXml);
        try {
            for(int i = 0; i < times; i++) {
                String resp_dev = t_dev.sendTcpRequest();
//                Thread.sleep(1);
                if((null != resp_dev) && (resp_dev.length() > 0)) {
                    t_AreaRsp.setText(resp_dev.replace("><", ">\n<"));
                }
            }

        } catch(IOException e1) {
            e1.printStackTrace();
//        } catch (InterruptedException e1) {
//            e1.printStackTrace();
        } finally {
            t_dev = null;
        }

    }

    private void thisWindowClosed(WindowEvent e) {
        // TODO add your code here
    }

    private void btn_choosexlsActionPerformed(ActionEvent e) {
        // TODO add your code here

        String defaultPath = new File("").getAbsolutePath() + "/files";
        defaultPath = defaultPath.replace("\\", "/");
        File fPath = new File(defaultPath);
        JFileChooser jfc = new JFileChooser(fPath);
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setApproveButtonText("确定");
        jfc.setDialogTitle("选择清单文件Excel");
        jfc.setVisible(true);
        int ret = jfc.showOpenDialog(this);
        File selectFile = null;
        if(ret == JFileChooser.APPROVE_OPTION) {
            selectFile = jfc.getSelectedFile();
            t_ExcelPath.setText(selectFile.getAbsolutePath().replace("\\", "/"));
        }
    }

    private void btn_choosepathActionPerformed(ActionEvent e) {
        // TODO add your code here
        LocalConfig config = new LocalConfig();
        String defaultPath = config.GetItemConfig("ver_finnal");
        defaultPath = defaultPath.replace("\\", "/");
        File fPath = new File(defaultPath);
        JFileChooser jfc = new JFileChooser(fPath);
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setApproveButtonText("确定");
        jfc.setDialogTitle("选择最终版本路径");
        jfc.setVisible(true);
        int ret = jfc.showOpenDialog(this);
        File selectPath = null;
        if(ret == JFileChooser.APPROVE_OPTION) {
            selectPath = jfc.getSelectedFile();
            t_VerPathLast.setText(selectPath.getAbsolutePath().replace("\\", "/"));
        }
    }

    private void btn_EditXLSActionPerformed(ActionEvent e) {
        // TODO add your code here
        String excel = t_ExcelPath.getText().trim();
        if(!excel.endsWith("xls") && !excel.endsWith("XLS")) {
            JOptionPane.showMessageDialog(this, "步骤0选择的excel格式不正确：" + excel.substring(excel.lastIndexOf("/")),
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cmd = new File("").getAbsolutePath() + "/tools/Microexcel/bin/microexcel.exe " + excel;
//        System.out.println(cmd);
        try {
            Runtime.getRuntime().exec(cmd);
        } catch(IOException err) {
            err.printStackTrace();
        }
    }

    private void btn_CreateShActionPerformed(ActionEvent e) {
        // TODO add your code here
        String verPathLast = t_VerPathLast.getText().trim();
        File fVerPath = new File(verPathLast);
        if((!fVerPath.exists()) || (!fVerPath.isDirectory())){
            JOptionPane.showMessageDialog(this, "版本路径不是目录：" + verPathLast.substring(verPathLast.lastIndexOf("/")),
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            VersionMananger.CreateScripts(verPathLast);
            JOptionPane.showMessageDialog(this, "脚本生成成功！","Info", JOptionPane.INFORMATION_MESSAGE);
        } catch(IOException err) {
            err.printStackTrace();
        }
        return;
    }

    private void btn_HTTPActionPerformed(ActionEvent e) {
        // TODO add your code here
        String url = t_Address.getText().trim();
        if(url == null || url.length() < 0) {
            JOptionPane.showMessageDialog(this, "请求的http路径为空!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int timeout = 10000;
        int headlen = 8;
        String reqXml = new File("").getAbsolutePath().replace("\\", "/") + "/files/req.xml";
        String result = null;
        // 获取报文内容 组装报文
        byte[] req = TestProxy.getMsgData(reqXml);
        String req_msg = null;
        try {
            req_msg = new String(req, "UTF-8").replaceAll("  ", "").replaceAll("[\\t\\n\\r]", "");
            // 获取报文长度时必须用getBytes 计算字节码长度，不然中文的传输会有问题
            req_msg = String.format("%08d", req_msg.getBytes().length) + req_msg;
            System.out.println("请求数据[ " + req_msg + " ]\n");

            TestHttp http = new TestHttp(url, timeout, headlen);
            result = http.SendPostReq(req_msg);

            System.out.println("响应数据[ " + result.replace("><", ">\n<") + " ]\n");
            t_AreaRsp.setText(result.replace("><", ">\n<"));
        } catch(UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }


    }

    private void btn_dcitChoose(ActionEvent e) {
        // TODO add your code here
        String defaultPath = new File("").getAbsolutePath() + "/泰隆_数据字典.xlsx";
        defaultPath = defaultPath.replace("\\", "/");
        File fPath = new File(defaultPath);
        JFileChooser jfc = new JFileChooser(fPath);
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setApproveButtonText("确定");
        jfc.setDialogTitle("选择<泰隆_数据字典>Excel文件");
        jfc.setVisible(true);
        int ret = jfc.showOpenDialog(this);
        File selectFile = null;
        if(ret == JFileChooser.APPROVE_OPTION) {
            selectFile = jfc.getSelectedFile();
            tf_XlsDict.setText(selectFile.getAbsolutePath());
        }
    }

    private void btn_chChoose(ActionEvent e) {
        // TODO add your code here
        String defaultPath = new File("").getAbsolutePath() + "/Dict_Ch.xls";
        defaultPath = defaultPath.replace("\\", "/");
        File fPath = new File(defaultPath);
        JFileChooser jfc = new JFileChooser(fPath);
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setApproveButtonText("确定");
        jfc.setDialogTitle("选择<中文字段>Excel文件");
        jfc.setVisible(true);
        int ret = jfc.showOpenDialog(this);
        File selectFile = null;
        if(ret == JFileChooser.APPROVE_OPTION) {
            selectFile = jfc.getSelectedFile();
            tf_XlsCh.setText(selectFile.getAbsolutePath());
        }
    }

    private void btn_ConvertAction(ActionEvent e) {
        // TODO add your code here
        String sXlsDict = tf_XlsDict.getText().trim();
        sXlsDict = sXlsDict.replace("\\", "/");
        String sShtDict = tf_ShtDict.getText().trim();
        String sXlsCh = tf_XlsCh.getText().trim();
        sXlsCh = sXlsCh.replace("\\", "/");
        String sShtCh = tf_ShtCh.getText().trim();
        String sXlsOut = tf_XlsOut.getText().trim();
        String sShtOut = tf_ShtOut.getText().trim();

        if((sXlsDict.length() < 1) || (!(new File(sXlsDict).exists()))) {
            JOptionPane.showMessageDialog(this, "文件不存在, 请重新选择<泰隆_数据字典>!", "Info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if((sXlsCh.length() < 1) || (!(new File(sXlsCh).exists()))) {
            JOptionPane.showMessageDialog(this, "文件不存在, 请重新选择中文字段文件!", "Info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if(sShtCh.length() < 1) {
            JOptionPane.showMessageDialog(this, "中文字段文件sheet页为空, 请输入!", "Info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if(sShtOut.length() < 1) {
            JOptionPane.showMessageDialog(this, "中文字段文件sheet页为空, 赋值为中文字段sheet页的名称!", "Info", JOptionPane.WARNING_MESSAGE);
            tf_ShtOut.setText(sShtCh);
        }

        try {
            ConvertDict.ConvertCh2En(sXlsDict, sShtDict, sXlsCh, sShtCh, sXlsOut, sShtOut);
            JOptionPane.showMessageDialog(this, "转换结束!", "Info", JOptionPane.WARNING_MESSAGE);
        } catch(Exception e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(this, "转换失败！", "Info", JOptionPane.WARNING_MESSAGE);
        }

    }

    private void btn_ChooseSvnActionPerformed(ActionEvent e) {
        // TODO add your code here
        // TODO add your code here
        String defaultPath = "D:/";
        defaultPath = defaultPath.replace("\\", "/");
        File fPath = new File(defaultPath);
        JFileChooser jfc = new JFileChooser(fPath);
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setApproveButtonText("确定");
        jfc.setDialogTitle("选择本地SVN目录");
        jfc.setVisible(true);
        int ret = jfc.showOpenDialog(this);
        File selectFile = null;
        if(ret == JFileChooser.APPROVE_OPTION) {
            selectFile = jfc.getSelectedFile();
            t_SvnPath.setText(selectFile.getAbsolutePath().replace("\\", "/"));
        }
    }

    private void btn_CreateVer2ActionPerformed(ActionEvent e) {
        // TODO add your code here
        String sXls = t_ExcelPath.getText().trim();
        //IP
        String sSrcIP = tSrcIP.getText().trim();
        String sDstIP = tDstIP.getText().trim();
        String sLocalSvn = t_SvnPath.getText().trim() + "/configs";
        GetVersions(sSrcIP, sDstIP, sLocalSvn, sXls);

        LocalConfig config = new LocalConfig();
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String sDate = sdf.format(d);
        String sResultPath = config.GetItemConfig("ver_temp") + "/" + sDate;
        t_VerResult.setText(sResultPath);

        JOptionPane.showMessageDialog(this, "打包全量版本成功.", "Warning", JOptionPane.INFORMATION_MESSAGE);
    }

    private void btn_CreateSh2ActionPerformed(ActionEvent e) {
        // TODO add your code here
        String sConfig = new File("").getAbsolutePath().replace("\\", "/") + "/files/allConfig.properties";
        Readfile rf = new Readfile(sConfig);
        Thread thread = new Thread(rf);
        thread.start();
        JOptionPane.showMessageDialog(this, "配置修改需重启才能完全生效!", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void btn_ChooseSvn2ActionPerformed(ActionEvent e) {
        // TODO add your code here
        String sSvn = t_SvnPath.getText().trim();
        String sDir = sSvn.substring(0, sSvn.indexOf(":"));
        String sSvnBat = new File("").getAbsolutePath().replace("\\", "/") + "/files/updatesvn.bat";
        String sCmd = String.format(" %s: \n cd %s \n svn up \n pause \n exit", sDir, sSvn);
        try {
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(new File(sSvnBat)), "GBK");
            osw.write(sCmd);
            osw.close();
        } catch(UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch(FileNotFoundException e1) {
            e1.printStackTrace();
        } catch(IOException e1) {
            e1.printStackTrace();
        }
        File fSvn = new File(sSvn);
        if(fSvn.exists() && fSvn.isDirectory()) {

            try {
                Runtime.getRuntime().exec("cmd.exe /c start " + sSvnBat);
            } catch(IOException err) {
                err.printStackTrace();
            }

        }

    }

    private void btn_CreateSh3ActionPerformed(ActionEvent e) {
        // TODO add your code here
        // 上传版本到服务器

    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        tabbedPane1 = new JTabbedPane();
        panel2 = new JPanel();
        btn_choosexls = new JButton();
        t_ExcelPath = new JTextField();
        btn_EditXLS = new JButton();
        btn_CreateVer2 = new JButton();
        t_VerResult = new JTextField();
        t_VerPathLast = new JTextField();
        btn_choosepath = new JButton();
        btn_CreateSh = new JButton();
        t_SvnPath = new JTextField();
        label12 = new JLabel();
        tSrcIP = new JTextField();
        label16 = new JLabel();
        tDstIP = new JTextField();
        btn_ChooseSvn = new JButton();
        textField1 = new JTextField();
        btn_CreateSh2 = new JButton();
        btn_ChooseSvn2 = new JButton();
        panel1 = new JPanel();
        scrollPane1 = new JScrollPane();
        textArea1 = new JTextArea();
        list_Env = new JComboBox();
        btn_ = new JButton();
        label7 = new JLabel();
        t_HeadLen = new JTextField();
        label8 = new JLabel();
        t_RevLen = new JTextField();
        label1 = new JLabel();
        t_IP = new JTextField();
        label2 = new JLabel();
        t_PORT = new JTextField();
        btn_TCP = new JButton();
        btn_HTTP = new JButton();
        t_Address = new JTextField();
        label3 = new JLabel();
        label9 = new JLabel();
        t_Times = new JTextField();
        label4 = new JLabel();
        scrollPane2 = new JScrollPane();
        t_AreaRsp = new JTextArea();
        label10 = new JLabel();
        t_TimeOut = new JTextField();
        panel3 = new JPanel();
        label11 = new JLabel();
        tf_XlsDict = new JTextField();
        tf_ShtCh = new JTextField();
        tf_XlsCh = new JTextField();
        label13 = new JLabel();
        tf_ShtDict = new JTextField();
        label14 = new JLabel();
        tf_XlsOut = new JTextField();
        label15 = new JLabel();
        tf_ShtOut = new JTextField();
        btn_dcit = new JButton();
        btn_ch = new JButton();
        btn_Convert = new JButton();

        //======== this ========
        setTitle("ESB_Tools");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                thisWindowClosed(e);
            }
        });
        Container contentPane = getContentPane();

        //======== tabbedPane1 ========
        {

            //======== panel2 ========
            {

                //---- btn_choosexls ----
                btn_choosexls.setText("0-\u9009\u62e9\u7248\u672c\u6e05\u5355excel");
                btn_choosexls.setHorizontalAlignment(SwingConstants.LEFT);
                btn_choosexls.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_choosexlsActionPerformed(e);
                    }
                });

                //---- t_ExcelPath ----
                t_ExcelPath.setEditable(false);
                t_ExcelPath.setForeground(Color.black);
                t_ExcelPath.setBorder(new EtchedBorder(EtchedBorder.RAISED, Color.pink, null));

                //---- btn_EditXLS ----
                btn_EditXLS.setText("1-\u7f16\u8f91\u7248\u672c\u6e05\u5355");
                btn_EditXLS.setHorizontalAlignment(SwingConstants.LEFT);
                btn_EditXLS.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_EditXLSActionPerformed(e);
                    }
                });

                //---- btn_CreateVer2 ----
                btn_CreateVer2.setText("4-\u6253\u5305\u5168\u91cf\u7248\u672c");
                btn_CreateVer2.setHorizontalAlignment(SwingConstants.LEFT);
                btn_CreateVer2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_CreateVer2ActionPerformed(e);
                    }
                });

                //---- t_VerResult ----
                t_VerResult.setBorder(new EtchedBorder(Color.green, null));
                t_VerResult.setEditable(false);

                //---- t_VerPathLast ----
                t_VerPathLast.setBorder(new EtchedBorder(Color.green, null));
                t_VerPathLast.setBackground(new Color(204, 255, 204));

                //---- btn_choosepath ----
                btn_choosepath.setText("5-\u9009\u62e9\u6700\u7ec8\u7248\u672c\u7684\u8def\u5f84");
                btn_choosepath.setActionCommand("3-\u9009\u62e9\u7248\u672c\u6587\u4ef6\u5939");
                btn_choosepath.setHorizontalAlignment(SwingConstants.LEFT);
                btn_choosepath.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_choosepathActionPerformed(e);
                    }
                });

                //---- btn_CreateSh ----
                btn_CreateSh.setText("6-\u751f\u6210\u6700\u7ec8\u7248\u672c\u7684\u811a\u672c");
                btn_CreateSh.setHorizontalAlignment(SwingConstants.LEFT);
                btn_CreateSh.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_CreateShActionPerformed(e);
                    }
                });

                //---- t_SvnPath ----
                t_SvnPath.setBorder(new EtchedBorder(Color.green, null));
                t_SvnPath.setEditable(false);

                //---- label12 ----
                label12.setText("\u6e90\u7248\u672c\u6240\u5728\u670d\u52a1\u5668:");

                //---- tSrcIP ----
                tSrcIP.setBorder(new EtchedBorder(Color.green, null));

                //---- label16 ----
                label16.setText("\u76ee\u6807\u7248\u672c\u6240\u5728\u670d\u52a1\u5668:");

                //---- tDstIP ----
                tDstIP.setBorder(new EtchedBorder(Color.green, null));

                //---- btn_ChooseSvn ----
                btn_ChooseSvn.setText("2-\u9009\u62e9\u672c\u5730SVN\u76ee\u5f55");
                btn_ChooseSvn.setHorizontalAlignment(SwingConstants.LEFT);
                btn_ChooseSvn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_ChooseSvnActionPerformed(e);
                    }
                });

                //---- textField1 ----
                textField1.setText("\u300b\u300b\u300b\u300b\u300b");
                textField1.setEditable(false);
                textField1.setBorder(null);

                //---- btn_CreateSh2 ----
                btn_CreateSh2.setText("\u4fee\u6539\u9ed8\u8ba4\u914d\u7f6e");
                btn_CreateSh2.setBorder(new BevelBorder(BevelBorder.LOWERED, Color.red, Color.red, Color.red, Color.red));
                btn_CreateSh2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_CreateSh2ActionPerformed(e);
                    }
                });

                //---- btn_ChooseSvn2 ----
                btn_ChooseSvn2.setText("3-\u66f4\u65b0\u672c\u5730SVN");
                btn_ChooseSvn2.setHorizontalAlignment(SwingConstants.LEFT);
                btn_ChooseSvn2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_ChooseSvn2ActionPerformed(e);
                    }
                });

                GroupLayout panel2Layout = new GroupLayout(panel2);
                panel2.setLayout(panel2Layout);
                panel2Layout.setHorizontalGroup(
                    panel2Layout.createParallelGroup()
                        .addGroup(panel2Layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(panel2Layout.createParallelGroup()
                                .addGroup(panel2Layout.createSequentialGroup()
                                    .addGroup(panel2Layout.createParallelGroup()
                                        .addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(btn_EditXLS, GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
                                            .addComponent(btn_choosexls, GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE))
                                        .addComponent(btn_ChooseSvn, GroupLayout.PREFERRED_SIZE, 170, GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(t_SvnPath, GroupLayout.DEFAULT_SIZE, 481, Short.MAX_VALUE)
                                        .addComponent(t_ExcelPath, GroupLayout.DEFAULT_SIZE, 481, Short.MAX_VALUE)))
                                .addGroup(panel2Layout.createSequentialGroup()
                                    .addComponent(btn_choosepath, GroupLayout.PREFERRED_SIZE, 170, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(t_VerPathLast, GroupLayout.PREFERRED_SIZE, 483, GroupLayout.PREFERRED_SIZE))
                                .addGroup(panel2Layout.createSequentialGroup()
                                    .addGap(15, 15, 15)
                                    .addComponent(label12)
                                    .addGap(18, 18, 18)
                                    .addComponent(tSrcIP, GroupLayout.PREFERRED_SIZE, 124, GroupLayout.PREFERRED_SIZE)
                                    .addGap(33, 33, 33)
                                    .addComponent(textField1, GroupLayout.PREFERRED_SIZE, 79, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(label16)
                                    .addGap(18, 18, 18)
                                    .addComponent(tDstIP, GroupLayout.PREFERRED_SIZE, 124, GroupLayout.PREFERRED_SIZE))
                                .addGroup(panel2Layout.createSequentialGroup()
                                    .addComponent(btn_CreateVer2, GroupLayout.PREFERRED_SIZE, 170, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(t_VerResult, GroupLayout.PREFERRED_SIZE, 483, GroupLayout.PREFERRED_SIZE))
                                .addComponent(btn_ChooseSvn2, GroupLayout.PREFERRED_SIZE, 170, GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn_CreateSh)
                                .addComponent(btn_CreateSh2, GroupLayout.PREFERRED_SIZE, 170, GroupLayout.PREFERRED_SIZE))
                            .addGap(0, 20, Short.MAX_VALUE))
                );
                panel2Layout.setVerticalGroup(
                    panel2Layout.createParallelGroup()
                        .addGroup(panel2Layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(btn_choosexls, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(t_ExcelPath, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btn_EditXLS, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(t_SvnPath, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn_ChooseSvn, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(btn_ChooseSvn2, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                            .addGap(14, 14, 14)
                            .addGroup(panel2Layout.createParallelGroup()
                                .addGroup(GroupLayout.Alignment.TRAILING, panel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(tSrcIP, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(textField1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label16)
                                    .addComponent(tDstIP, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addComponent(label12, GroupLayout.Alignment.TRAILING))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(btn_CreateVer2, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(t_VerResult, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(btn_choosepath, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(t_VerPathLast, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btn_CreateSh, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(btn_CreateSh2, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                            .addContainerGap(147, Short.MAX_VALUE))
                );
            }
            tabbedPane1.addTab("ESB\u7248\u672c\u6253\u5305", panel2);

            //======== panel1 ========
            {

                //======== scrollPane1 ========
                {

                    //---- textArea1 ----
                    textArea1.setText("\u73af\u5883\u53c2\u6570\uff1a");
                    textArea1.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
                    textArea1.setEditable(false);
                    textArea1.setAutoscrolls(false);
                    textArea1.setBorder(new EtchedBorder(Color.cyan, null));
                    scrollPane1.setViewportView(textArea1);
                }

                //---- list_Env ----
                list_Env.setModel(new DefaultComboBoxModel(new String[] {
                    "\u5f00\u53d1\u73af\u5883",
                    "\u6d4b\u8bd5\u73af\u5883",
                    "\u56de\u5f52\u73af\u5883"
                }));
                list_Env.setBorder(new EtchedBorder(Color.magenta, null));
                list_Env.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        list_EnvActionPerformed(e);
                    }
                });

                //---- btn_ ----
                btn_.setText("\u7f16\u8f91\u8bf7\u6c42\u62a5\u6587");
                btn_.setHorizontalAlignment(SwingConstants.LEFT);
                btn_.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_ActionPerformed(e);
                    }
                });

                //---- label7 ----
                label7.setText("\u8bf7\u6c42\u62a5\u6587\u5934\u957f\u5ea6:");
                label7.setBorder(new EtchedBorder(Color.cyan, null));

                //---- t_HeadLen ----
                t_HeadLen.setText("8");
                t_HeadLen.setBorder(new EtchedBorder(Color.magenta, null));

                //---- label8 ----
                label8.setText("\u63a5\u6536\u62a5\u6587\u5934\u957f\u5ea6:");
                label8.setBorder(new EtchedBorder(Color.cyan, null));

                //---- t_RevLen ----
                t_RevLen.setText("8");
                t_RevLen.setBorder(new EtchedBorder(Color.magenta, null));

                //---- label1 ----
                label1.setText("TCP_IP:");
                label1.setBorder(new EtchedBorder(Color.cyan, null));

                //---- t_IP ----
                t_IP.setText("10.4.16.231");
                t_IP.setBorder(new EtchedBorder(Color.magenta, null));

                //---- label2 ----
                label2.setText("TCP_PORT:");
                label2.setBorder(new EtchedBorder(Color.cyan, null));

                //---- t_PORT ----
                t_PORT.setBorder(new EtchedBorder(Color.magenta, null));

                //---- btn_TCP ----
                btn_TCP.setText("\u53d1\u9001TCP");
                btn_TCP.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_TCPActionPerformed(e);
                    }
                });

                //---- btn_HTTP ----
                btn_HTTP.setText("\u53d1\u9001HTTP");
                btn_HTTP.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_HTTPActionPerformed(e);
                    }
                });

                //---- t_Address ----
                t_Address.setText("http://10.4.16.231:38080/***");
                t_Address.setBorder(new EtchedBorder(Color.magenta, null));

                //---- label3 ----
                label3.setText("HTTP_ADD:");
                label3.setBorder(new EtchedBorder(Color.cyan, null));

                //---- label9 ----
                label9.setText("\u5faa\u73af\u53d1\u9001\u6b21\u6570\uff1a");
                label9.setBorder(new EtchedBorder(Color.cyan, null));

                //---- t_Times ----
                t_Times.setText("1");
                t_Times.setBorder(new EtchedBorder(Color.magenta, null));

                //---- label4 ----
                label4.setText("\u8fd4\u56de\u62a5\u6587\uff1a");
                label4.setBorder(new EtchedBorder(Color.cyan, null));

                //======== scrollPane2 ========
                {

                    //---- t_AreaRsp ----
                    t_AreaRsp.setBorder(new EtchedBorder(Color.pink, null));
                    scrollPane2.setViewportView(t_AreaRsp);
                }

                //---- label10 ----
                label10.setText("\u63a5\u53d7\u8d85\u65f6\u65f6\u95f4:");
                label10.setBorder(new EtchedBorder(Color.cyan, null));

                //---- t_TimeOut ----
                t_TimeOut.setText("60000");
                t_TimeOut.setBorder(new EtchedBorder(Color.magenta, null));

                GroupLayout panel1Layout = new GroupLayout(panel1);
                panel1.setLayout(panel1Layout);
                panel1Layout.setHorizontalGroup(
                    panel1Layout.createParallelGroup()
                        .addGroup(panel1Layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(panel1Layout.createParallelGroup()
                                .addGroup(panel1Layout.createSequentialGroup()
                                    .addComponent(label3, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(t_Address, GroupLayout.PREFERRED_SIZE, 314, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(panel1Layout.createParallelGroup()
                                        .addComponent(btn_HTTP, GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE)
                                        .addComponent(btn_TCP, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGroup(panel1Layout.createSequentialGroup()
                                    .addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 103, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(list_Env, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 102, Short.MAX_VALUE)
                                    .addComponent(btn_, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(label9)
                                    .addGap(27, 27, 27)
                                    .addComponent(t_Times, GroupLayout.PREFERRED_SIZE, 56, GroupLayout.PREFERRED_SIZE)
                                    .addGap(35, 35, 35))
                                .addGroup(panel1Layout.createSequentialGroup()
                                    .addComponent(label4, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(scrollPane2, GroupLayout.DEFAULT_SIZE, 599, Short.MAX_VALUE))
                                .addGroup(panel1Layout.createSequentialGroup()
                                    .addGroup(panel1Layout.createParallelGroup()
                                        .addGroup(panel1Layout.createSequentialGroup()
                                            .addComponent(label1, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
                                            .addGap(18, 18, 18)
                                            .addComponent(t_IP, GroupLayout.PREFERRED_SIZE, 99, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panel1Layout.createSequentialGroup()
                                            .addComponent(label7, GroupLayout.PREFERRED_SIZE, 103, GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(t_HeadLen, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)))
                                    .addGap(18, 18, 18)
                                    .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addGroup(panel1Layout.createSequentialGroup()
                                            .addComponent(label2, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(t_PORT, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panel1Layout.createSequentialGroup()
                                            .addComponent(label8)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(t_RevLen, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(label10)
                                    .addGap(18, 18, 18)
                                    .addComponent(t_TimeOut, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 164, Short.MAX_VALUE)))
                            .addContainerGap())
                );
                panel1Layout.setVerticalGroup(
                    panel1Layout.createParallelGroup()
                        .addGroup(GroupLayout.Alignment.TRAILING, panel1Layout.createSequentialGroup()
                            .addGap(14, 14, 14)
                            .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(t_Times, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(label9, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btn_, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(list_Env, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label7, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(t_HeadLen, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(label8, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(t_RevLen, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(label10, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(t_TimeOut, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                            .addGap(6, 6, 6)
                            .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label1, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(t_IP, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(label2, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn_TCP, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(t_PORT, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                            .addGap(7, 7, 7)
                            .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label3, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(t_Address, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn_HTTP, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                            .addGap(18, 18, 18)
                            .addGroup(panel1Layout.createParallelGroup()
                                .addComponent(label4)
                                .addComponent(scrollPane2, GroupLayout.PREFERRED_SIZE, 272, GroupLayout.PREFERRED_SIZE))
                            .addContainerGap(28, Short.MAX_VALUE))
                );
            }
            tabbedPane1.addTab("ESB\u62a5\u6587\u6d4b\u8bd5", panel1);

            //======== panel3 ========
            {

                //---- label11 ----
                label11.setText("\u6e90\u5b57\u6bb5sheet\u9875");
                label11.setBorder(new EtchedBorder(Color.cyan, null));

                //---- tf_XlsDict ----
                tf_XlsDict.setText("D:/\u6cf0\u9686_\u6570\u636e\u5b57\u5178.xlsx");

                //---- tf_ShtCh ----
                tf_ShtCh.setText("dict_ch");

                //---- tf_XlsCh ----
                tf_XlsCh.setText("D:/Dict_CH.xlsx");

                //---- label13 ----
                label13.setText("\u4e2d\u6587\u5b57\u6bb5sheet\u9875");
                label13.setBorder(new EtchedBorder(Color.cyan, null));

                //---- tf_ShtDict ----
                tf_ShtDict.setText("\u88682\u4e2d\u82f1\u6587\u540d\u79f0\u53ca\u7f29\u5199\u5bf9\u7167\u8868");

                //---- label14 ----
                label14.setText("\u7ed3\u679c\u6587\u4ef6\u5b58\u653e\u8def\u5f84");
                label14.setBorder(new EtchedBorder(Color.cyan, null));

                //---- tf_XlsOut ----
                tf_XlsOut.setText("D:/Dict_EN.xlsx");

                //---- label15 ----
                label15.setText("\u7ed3\u679c\u8868\u540d");
                label15.setBorder(new EtchedBorder(Color.cyan, null));

                //---- tf_ShtOut ----
                tf_ShtOut.setText("dict_en");

                //---- btn_dcit ----
                btn_dcit.setText("\u6cf0\u9686\u6570\u636e\u5b57\u5178");
                btn_dcit.setHorizontalAlignment(SwingConstants.LEFT);
                btn_dcit.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_dcitChoose(e);
                    }
                });

                //---- btn_ch ----
                btn_ch.setText("\u4e2d\u6587\u5b57\u6bb5\u6587\u4ef6");
                btn_ch.setHorizontalAlignment(SwingConstants.LEFT);
                btn_ch.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_chChoose(e);
                    }
                });

                //---- btn_Convert ----
                btn_Convert.setText("\u8f6c\u6362");
                btn_Convert.setHorizontalAlignment(SwingConstants.LEFT);
                btn_Convert.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btn_ConvertAction(e);
                    }
                });

                GroupLayout panel3Layout = new GroupLayout(panel3);
                panel3.setLayout(panel3Layout);
                panel3Layout.setHorizontalGroup(
                    panel3Layout.createParallelGroup()
                        .addGroup(panel3Layout.createSequentialGroup()
                            .addGroup(panel3Layout.createParallelGroup()
                                .addGroup(panel3Layout.createSequentialGroup()
                                    .addGap(14, 14, 14)
                                    .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(label14, GroupLayout.PREFERRED_SIZE, 128, GroupLayout.PREFERRED_SIZE)
                                        .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                            .addComponent(label13, GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
                                            .addComponent(btn_ch)
                                            .addComponent(btn_dcit)
                                            .addComponent(label11, GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE))
                                        .addComponent(label15, GroupLayout.PREFERRED_SIZE, 128, GroupLayout.PREFERRED_SIZE))
                                    .addGap(18, 18, 18)
                                    .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(tf_XlsDict, GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
                                        .addComponent(tf_XlsCh, GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
                                        .addComponent(tf_ShtDict, GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tf_ShtCh, GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tf_ShtOut, GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tf_XlsOut, GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)))
                                .addGroup(panel3Layout.createSequentialGroup()
                                    .addGap(265, 265, 265)
                                    .addComponent(btn_Convert)))
                            .addContainerGap(31, Short.MAX_VALUE))
                );
                panel3Layout.setVerticalGroup(
                    panel3Layout.createParallelGroup()
                        .addGroup(panel3Layout.createSequentialGroup()
                            .addGap(15, 15, 15)
                            .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(btn_dcit, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE)
                                .addComponent(tf_XlsDict, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label11, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(tf_ShtDict, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(btn_ch, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE)
                                .addComponent(tf_XlsCh, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label13, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(tf_ShtCh, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addGap(7, 7, 7)
                            .addComponent(btn_Convert, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label14, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(tf_XlsOut, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label15, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(tf_ShtOut, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                            .addContainerGap(198, Short.MAX_VALUE))
                );
            }
            tabbedPane1.addTab("ESB\u5b57\u5178\u5de5\u5177", panel3);
        }

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(tabbedPane1, GroupLayout.PREFERRED_SIZE, 693, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(230, Short.MAX_VALUE))
        );
        contentPaneLayout.setVerticalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addComponent(tabbedPane1, GroupLayout.PREFERRED_SIZE, 486, GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 169, Short.MAX_VALUE))
        );
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTabbedPane tabbedPane1;
    private JPanel panel2;
    private JButton btn_choosexls;
    private JTextField t_ExcelPath;
    private JButton btn_EditXLS;
    private JButton btn_CreateVer2;
    private JTextField t_VerResult;
    private JTextField t_VerPathLast;
    private JButton btn_choosepath;
    private JButton btn_CreateSh;
    private JTextField t_SvnPath;
    private JLabel label12;
    private JTextField tSrcIP;
    private JLabel label16;
    private JTextField tDstIP;
    private JButton btn_ChooseSvn;
    private JTextField textField1;
    private JButton btn_CreateSh2;
    private JButton btn_ChooseSvn2;
    private JPanel panel1;
    private JScrollPane scrollPane1;
    private JTextArea textArea1;
    private JComboBox list_Env;
    private JButton btn_;
    private JLabel label7;
    private JTextField t_HeadLen;
    private JLabel label8;
    private JTextField t_RevLen;
    private JLabel label1;
    private JTextField t_IP;
    private JLabel label2;
    private JTextField t_PORT;
    private JButton btn_TCP;
    private JButton btn_HTTP;
    private JTextField t_Address;
    private JLabel label3;
    private JLabel label9;
    private JTextField t_Times;
    private JLabel label4;
    private JScrollPane scrollPane2;
    private JTextArea t_AreaRsp;
    private JLabel label10;
    private JTextField t_TimeOut;
    private JPanel panel3;
    private JLabel label11;
    private JTextField tf_XlsDict;
    private JTextField tf_ShtCh;
    private JTextField tf_XlsCh;
    private JLabel label13;
    private JTextField tf_ShtDict;
    private JLabel label14;
    private JTextField tf_XlsOut;
    private JLabel label15;
    private JTextField tf_ShtOut;
    private JButton btn_dcit;
    private JButton btn_ch;
    private JButton btn_Convert;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}


