package com.db.MT.Framework.AAISUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import oracle.jdbc.*;

import com.db.MT.Framework.AAISServiceCo.ConnectionPool;
import com.db.MT.Framework.AAISServiceCo.ReadProp;


//main branch change

public class GenGateKeeperFile {

          private static String  userName = null;
          private static String  passWord = null;
          private static String  dbName = null;
          private static ConnectionPool pool = null;

          // Read login name and password from aais.properties file and get the connection
          static{

               ReadProp rP = new ReadProp();
               dbName   = rP.getAppnameInfo("AAIS", "DB_NAME");
               userName = rP.getAppnameInfo("AAIS", "DB_USERNAME");
               passWord   = rP.getAppnameInfo("AAIS", "DB_PASSWD");

               try
               {

                pool = new ConnectionPool(dbName,userName,passWord,4,2);

               }
               catch(Exception e){e.printStackTrace();}
          }

          public void GenGatekeeperFile() {}

          public static Date getDateInGMT (Date inputDate) {

                Date outputdate = null;
                SimpleDateFormat inputSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat outputSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                inputSimpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                try {
                        String outputStr = inputSimpleDateFormat.format(inputDate);                                           
                        outputdate = outputSimpleDateFormat.parse(outputStr);

                } catch (ParseException e) {
                        return null;
                }
                return outputdate;
                }

          public static String getInputDateGMTString(String inpDate) {
                  String strGMT="";
                  try {

                          SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                          Date inputDate = dateFormat.parse(inpDate);
                          Date inputDate_GMT=getDateInGMT(inputDate);
                          SimpleDateFormat formatter_GMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'");
                          strGMT = formatter_GMT.format(inputDate_GMT).toString();

                  }
                  catch (Exception e) {
                          e.printStackTrace();
                  }
                  return strGMT;
          }


          public void GenGateKeeper (String narid,String loc){
                        SimpleDateFormat formatter;
                        String result;

                    formatter = new SimpleDateFormat("yyyyMMdd");
                    result=formatter.format(new Date());

                    OracleCallableStatement cstmt = null;
                Connection conn = null;
                OracleResultSet rs1 = null;
                OracleResultSet rs2 = null;
                ResultSetMetaData rsData1 = null;
                ResultSetMetaData rsData2 = null;
                int colCount1, colCount2;
                StringBuffer strBuffHeader = new StringBuffer();
                StringBuffer strBuffRecord = new StringBuffer();


                try {
                        loc = loc.equals("") ? "" : (loc.endsWith("/") ? loc : loc+"/");
                        File wfile = new File(loc+narid+"_"+result+".csv");

                        FileWriter fstream = new FileWriter(wfile);
                        BufferedWriter out = new BufferedWriter(fstream);


                  conn = pool.getConnection();
                  cstmt = (OracleCallableStatement) conn.prepareCall("{call PROC_GATEKEEPER(?,?, ?, ?, ?, ?) }");
                  
				cstmt.setString(1,narid);
                cstmt.registerOutParameter(2,OracleTypes.CURSOR);
                cstmt.registerOutParameter(3,OracleTypes.CURSOR);
                 cstmt.registerOutParameter(4,OracleTypes.INTEGER);
                cstmt.registerOutParameter(5,OracleTypes.INTEGER);
                cstmt.registerOutParameter(6,OracleTypes.VARCHAR);


                  cstmt.execute();

                  rs1 = (OracleResultSet) cstmt.getCursor(2);
                  rs2 = (OracleResultSet) cstmt.getCursor(3);

                  rsData1 = rs1.getMetaData();
                  colCount1 = rsData1.getColumnCount();

                  rsData2 = rs2.getMetaData();
                  colCount2 = rsData2.getColumnCount();

                  while (rs1.next()){
                          for(int i=1;i<colCount1;i++) {
				
                                  String rsColString = rs1.getString(i)==null ? "" : rs1.getString(i);
                                  strBuffHeader.append(rsColString).append(",");
                          }
                          String rsColString = rs1.getString(colCount1)==null ? "" : rs1.getString(colCount1);
                          strBuffHeader.append(rsColString).append("\n");
                          out.write(strBuffHeader.toString());
                  }

                  while (rs2.next()){
                          for(int i=1;i<colCount2;i++) {
                        	  String fieldName=rsData2.getColumnLabel(i);

                                  if(fieldName.equals("Last-Login")){
                                          String  lastLogin = rs2.getString(i);
                                          if (lastLogin != null && !lastLogin.trim().equals("")) {
                                                strBuffRecord.append(getInputDateGMTString(lastLogin)).append(",");
                                          } else {
                                                strBuffRecord.append(",");
                                          }
                                  } else if(fieldName.equals("Start Date") || fieldName.equals("Expiry Date")) {
                                          String dateString =  rs2.getString(i);
                                          if (dateString != null && !dateString.trim().equals("")) {
                                                strBuffRecord.append(getInputDateGMTString(dateString)).append(",");
                                          } else {
                                                strBuffRecord.append(",");
                                          }
                                  }else {
                                          String rsColString2 = rs2.getString(i)==null ? "" : rs2.getString(i);
                                          strBuffRecord.append(rsColString2).append(",");
                                  }
                          }
                          String rsColString2 = rs2.getString(colCount2)==null ? "" : rs2.getString(colCount2);
                          strBuffRecord.append(rsColString2).append("\n");
                  }

				 String resultString = strBuffRecord.toString().replaceAll(String.valueOf((char) 228),"ae").replaceAll(String.valueOf((char) 246),"oe").replaceAll(String.valueOf((char) 252),"ue").replaceAll(String.valueOf((char) 223),"ss").replaceAll(String.valueOf((char) 160),"");
                  out.write(resultString);
                  out.close();
                }

                catch(Exception e){

                  e.printStackTrace();

                }


                finally{
                          try{
                          cstmt.close();
                  if ( conn != null) pool.returnConnection(conn);
                          }
                          catch(Exception e){}
                }



              }

          public static void main(String args[]) {
                String narid =null;
                String loc = null;
                narid=args[0];
                if(args[1]==null)
                {
                  loc="";
                }else
                  {
                                        loc = args[1];
                  }
//              System.out.println("narid"+narid+"location"+loc);

                GenGateKeeperFile gk = new GenGateKeeperFile();
                gk.GenGateKeeper(narid,loc);
          }


}

