package com.healthcloud.qa.utils;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class DataWriter {

	
	public static void writeData(XSSFSheet comparsionSheet, String result, String iD, String test_case) {
		int lastNum = comparsionSheet.getLastRowNum();
		System.out.println("lastNum:"+lastNum);
		if(0 == lastNum){
			writeSheet(comparsionSheet.createRow(lastNum),"comparsionDetail","ID","TestCase");
			lastNum ++;
		}
		writeSheet(comparsionSheet.createRow(lastNum),result,iD,test_case);
	}
	
	public static void writeSheet(XSSFRow row, String... data){
		for(int i=0;i<data.length;i++){
			row.createCell(i).setCellValue(data[i]);
		}
	}

	public static void writeData(XSSFSheet resultSheet, String string, String iD, String test_case, int i) {
		int lastNum = resultSheet.getLastRowNum();
		writeSheet(resultSheet.createRow(lastNum+1),string,iD,test_case);
	}

	public static void writeData(XSSFSheet comparsionSheet, String string, String iD, String iD2, String test_case) {
		int lastNum = comparsionSheet.getLastRowNum();
		writeSheet(comparsionSheet.createRow(lastNum+1),string,iD,iD2,test_case);
		
	}

	public static void writeData(XSSFSheet resultSheet, double totalcase, double failedcase, String startTime,
			String endTime) {
		
		writeSheet(resultSheet.createRow(1),String.valueOf(totalcase),String.valueOf(failedcase),startTime,endTime);
	}

	

}
