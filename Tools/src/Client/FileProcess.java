package Client;

import com.jcraft.jsch.*;

import javax.swing.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.Vector;

/**
 * Created by guoxj on 2017/5/27.
 */
public class FileProcess {
    private ChannelSftp sftp = null;
    private Session sshSession = null;

    //拷贝本地文件
    public static Boolean CopySingleFile(String sSrc, String sDst) throws IOException {
        File fSrc = new File(sSrc);
        if(!fSrc.exists()) {
            System.out.printf("源文件不存在[%s], 请确定svn版本是否正确.\n", fSrc.getAbsolutePath());
            return false;
        }

        File fDst = new File(sDst);
        if(fDst.exists()) {
            return true;
        }

        if(!fDst.getParentFile().exists()) {
            //目录不存在，创建目录
            if(!fDst.getParentFile().mkdirs()) {
                System.out.println("创建目录失败 " + fDst.getParentFile());
                return false;
            }
        }
        // 创建文件
        if(fDst.createNewFile()) {
            //创建文件成功, 开始拷贝文件
            System.out.println("创建文件成功 " + fDst.getName());
            //单线程文件复制最快的方法
            FileChannel in = null;
            FileChannel out = null;
            FileInputStream inStream = null;
            FileOutputStream outStream = null;
            try {
                inStream = new FileInputStream(fSrc);
                outStream = new FileOutputStream(fDst);
                in = inStream.getChannel();
                out = outStream.getChannel();
                in.transferTo(0, in.size(), out);
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                inStream.close();
                in.close();
                outStream.close();
                out.close();
            }
        } else {
            System.out.println("创建文件失败 " + fDst.getName());
            return false;
        }
        return true;
    }

    //拷贝服务器文件
    public static Boolean CopySingleFile(String sSrc, String sDst, ChannelSftp sFtp) {

        File fSrc = new File(sSrc);
        File fDst = new File(sDst);
        try {
            String directory = fSrc.getParent().replace("\\", "/");
            sFtp.cd(directory);
            if(!fDst.getParentFile().exists()) {
                // System.out.printf("创建目录：%s\n", file.getParentFile().toString());
                fDst.getParentFile().mkdirs();
            }

            Vector vt = sFtp.ls(fSrc.getName());
            if(!vt.isEmpty()) {
                System.out.printf("[ 下载 ] 开始下载文件 %s\n", new File(sSrc).getName());
                OutputStream outputStream = new FileOutputStream(fDst);
                sFtp.get(sSrc, outputStream);
                outputStream.flush();
                outputStream.close();
            }
            return true;

        } catch(Exception e) {
            // e.printStackTrace();
            return false;
        } finally {
            //            fos.close();
            //            if(file.isFile() && file.delete()){
            //                System.out.printf("[ 删除 ] 失败的空文件 %s\n", file.getAbsolutePath());
            //            }
        }
    }

}

//此类暂时没有使用
class SSHToESB {
    // 服务器ip，端口，用户名称，密码, 服务器类型
    private String ip;
    private int port;
    private String user;
    private String passwd;
    private String env;
    private ChannelSftp sftp;
    private Session sshSession;

    // 初始化
    public void Init(String ip, int port, String user, String passwd, String env) {
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.passwd = passwd;
        this.env = env;
    }

    public boolean ConnectToServer() {
        ChannelSftp sftp = null;
        try {
            JSch jsch = new JSch();
            System.out.println("\n开始建立远程Sftp服务.Start to Create Session......");
            jsch.getSession(user, ip, port);
            sshSession = jsch.getSession(user, ip, port);
            sshSession.setPassword(passwd);
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(sshConfig);
            sshSession.connect();
            //            System.out.println("远程会话Session connected.");
            //            System.out.println("sftp start Opening Channel.");
            Channel channel = sshSession.openChannel("sftp");
            channel.connect();
            this.sftp = (ChannelSftp) channel;
            //            System.out.println("Connected to " + ip + ".");
            return true;
        } catch(Exception e) {
            System.out.printf("连接远程服务器异常.ip=%s,port=%d,user=%s\n", ip, port, user);
            e.printStackTrace();
            return false;
        }

    }

    public void CloseConnection() {
        //关闭ftp链接
        if(this.sftp != null) {
            this.sftp.disconnect();
            this.sftp.exit();
            this.sftp = null;
        }

        //关闭会话
        if(this.sshSession != null) {
            this.sshSession.disconnect();
            this.sshSession = null;
        }
    }

    public boolean DownLoadFile(String remoteFilePath, String localFilePath) throws IOException {
        try {
            String directory = remoteFilePath.substring(0, remoteFilePath.lastIndexOf("/"));
            String downloadFile = remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1, remoteFilePath.length());
            //            System.out.printf("下载目录：%s, 下载文件: %s\n", directory, downloadFile);

            File file = new File(localFilePath);

            sftp.cd(directory);
            if(!file.getParentFile().exists()) {
                //                System.out.printf("创建目录：%s\n", file.getParentFile().toString());
                file.getParentFile().mkdirs();
            }

            Vector vt = sftp.ls(remoteFilePath);
            if(!vt.isEmpty()) {
                System.out.printf("[ 下载 ] 开始下载文件 %s\n", remoteFilePath);
                sftp.get(remoteFilePath, new FileOutputStream(file));
            }
            return true;

        } catch(Exception e) {
            //            e.printStackTrace();
            return false;
        } finally {
            //            fos.close();
            //            if(file.isFile() && file.delete()){
            //                System.out.printf("[ 删除 ] 失败的空文件 %s\n", file.getAbsolutePath());
            //            }
        }
    }

    //参考相关目录，从远程服务器下载的所有文件和目录
    public boolean DownLoanDir(String file, String refPath, String srcPath, String dstPath) throws IOException {
        String remoteFilePath = file.replace(refPath, srcPath);
        String localFilePath = file.replace(refPath, dstPath);

        File refDir = new File(file);
        if(refDir.isDirectory()) {
            String[] children = refDir.list();
            //递归拷贝目录中的子目录下
            for(int i = 0; i < children.length; i++) {
                //                System.out.printf("children[%d] = %s\n", i, file + "/" + children[i]);
                //不论成功失败 都继续下载下一个
                boolean success = DownLoanDir(file + "/" + children[i], refPath, srcPath, dstPath);
            }
        }
        // 目录此时为空,或为文件，可以删除
        if(refDir.isFile()) {
            if(DownLoadFile(remoteFilePath, localFilePath)) {
                System.out.printf("[ 成功！] 从 %s 下载文件 %s, 到 %s\n", this.env, remoteFilePath, localFilePath);
            } else {
                System.out.printf("[ 失败！] 从 %s 下载文件 %s, 到 %s\n", this.env, remoteFilePath, localFilePath);
            }
        }
        return true;
    }
}
