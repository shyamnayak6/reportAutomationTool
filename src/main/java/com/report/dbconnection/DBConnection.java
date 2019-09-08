package com.report.dbconnection;



import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.report.export.RunReport;
 
public class DBConnection {
 
	final static Logger logger = Logger.getLogger(DBConnection.class);
    public static Connection getConnection() {
        Properties props = new Properties();
        FileInputStream fis = null;
        Connection con = null;
        try {
        	//System.out.println(new java.io.File("").getAbsolutePath());
        	String DB_PROP = System.getProperty("user.dir");
            fis = new FileInputStream(DB_PROP+File.separator+"/connection.properties");
            props.load(fis);
            logger.info("DB Details : "+props.getProperty("DB_URL"));
            // load the Driver Class
            Class.forName(props.getProperty("DB_DRIVER_CLASS".trim()));
 
            // create the connection now
            con = DriverManager.getConnection(props.getProperty("DB_URL").trim(),
                    props.getProperty("DB_USERNAME").trim(),
                    props.getProperty("DB_PASSWORD").trim());
        } catch (Exception e) {
            // TODO Auto-generated catch block
        	logger.error(e.getMessage());
        }
        return con;
    }
}

