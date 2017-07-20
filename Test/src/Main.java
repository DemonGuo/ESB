import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public class Main {

    public static void main(String[] args) {
        try {
            TestWriteExcel();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Hello World!");
    }

    public static void TestBigDecimal(){
        String newStr = "0001000000";
        int scale = 3;
        BigDecimal b1 = new BigDecimal(Integer.parseInt(newStr));
        BigDecimal b2 = BigDecimal.valueOf(Math.pow(10.0, scale/1.0));
        System.out.println(b1.toString());
        System.out.println(b2.toString());
        System.out.println(b1.divide(b2).setScale(scale).toString());
    }

    public static void TestReadExcel(){
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        try {
            //同时支持Excel 2003、2007
            File excelFile = new File("/home/zht/test.xls"); //创建文件对象
            FileInputStream is = new FileInputStream(excelFile); //文件流
            Workbook workbook = WorkbookFactory.create(is); //这种方式 Excel 2003/2007/2010 都是可以处理的
            int sheetCount = workbook.getNumberOfSheets();  //Sheet的数量

            //遍历每个Sheet
            for (int s = 0; s < sheetCount; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                int rowCount = sheet.getPhysicalNumberOfRows(); //获取总行数
                //遍历每一行
                for (int r = 0; r < rowCount; r++) {
                    Row row = sheet.getRow(r);
                    int cellCount = row.getPhysicalNumberOfCells(); //获取总列数
                    //遍历每一列
                    for (int c = 0; c < cellCount; c++) {
                        Cell cell = row.getCell(c);
                        int cellType = cell.getCellType();
                        String cellValue = null;
                        switch(cellType) {
                            case Cell.CELL_TYPE_STRING: //文本
                                cellValue = cell.getStringCellValue();
                                break;
                            case Cell.CELL_TYPE_NUMERIC: //数字、日期
                                if(DateUtil.isCellDateFormatted(cell)) {
                                    cellValue = fmt.format(cell.getDateCellValue()); //日期型
                                }
                                else {
                                    cellValue = String.valueOf(cell.getNumericCellValue()); //数字
                                }
                                break;
                            case Cell.CELL_TYPE_BOOLEAN: //布尔型
                                cellValue = String.valueOf(cell.getBooleanCellValue());
                                break;
                            case Cell.CELL_TYPE_BLANK: //空白
                                cellValue = cell.getStringCellValue();
                                break;
                            case Cell.CELL_TYPE_ERROR: //错误
                                cellValue = "错误";
                                break;
                            case Cell.CELL_TYPE_FORMULA: //公式
                                cellValue = "错误";
                                break;
                            default:
                                cellValue = "错误";
                        }
                        System.out.print(cellValue + "    ");
                    }
                    System.out.println();
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void TestWriteExcel() throws IOException {
        XSSFWorkbook xfwb = new XSSFWorkbook();
        XSSFSheet xfSht = xfwb.createSheet("index");
        XSSFSheet xfSht1 = xfwb.createSheet("sht1");
        XSSFSheet xfSht2 = xfwb.createSheet("sht2");

        XSSFCell xfCell = null;

        xfCell = xfSht.createRow(1).createCell(1);
        xfCell.setCellType(XSSFCell.CELL_TYPE_FORMULA);
        xfCell.setCellFormula("HYPERLINK(\"#sht1!A1\", \"link1\")");

        xfCell = xfSht.getRow(1).createCell(2);
        xfCell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
        String value = String.format("HYPERLINK(\"#%s!A1\", \"%s\")", "sht2", "link2");
        xfCell.setCellFormula(value);

        xfCell = xfSht1.createRow(1).createCell(1);
        xfCell.setCellValue("sheet1 row 1 cell1");

        xfCell = xfSht1.createRow(2).createCell(2);
        xfCell.setCellValue("sheet1 row 2 cell2");


        xfCell = xfSht2.createRow(1).createCell(1);
        xfCell.setCellValue("sheet2 row 1 cell1");

        xfCell = xfSht2.createRow(2).createCell(2);
        xfCell.setCellValue("sheet2 row 2 cell2");

        FileOutputStream outputStream = new FileOutputStream("test.xlsx");
        xfwb.write(outputStream);
        outputStream.close();

    }
}
