package Client;


import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.HashMap;

/**
 * Created by guoxj on 2017/2/13.
 */
public class ConvertDict {

    @SuppressWarnings("static-access")
    public static String getValue(XSSFCell xssfRow) {
        if (xssfRow.getCellType() == xssfRow.CELL_TYPE_BOOLEAN) {
            return String.valueOf(xssfRow.getBooleanCellValue());
        } else if (xssfRow.getCellType() == xssfRow.CELL_TYPE_STRING){
            return xssfRow.getStringCellValue();
        }
        else {
            return "*";
        }
    }

    // 读取 xls 中的一个sht页的指定行列内容
    public static HashMap ReadOneSheet(XSSFSheet sht, int[] iRowSE, int[] iColSE) {
//        sXls = "D:/SVN/ESB_DOC/配合核心信贷ESB升级改造/04.需求管理/服务定义/泰隆_数据字典.xlsx";
//        sSheet = "表2中英文名称及缩写对照表";
//        iRowSE[0] = 1;
//        iRowSE[1] = 999999;
//        iColSE[0] = 0;
//        iColSE[1] = 999999;

        HashMap hmSht = new HashMap();
        int iMap = 0;
        int iRow = sht.getLastRowNum() > iRowSE[1] ? iRowSE[1] : sht.getLastRowNum();
//        int iCol = sht.getRow(0).getLastCellNum() > iColSE[1] ? iColSE[1] : sht.getRow(0).getLastCellNum();
        int iCol = iColSE[1];
        System.out.printf("=====读取Sheet(%s) 开始, 预计获取列数(%d,%d)！=====\n\n", sht.getSheetName(), iRow, iCol);
        for(int row = iRowSE[0]; row <= iRow; row++) {
            System.out.printf("ReadOneSheet(%d) ", row);
            String[] asRow = new String[iCol];
            for(int col = iColSE[0]; col < iCol; col++) {
                try {
                    asRow[col] = getValue(sht.getRow(row).getCell(col));
                    System.out.printf("%s, ", asRow[col]);
                } catch(Exception e){
                    System.out.printf("Read Error (%d, %d)\n", row, col);
                    asRow[col] = "*";
                }
            }
            System.out.printf("\n");
            hmSht.put(iMap, asRow);
            iMap++;
        }
        System.out.printf("=====读取Sheet(%s) 成功, 总共获取列数(%d)！=====\n\n", sht.getSheetName(), hmSht.size());
        return hmSht;
    }

    // 写 xls 中的一个sht页的指定行列内容
    public static void WriteOneSheet(SXSSFWorkbook wwb, String sSht, HashMap hmSht, int iRowS) {
        SXSSFSheet wsht = wwb.createSheet(sSht);
        SXSSFRow row =null;
        System.out.printf("=====写Sheet(%s) 开始, 预计写列数(%d)！=====\n\n", sSht, hmSht.size());
        int iMap = 0;
        for(;iMap < hmSht.size(); iMap++) {
            String[] asRow = (String[]) hmSht.get(iMap);
            row = (SXSSFRow) wsht.createRow(iMap);

            System.out.printf("WriteOneSheet(%d) ", iRowS);
            for(int col = 0; col < asRow.length; col++) {
                row.createCell(col).setCellValue(asRow[col]);
                System.out.printf("%s, ", asRow[col]);
            }
            System.out.printf("\n");
            row = null;
            iRowS++;
        }
        System.out.printf("=====写Sheet(%s) 开始, 写列数(%d)！=====\n\n", sSht, iMap);
    }

