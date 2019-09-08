package com.report.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;

import com.report.dbconnection.DBConnection;
import com.report.dbconnection.QueryList;


public class RunReport {

	final static Logger logger = Logger.getLogger(RunReport.class);
	public static void main(String[] args) throws SQLException, IOException {

		

		FileOutputStream fileOut = null;
		ResultSet rs = null;
		Properties props = new Properties();
		FileInputStream fis;
		String[] queryList = null;
		int reportNum = 1;
		
		
		try {
			String HOME = System.getProperty("user.dir");
			String log4JPropertyFile = HOME+File.separator+"log4j.properties";
			Properties log4J = new Properties();
			
			log4J.load(new FileInputStream(log4JPropertyFile));
		    PropertyConfigurator.configure(log4J);
		    logger.info("Wow! log4j configured!");
		    
			
			logger.debug("Path :"+HOME);
			fis = new FileInputStream(HOME+File.separator+"reportList.properties");
			props.load(fis);
		} catch (FileNotFoundException e1) {

			logger.error("Propperties file not found" + e1.getMessage());
		} catch (IOException e) {

			logger.error("Propperties file not found" + e.getMessage());
		}

		if (null != props.getProperty("dailyReports")) {
			queryList = props.getProperty("dailyReports").split(",");
			logger.info("Started executing daily report batch process................");

		} else if (null != props.getProperty("weeklyReports")) {
			queryList = props.getProperty("weeklyReports").split(",");
			logger.info("Started executing weekly report batch process................");

		} else if (null != props.getProperty("monthEndReportsDay1")) {
			queryList = props.getProperty("monthEndReportsDay1").split(",");
			logger.info("Started executing month end day1 report batch process................");
		} else if (null != props.getProperty("monthEndReportsDay2")) {
			queryList = props.getProperty("monthEndReportsDay2").split(",");
			logger.info("Started executing month end day2 report batch process................");

		} else if (null != props.getProperty("monthEndReportsDay3")) {
			queryList = props.getProperty("monthEndReportsDay3").split(",");
			logger.info("Started executing month end day3 report batch process................");
		} else {
			throw new RuntimeException("No report is configured for execution");
		}

		Connection conn = DBConnection.getConnection();
		if (null == conn) {
			throw new RuntimeException(" Database connection error");
		}

		for (String reportName : queryList) {

			String sql = new QueryList().getSqlQuery(reportName.trim(),
					props.getProperty("inputPath"));
			if (!sql.isEmpty()) {
				sql = sql.trim();
			} else {
				throw new RuntimeException("The report " + reportName
						+ " is not available in the input path ");
			}

			if (sql.charAt(sql.length() - 1) == ';') {

				sql = sql.substring(0, sql.length() - 1);

			}

			try {

				Statement st = conn
						.createStatement(ResultSet.TYPE_FORWARD_ONLY,
								ResultSet.CONCUR_READ_ONLY);
				st.setFetchSize(10000);

				logger.info(reportNum + ". " + reportName
						+ " started executing.");
				// logger.info(sql);

				rs = st.executeQuery(sql);
				SXSSFWorkbook workbook = new SXSSFWorkbook(1000);

				CellStyle dateCellStyle = workbook.createCellStyle();
				CreationHelper dateCreationHelper = workbook
						.getCreationHelper();

				Cell cell;

				dateCellStyle.setDataFormat(dateCreationHelper
						.createDataFormat().getFormat("dd-MMM-yy hh:MM:ss"));

				Sheet sheet = workbook.createSheet(reportName);
				ResultSetMetaData resultSetMetaData = rs.getMetaData();
				int columnNumbers = resultSetMetaData.getColumnCount();

				XSSFCellStyle style = (XSSFCellStyle) workbook
						.createCellStyle();
				style.setBorderTop(BorderStyle.THICK); // double lines border
				style.setBorderBottom(BorderStyle.THICK); // single line border
				XSSFFont font = (XSSFFont) workbook.createFont();
				font.setFontHeightInPoints((short) 9);
				//font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
				font.setBold(true);
				style.setFont(font);
				style.setAlignment(HorizontalAlignment.CENTER);

				Row header = sheet.createRow(0);
				sheet.createFreezePane(0, 1);

				for (int i = 1; i <= columnNumbers; i++) {
					String name = resultSetMetaData.getColumnName(i);
					header.createCell(i - 1).setCellValue(name);
					header.getCell(i - 1).setCellStyle(style);
				}

				int rowNum = 1;
				while (rs.next()) {
					Row row = sheet.createRow(rowNum);

					for (int i = 1; i <= columnNumbers; i++) {
						String type = resultSetMetaData.getColumnTypeName(i);

						if (type.equalsIgnoreCase("VARCHAR2")) {
							cell = row.createCell(i - 1);

							if (null != rs.getString(i)) {
								cell.setCellType(CellType.STRING);
								cell.setCellValue(rs.getString(i));
							}

						} else if (type.equalsIgnoreCase("NUMBER")) {

							cell = row.createCell(i - 1);
							cell.setCellType(CellType.NUMERIC);

							cell.setCellValue(rs.getDouble(i));

						} else if (type.equalsIgnoreCase("DATE")) {

							cell = row.createCell(i - 1);

							// getting null pointer exception when date is null
							if (null != rs.getDate(i)) {
								cell.setCellStyle(dateCellStyle);
								cell.setCellValue(rs.getDate(i));
							}

						} else {
							row.createCell(i - 1).setCellValue(rs.getString(i));

						}
					}

					rowNum++;

				}

				if (rowNum == 1) {
					sheet.createRow(rowNum)
							.createCell(0)
							.setCellValue(
									"No Results Found.............................");
					logger.info(reportNum + ". " + "No results found for "
							+ reportName + " .............................");
				}

				/*for (int i = 0; i <= columnNumbers; i++) {

					sheet.autoSizeColumn(i);
				}*/
				
				workbook.createSheet("Query").createRow(0).createCell(0)
						.setCellValue(sql);
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				String formattedDate = sdf.format(date);

				String outputPath = props.getProperty("outputPath")
						+ reportName + "_" + formattedDate + ".xlsx";
				
				rs.close();
				
				String outPath= props.getProperty("outputPath");
				Path path = Paths.get(outPath);
				if (Files.exists(path,  LinkOption.NOFOLLOW_LINKS)) {
					fileOut = new FileOutputStream(outputPath);
				}else {
					Files.createDirectories(path);
					fileOut = new FileOutputStream(outputPath);					
				}

				workbook.write(fileOut);
				fileOut.close();
				// logger.info("Successfully downloaded the report " +
				// reportName);
				logger.info(reportNum + ". " + reportName
						+ " successfully downloaded.");
				// logger.info("\n");
				reportNum++;
			} catch (FileNotFoundException e) {
				logger.error("An exception occured while runinng the report "
						+ reportName + e.getMessage());

			} catch (SQLException e) {
				logger.error("An exception occured while runinng the report "
						+ reportName + e.getMessage());
			}

			catch (Exception e) {
				logger.error("An exception occured while runinng the report "
						+ reportName + "  " + e.getMessage());
			}

		}

		logger.info("Ended Report batch process and reports are downloaded to "+props.getProperty("outputPath"));
	}
}