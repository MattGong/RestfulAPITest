package com.healthcloud.qa.utils;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SheetUtils {

	public static void removeSheetByName(XSSFWorkbook wb, String string) {
		XSSFSheet sheet = wb.getSheet(string);
		wb.removeSheetAt(wb.getSheetIndex(sheet));
	}

}
