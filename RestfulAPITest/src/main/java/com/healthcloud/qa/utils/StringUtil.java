package com.healthcloud.qa.utils;

public class StringUtil {
    public static String removeSpaces(String str){
    	return str.replaceAll("[\\s]+", "");
    }
}
