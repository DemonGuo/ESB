package Client;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/7/12.
 */
public class TestProxy {
    // 获取报文内容
    public static byte[] getMsgData(String path) {
        byte[] reqData = null;
        try {
            File file = new File(path);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            int len = (int) file.length();
            reqData = new byte[len];
            bis.read(reqData);
            bis.close();
        } catch(Exception e) {
            System.out.println("获取报文内容出错" + e);
        }
        return reqData;
    }
}

class TestTcp {
    // esb环境地址和端口 （开发、业务测试、回归）
//    private final static String DEV_IP = "10.4.16.231";
//    private final static String UAT_IP = "10.4.24.167";
//    private final static String PDT_IP = "10.4.32.41";
    private String ip;
    private int port;

    // 发送报文头长度
    private int head_len;

    // 接收报文头长度
    private int rev_len;

    // 超时时间
    private int timeout;

    // 测试报文 所在文件路径
    private String msg_xml;

    // 设置开发环境与消费系统对接的tcp/ip端口
    public void setIP(String ip) {
        this.ip = ip;
    }

    // 设置开发环境与消费系统对接的tcp/ip端口
    public void setPort(int port) {
        this.port = port;
    }

    // 设置报文所在路径
    public void setMsgPath(String msg_xml) {
        this.msg_xml = msg_xml;
    }

    // 设置报文头长度
    public void setHead_len(int head_len) {
        this.head_len = head_len;
    }

    // 设置接收报文头长度
    public void setRev_len(int rev_len) {
        this.rev_len = rev_len;
    }

    // 设置报文超时时间
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    // 初始化类各个属性
    public void Init(String ip, int port, int timeout, int head_len, int rev_len, String msg_xml) {
        setIP(ip);
        setPort(port);
        setHead_len(head_len);
        setRev_len(rev_len);
        setTimeout(timeout);
        setMsgPath(msg_xml);
    }

    // 读取制定长度报文
    public byte[] readLenContent(InputStream is, int length)
            throws IOException {

        // 每次最大接收100K的请求报文
        if(length > 102400) {
            return null;
        }

        int count = 0;
        int offset = 0;
        byte[] retData = new byte[length];
        while((count = is.read(retData, offset, length - offset)) != -1) {
            offset += count;
            if(offset == length)
                break;
        }

        return retData;
    }

    // 开发环境 发送TCP请求 获取响应报文
    public String sendTcpRequest() throws IOException {

        Socket sk = new Socket(this.ip, this.port);
        sk.setSoTimeout(this.timeout);

        // 获取输出流,用于发送数据到服务端
//        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sk.getOutputStream()));
        OutputStream out = new BufferedOutputStream(sk.getOutputStream());

        // 获取输入流,用于接收服务端的数据
        InputStream in = new BufferedInputStream(sk.getInputStream());
        byte[] rspData = null;
        try {
            // 获取报文内容 组装报文
            byte[] req = TestProxy.getMsgData(this.msg_xml);
            String req_msg = new String(req, "UTF-8").replaceAll("  ", "").replaceAll("[\\t\\n\\r]", "");

            // 获取报文长度时必须用getBytes 计算字节码长度，不然中文的传输会有问题
            String tlen = String.format("%%0%dd", head_len);
            req_msg = String.format(tlen, req_msg.getBytes().length) + req_msg;
            System.out.println("请求数据[ " + req_msg + " ]");
            // 发送报文
            out.write(req_msg.getBytes());
            out.flush();
            // 接收报文
            byte[] len = readLenContent(in, this.rev_len);
            int length = Integer.parseInt(new String(len, "UTF-8"));
            System.out.println("响应数据长度为[ " + length + " ]");

            rspData = readLenContent(in, length);
            System.out.println("响应数据[" + new String(rspData, "UTF-8").replace("><", ">\n<") + "]");


        } catch(Exception e) {
            e.printStackTrace();
        }

        return new String(rspData, "UTF-8");
    }


}


class TestHttp {
    // http 地址
    private String url;

    // 超时时间
    private int timeout;

    // 报文头长度
    private int headlen;


    public TestHttp(String url, int headlen, int timeout){
        this.url = url;
        this.headlen = headlen;
        this.timeout = timeout;
    }

    public void SetHeadlen(int headlen){
        this.headlen = headlen;
    }

    public void SetTimeout(int timeout){
        this.timeout = timeout;
    }

    public void SetAddress(String url){
        this.url = url;
    }

    public String SendGetReq(){
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            URL realUrl = new URL(this.url);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (Map.Entry<String,List<String>> one: map.entrySet()) {
                System.out.println(one.getKey() + "--->" + one.getValue());
            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result.toString();
    }

    public String SendPostReq(String reqMessage){
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(reqMessage);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result.toString();
    }

}