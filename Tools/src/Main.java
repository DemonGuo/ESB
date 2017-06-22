import Client.TestESB;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
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
}
