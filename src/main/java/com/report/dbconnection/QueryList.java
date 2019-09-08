package com.report.dbconnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

public class QueryList {

	final static Logger logger = Logger.getLogger(QueryList.class);
	StringBuffer sb = null;

	public String getSqlQuery(String name, String inputPath) {

		File directory = new File(inputPath);

		// File directory = new
		// File("Y:\\Groups\\Fleet Services\\Fleet FASB Query Output\\Query Scripts");

		for (File file : directory.listFiles()) {
			String fileName = file.getName();
			if (fileName.equals(name + ".txt")
					|| fileName.equals(name + ".sql")) {

				try {

					BufferedReader sqlQuery = new BufferedReader(new FileReader(
							inputPath + fileName));

					String line;
					line = sqlQuery.readLine();
					sb = new StringBuffer();
					String[] commentedItems;

					while (line != null) {

						
						commentedItems = line.split("--");
						if (commentedItems.length > 0) {
							sb = sb.append(commentedItems[0]).append(" ");
						} else {
							sb = sb.append(line).append(" ");
						}

						line = sqlQuery.readLine();
					}
					
					
				} catch (IOException e) {
					logger.error(e.getMessage());
				}

			}
		}

		if (sb != null) {
			return sb.toString();
		} else
			return "";

	}

}
