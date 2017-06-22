package Client;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2016/8/4.
 */
public class LocalConfig {
    private HashMap config;

    public LocalConfig(){
        this.config = new HashMap();
        String configFile = new File("").getAbsolutePath() + "/files/allConfig.properties";
        try {
            FileInputStream fis = new FileInputStream(configFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "GBK"));
            for(String line = br.readLine().trim(); line.trim() != null; line = br.readLine().trim()) {
                if("END".equals(line)) {
                    break;
                }

                if(line.startsWith("#") || "".equals(line)) {
                    continue;
                } else {
                    String key = line.split("=")[0].trim();
                    String value = line.split("=")[1].trim();
//                    System.out.println(key + " = " + value);
                    this.config.put(key, value);
                }
            }

            br.close();

        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // 获取版本比较的相关配置
    public HashMap GetConfig() throws IOException {
        return this.config;
    }

    // 获取服务器连接必要的配置信息（uat 有 40和90 两个, 版本一直默认用40）
    public String[] GetLinuxConfig(String envName) {
        //0-ip  1-user  2-password
        String[] linuxConf = new String[3];

        String key_ip = String.format("esb_linux_%s_ip", envName);
        String key_user = String.format("esb_linux_%s_user", envName);
        String key_passwd = String.format("esb_linux_%s_passwd", envName);

        linuxConf[0] = config.get(key_ip).toString();
        linuxConf[1] = config.get(key_user).toString();
        linuxConf[2] = config.get(key_passwd).toString();

        if((linuxConf[0].length() < 1) || (linuxConf[1].length() < 1) ||
                (linuxConf[2].length() < 1)) {
            System.out.println("配置异常，未获取到相应的配置,请检查配置文件是否有以下配置项：");
            System.out.printf("%s\n%s\n%s\n", key_ip, key_user, key_passwd);
            System.exit(0);
        }

        return linuxConf;
    }

    // 获取相应名称的环境变量的配置信息
    public String GetItemConfig(String item) {
        if(null == config.get(item)){
            return "";
        }
        String itemConfig = (String)config.get(item);

        if(itemConfig.length() < 1) {
            System.out.println("配置异常，未获取到相应的配置,请检查配置文件是否有以下配置项：");
            System.out.printf("%s\n", item);
            System.exit(0);
        }

        return itemConfig;
    }

    // 获取数据连接必要的配置信息
    public String[] GetOralceConfig(String envName) {
        //0-ip  1-user  2-password
        String[] oracleConf = new String[3];

        String key_ip = String.format("esb_oracle_%s_ip", envName);
        String key_user = String.format("esb_oracle_%s_user", envName);
        String key_passwd = String.format("esb_oracle_%s_passwd", envName);

        oracleConf[0] = config.get(key_ip).toString();
        oracleConf[1] = config.get(key_user).toString();
        oracleConf[2] = config.get(key_passwd).toString();

        if((oracleConf[0].length() < 1) || (oracleConf[1].length() < 1) ||
                (oracleConf[2].length() < 1)) {
            System.out.println("配置异常，未获取到相应的配置,请检查配置文件是否有以下配置项：");
            System.out.printf("%s\n%s\n%s\n", key_ip, key_user, key_passwd);
            System.exit(0);
        }

        return oracleConf;
    }
}