    // 获取“表2中英文名称及缩写对照表” 数据
    public static HashMap GetDictSrc(String sXls, String sSheet) {
        HashMap hmSht = new HashMap();
        //
//        sXls = "D:/SVN/ESB_DOC/配合核心信贷ESB升级改造/04.需求管理/服务定义/泰隆_数据字典.xlsx";
//        sSheet = "表2中英文名称及缩写对照表";
        int[] iRowSE = {1, 999999};
        int[] iColSE = {0, 3};
        try {
            InputStream stream = new FileInputStream(sXls);
            XSSFWorkbook wb = new XSSFWorkbook(stream);
            XSSFSheet sht = wb.getSheet(sSheet);
            if(null == sht){
                System.out.printf("[GetDictSrc] Excel(%s) 中不存在Sheet页(%s)!\n", sXls, sSheet);
                stream.close();
                wb.close();
            }

            HashMap temp = ReadOneSheet(sht, iRowSE, iColSE);
            for(int i = 0; i < temp.size(); i++) {
                String[] asRow = (String[]) temp.get(i);
                hmSht.put(asRow[0], asRow);
            }
            stream.close();
            wb.close();
        } catch(IOException e) {
        }
        return hmSht;
    }

    public static void ConvertCh2En(String sDictXls, String sDicSheet, String sChXls, String sChSheet,
                                    String sOutXls, String sOutSheet) {
        System.out.println("泰隆数据字典：" + sDictXls);
        System.out.println("泰隆数据字典-sheet页：" + sDicSheet);
        System.out.println("中文字段文件：" + sChXls);
        System.out.println("中文字段-sheet页：" + sChSheet);
        System.out.println("结果文件：" + sOutXls);
        System.out.println("结果文件-sheet页：" + sOutSheet);

        HashMap hmDict = GetDictSrc(sDictXls, sDicSheet);

        XSSFWorkbook wb = null;
        SXSSFWorkbook wwb = null;
        try {
            InputStream stream = new FileInputStream(sChXls);
            wb = new XSSFWorkbook(stream);
            XSSFSheet shtCh = wb.getSheet(sChSheet);
            int[] iRowSE = {1, 999999};
            int[] iColSE = {0, 2};

            if(null == shtCh){
                System.out.printf("[ConvertCh2En] Excel(%s) 中不存在Sheet页(%s)!\n", sChXls, sChSheet);
                stream.close();
                wb.close();
            }
            HashMap hmCh = ReadOneSheet(shtCh, iRowSE, iColSE);
            HashMap hmOut = new HashMap();
            for(int i = 0; i < hmCh.size(); i++) {
                // 0-类型 1-中文
                String[] asRowCh = (String[]) hmCh.get(i);
                System.out.printf("hmCh.get(%d): %s\n", i, asRowCh[1]);
                if(asRowCh[1].length() < 1) {
                    continue;
                }

                String[] asChWord = asRowCh[1].split("-");
                String sEnWord = "";
                String sEnFullWord = "";
                String sChWord = asRowCh[1].replace("-", "");
                String sType = asRowCh[0];
                for(int j = 0; j < asChWord.length; j++) {
                    // 最后一个字段不需要拼接 "_"
                    String sCon = "_";
                    if(j == (asChWord.length-1)) {
                        sCon = "";
                    }

                    if(hmDict.containsKey(asChWord[j])) {
                        sEnWord += (((String[]) hmDict.get(asChWord[j]))[2] + sCon);
                        sEnFullWord += (((String[]) hmDict.get(asChWord[j]))[1] + sCon);
                    } else {
                        sEnWord = sEnWord + "*" + sCon;
                        sEnFullWord = sEnFullWord+ "*" + sCon;
                    }
                }

                // 0-缩写英文 1-类型 2-中文 3-全写英文
                String[] asOutRow = {sEnWord, sType, sChWord, sEnFullWord};
                System.out.println(sChWord + " -> " + sEnWord + " " + sEnFullWord);
                hmOut.put(i, asOutRow);
            }
            System.out.printf("=====解析中文字段成功！=====\n");

            OutputStream ostream = new FileOutputStream(sOutXls);
            wwb = new SXSSFWorkbook();
            WriteOneSheet(wwb, sOutSheet, hmOut, 1);
            wwb.write(ostream);
            ostream.close();
            stream.close();
            System.out.printf("=====报存结果文件结束！=====\n");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
