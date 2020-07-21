package com.tms.server.loadmaster.dsp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.net.ssl.HttpsURLConnection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tms.common.lib.StringList;
import com.tms.common.lib.Application;
import com.tms.common.lib.Logger;

public class ExportChepAPIData {

	private static Logger exportLogger = null;
	private static int logLevel = 1;
	private static String logDir = Application.getBaseDir() + File.separatorChar + "logs" + File.separatorChar + "chep";
	protected StringList exceptionMessages = new StringList();


	public static void main(String[] args) 
	{
		exportLogger = new Logger(logLevel, logDir, "ChepAPI");

		ExportChepAPIData start = new ExportChepAPIData();

		try 
		{
			exportLogger.log(1, "******* STARTING ChepAPI EXPORT PROCESS *********" + "\n");
			start.run();

		} catch (Exception ex) {

			exportLogger.log(1, "Problem in start()");
			ex.printStackTrace();
			System.exit(-1);
		}
		exportLogger.log(1, "\n" + "******* COMPLETED ChepAPI EXPORT PROCESS ********");
		System.exit(0);
	}


	public void run() throws IOException, ParseException
	{
		chepFunction();
	}


	public static String toPrettyFormat(String jsonString) 
	{
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(jsonString).getAsJsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(json);
		return prettyJson;
	}

	public static JsonObject toJSON(String jsonString) 
	{
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(jsonString).getAsJsonObject();
		return json;
	}

	///////////////////////////////////////////////////////////////////

	//	AUTHENTICATION
	protected static String getChepToken() throws IOException, ParseException
	{
		int expires_in = 0;
		String token = "";
		BufferedReader in = null;
		exportLogger.log(1,"AUTHENTICATION START*************");

		String last=null, line;
		BufferedReader csvReader = new BufferedReader(new FileReader("\\" + File.separatorChar + "frciprolme" + File.separatorChar + "d$" + File.separatorChar + "McLeod_1820" + File.separatorChar + "lme" + File.separatorChar + "edi" + File.separatorChar + "fcc" + File.separatorChar + "out" + File.separatorChar + "chep" + File.separatorChar + "inprocess" + File.separatorChar + "authentication.txt"));
		while ((line = csvReader.readLine()) != null) 
		{
			if(line!=null)
			{
				last = line;
			}	
		}
		String[] token_data = last.split(",");
		String expiration_date = (token_data[2]);

		Date expiration_date2 = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy").parse(expiration_date);
		Date today = new Date();
		csvReader.close();

		if(expiration_date2.after(today)) {
			token = (token_data[0]);
			exportLogger.log(8,"Token From File: " + token);
		}

		else {

			String urlString = "****************************";
			StringBuilder data = new StringBuilder();

			data.append("{\"password\": \"*********\",");
			data.append("\"username\": \"**********\"}");

			exportLogger.log(8,"Credentials: " + data.toString());

			URL url = new URL(urlString);
			HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", "bearer " + data.toString());
			conn.setDoOutput(true);

			OutputStream postStream = conn.getOutputStream();
			postStream.write(data.toString().getBytes());

			int status = conn.getResponseCode();
			exportLogger.log(1,"HTTP Status code for Authentication = " + status);
			postStream.close();

			InputStreamReader reader = new InputStreamReader(conn.getInputStream());
			in = new BufferedReader(reader);

			String json = in.readLine();			
			String prettyjson = toPrettyFormat(json);
			exportLogger.log(4,"Response body = " + prettyjson);

			JsonObject jsonToken = toJSON(prettyjson);

			token = jsonToken.get("access_token").toString();
			expires_in = jsonToken.get("expires_in").getAsInt();

			token = token.replaceAll("^\"|\"$", "");

			Calendar cal = Calendar.getInstance();
			String now = cal.getTime().toString();

			cal.add(Calendar.SECOND, expires_in);
			String expiration_date3 = cal.getTime().toString();
			exportLogger.log(8,"Expiration Date of Token = " + expiration_date3);

			FileWriter csvWriter = new FileWriter(****************, true);
			csvWriter.append(token);
			csvWriter.append(",");
			csvWriter.append(now);
			csvWriter.append(",");
			csvWriter.append(expiration_date3);
			csvWriter.append("\n");
			csvWriter.flush();
			csvWriter.close();
		}
		exportLogger.log(1,"AUTHENTICATION FINISH*************");
		return token;
	}

	///////////////////////////////////////////////////////////////////

	//	chepAPI
	protected static void chepFunction() throws IOException, ParseException
	{			

		String filename= ************;

		File dir = new File(filename);
		if(!dir.isDirectory()) throw new IllegalStateException("There is an error in the chepAPI function");

		for(File file : dir.listFiles()) 
		{

			if(file.getName().startsWith("edi"))
			{

				try
				{
					String token = ExportChepAPIData.getChepToken();

					FileInputStream fstream = new FileInputStream(file);
					DataInputStream in = new DataInputStream(fstream);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					String value = "";
					BufferedReader in2 = null;

					String loadid="";
					String delivery_numbers="";
					String sourceid="";
					String event_code="";
					String vehicle_id="";
//					String stop_num="";
//					String event_type="";

					while((value = br.readLine()) != null)
					{
						value = value.replaceAll("(?m)\\~$", "");

						if(value.startsWith("B10"))
						{
							String[] b10 = value.split(",");
							loadid = b10[1];
							delivery_numbers = b10[2];
							sourceid = b10[3];
							event_code = b10[4];
//							stop_num = b10[5];
//							event_type = b10[6];

						} else if (value.startsWith("MS1")) {
							String[] ms1 = value.split(",");
							vehicle_id = ms1[3];

						}
					}

					if(event_code.equals("AF"))
					{
						try
						{
							exportLogger.log(1,"CREATE_TRIP: START*************");
							String trip_id = "";
							String dest_address = "";
							String dest_city = "";
							String dest_postal_code = "";
							String dest_region_name = "";
							String check_out_date_time = "";
							String check_in_date_time = "";
							String origin_address = "";
							String origin_city = "";
							String origin_postal_code = "";
							String origin_region_name = "";
							String urlString = "************";

							StringBuilder tmpBuff = new StringBuilder();

							FileInputStream fstream2 = new FileInputStream(file);
							DataInputStream in3 = new DataInputStream(fstream2);
							BufferedReader br2 = new BufferedReader(new InputStreamReader(in3));
							String value2 = "";
							BufferedReader in44 = null;

							while((value2 = br2.readLine()) != null)
							{
								value2 = value2.replaceAll("(?m)\\~$", "");

								if (value2.startsWith("N3,SH")) {
									String[] n3sh = value2.split(",");
									origin_address = n3sh[1].substring(2);

								} else if (value2.startsWith("N4,SH")) {
									String[] n4sh = value2.split(",");
									origin_city = n4sh[1].substring(2);
									origin_region_name = n4sh[2];
									origin_postal_code = n4sh[3];

								} else if (value2.startsWith("N3,CN")) {
									String[] n3cn = value2.split(",");
									dest_address = n3cn[1].substring(2);

								} else if (value2.startsWith("N4,CN")) {
									String[] n4cn = value2.split(",");
									dest_city = n4cn[1].substring(2);
									dest_region_name = n4cn[2];
									dest_postal_code = n4cn[3];

								} else if (value2.startsWith("AT7,Ar,AF")) {
									String year = "";
									String month = "";
									String day = "";
									String hours = "";
									String minutes = "";
									String[] at7 = value2.split(",");
									year = at7[5].substring(0, 4);
									month = at7[5].substring(4,6);
									day = at7[5].substring(6,8);
									hours = at7[6].substring(0,2);
									minutes = at7[6].substring(2,4);
									check_in_date_time = year + "-" + month + "-" + day + "T" + hours + ":" + minutes + ":" + "00.001" + "-05:00";

								} else if (value2.startsWith("AT7,De,AF")) {
									String year = "";
									String month = "";
									String day = "";
									String hours = "";
									String minutes = "";
									String[] at7 = value2.split(",");
									year = at7[5].substring(0, 4);
									month = at7[5].substring(4,6);
									day = at7[5].substring(6,8);
									hours = at7[6].substring(0,2);
									minutes = at7[6].substring(2,4);
									check_out_date_time = year + "-" + month + "-" + day + "T" + hours + ":" + minutes + ":" + "00.001" + "-05:00";
								} 
							}
							br2.close();

							tmpBuff.append("{");
							tmpBuff.append("\"load_id\":" + "\"" + loadid + "\"" + ",");
							tmpBuff.append("\"sourceid\":" + "\"" + sourceid + "\"" + ",");
							tmpBuff.append("\"load_number\":" + "\"" + loadid + "\"" + ",");
							tmpBuff.append("\"delivery_numbers\": [");
							tmpBuff.append("\"" + delivery_numbers + "\"");
							tmpBuff.append("],");
							tmpBuff.append("\"dest_address1\":" + "\"" + dest_address + "\"" + ",");
							tmpBuff.append("\"dest_city\":" + "\"" + dest_city + "\"" + ",");
							tmpBuff.append("\"dest_postal_code\":" + "\"" + dest_postal_code + "\"" + ",");
							tmpBuff.append("\"dest_region_name\":" + "\"" + dest_region_name + "\"" + ",");
							tmpBuff.append("\"dest_country_code\": \"US\",");
							tmpBuff.append("\"origin_stop_id\": \"000000000\",");
							tmpBuff.append("\"dest_stop_id\": \"000000000\",");
							tmpBuff.append("\"vehicle_id\":" + "\"" + vehicle_id + "\"" + ",");			
							tmpBuff.append("\"check_out_date_time\":" + "\"" + check_out_date_time + "\"" + ",");
							tmpBuff.append("\"check_in_date_time\":" + "\"" + check_in_date_time + "\"" + ",");		
							tmpBuff.append("\"origin_address1\":" + "\"" + origin_address + "\"" + ",");
							tmpBuff.append("\"origin_city\":" + "\"" + origin_city + "\"" + ",");
							tmpBuff.append("\"origin_postal_code\":" + "\"" + origin_postal_code + "\"" + ",");
							tmpBuff.append("\"origin_region_name\":" + "\"" + origin_region_name + "\"" + ",");
							tmpBuff.append("\"origin_country_code\": \"US\"");
							tmpBuff.append("}");

							exportLogger.log(4,"POST body =\n" + tmpBuff);

							URL url = new URL(urlString);
							HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
							conn.setDoOutput(true);
							conn.setUseCaches(false);
							conn.setRequestMethod("POST");
							conn.setRequestProperty("Content-Type", "application/json");
							conn.setRequestProperty("Authorization", "bearer " + token);
							conn.setDoOutput(true);

							OutputStream postStream = conn.getOutputStream();
							postStream.write(tmpBuff.toString().getBytes());

							int status = conn.getResponseCode();
							exportLogger.log(1,"HTTP Status code for create_Trip = " + status);
							postStream.close();

							InputStreamReader reader = new InputStreamReader(conn.getInputStream());
							in44 = new BufferedReader(reader);

							String json = in44.readLine();			
							String prettyjson = toPrettyFormat(json);
							exportLogger.log(4,"Response body = " + prettyjson);

							JsonObject jsonToken = toJSON(prettyjson);

							trip_id = jsonToken.get("trip_id").toString();
							trip_id = trip_id.replaceAll("^\"|\"$", "");

							Calendar cal = Calendar.getInstance();
							String now = cal.getTime().toString();

							FileWriter csvWriter = new FileWriter(************, true);

							csvWriter.append(loadid);
							csvWriter.append(",");
							csvWriter.append(trip_id);
							csvWriter.append(",");
							csvWriter.append(now);		
							csvWriter.append("\n");

							csvWriter.flush();
							csvWriter.close();

							exportLogger.log(1,"CREATE_TRIP: FINISH*************");

						}

						catch(Throwable ex) {
							ex.printStackTrace();
						}

					} else if(event_code.equals("X6") || event_code.equals("X3") || event_code.equals("L1") || event_code.equals("X1") || event_code.equals("X5"))
					{
						try
						{
							exportLogger.log(1,"CREATE_PING: START*************");
							exportLogger.log(8,"Event_code: " + event_code);
							String timestamp = "";
							String statuses="";
							String longitude = "";
							String latitude = "";
							String urlString = "************";

							StringBuilder tmpBuff = new StringBuilder();

							FileInputStream fstream2 = new FileInputStream(file);
							DataInputStream in4 = new DataInputStream(fstream2);
							BufferedReader br2 = new BufferedReader(new InputStreamReader(in4));
							String value3 = "";

							while((value3 = br2.readLine()) != null)
							{
								value3 = value3.replaceAll("(?m)\\~$", "");

								if (value3.startsWith("AT7,CD")) {
									String year = "";
									String month = "";
									String day = "";
									String hours = "";
									String minutes = "";
									String[] at7 = value3.split(",");
									year = at7[5].substring(0, 4);
									month = at7[5].substring(4,6);
									day = at7[5].substring(6,8);
									hours = at7[6].substring(0,2);
									minutes = at7[6].substring(2,4);
									timestamp = year + "-" + month + "-" + day + "T" + hours + ":" + minutes + ":" + "00.001" + "-05:00";

								} else if (value3.startsWith("MS1")) {
									String[] ms1 = value3.split(",");
									longitude = ms1[4];
									latitude = ms1[5];

								} else if (value3.startsWith("AT7,ET,X1")) {
									statuses = "STOPPED\", \"AT_CUSTOMER";			

								} else if (value3.startsWith("AT7,ET,X3")) {
									statuses = "STOPPED\", \"AT_SC";	

								} else if (value3.startsWith("AT7,ET,L1")) {
									statuses = "STOPPED\", \"LOADING";	

								} else if (value3.startsWith("AT7,ET,X5")) {
									statuses = "STOPPED\", \"UNLOADING";	

								} else if (value3.startsWith("AT7,ET,X6")) {
									statuses = "EN_ROUTE";	
								} 
							}
							br2.close();

							tmpBuff.append("{");
							tmpBuff.append("\"location\": [");
							tmpBuff.append("{");
							tmpBuff.append("\"uuid\": \"0000\",");
							tmpBuff.append("\"timestamp\":" + "\"" + timestamp + "\"" + ",");
							tmpBuff.append("\"coords\": {");
							tmpBuff.append("\"latitude\":" + latitude + ",");
							tmpBuff.append("\"longitude\":" + longitude + ",");
							tmpBuff.append("\"accuracy\": 0.0");
							tmpBuff.append("},");
							tmpBuff.append("\"extras\": {");
							tmpBuff.append("\"registeredRoutes\": [");
							tmpBuff.append("{");
							tmpBuff.append("\"loadId\":" + "\"" + loadid + "\"" + ",");
							tmpBuff.append("\"sourceId\":" + "\"" + sourceid + "\"" + ",");
							tmpBuff.append("\"statuses\":[" + "\"" + statuses + "\"]");
							tmpBuff.append("}");
							tmpBuff.append("],");
							tmpBuff.append("\"vehicle\":" + "\"" + vehicle_id + "\"");
							tmpBuff.append("}");
							tmpBuff.append("}");
							tmpBuff.append("],");
							tmpBuff.append("\"device\": {");
							tmpBuff.append("\"uuid\": \"0000\",");
							tmpBuff.append("\"model\": \"OMNITRACS\"");
							tmpBuff.append("}");
							tmpBuff.append("}");

							exportLogger.log(4,"POST body =\n" + tmpBuff);

							URL url = new URL(urlString);
							HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
							conn.setDoOutput(true);
							conn.setUseCaches(false);
							conn.setRequestMethod("POST");
							conn.setRequestProperty("Content-Type", "application/json");
							conn.setRequestProperty("Authorization", "bearer " + token);
							conn.setDoOutput(true);

							OutputStream postStream = conn.getOutputStream();
							postStream.write(tmpBuff.toString().getBytes());

							int status = conn.getResponseCode();
							exportLogger.log(1,"HTTP Status code for create_Ping = " + status);
							postStream.close();

							InputStreamReader reader = new InputStreamReader(conn.getInputStream());
							in2 = new BufferedReader(reader);

							String json = in2.readLine();			
							String prettyjson = toPrettyFormat(json);
							exportLogger.log(4,"Response body = " + prettyjson);

							exportLogger.log(1,"CREATE_PING: FINISH*************");
						}	

						catch (Throwable ex) {
							ex.printStackTrace();
						}


					} else if(event_code.equals("AG"))
					{
						try
						{
							String trip_id = "";
							String eta = "";
							StringBuilder tmpBuff2 = new StringBuilder();
							BufferedReader in3 = null;
							BufferedReader csvReader = new BufferedReader(new FileReader(************);
							String value2;

							while((value2 = csvReader.readLine()) != null)
							{
								if(value2.startsWith(loadid.toString()))
								{
									String[] b11 = value2.split(",");
									trip_id = b11[1];
								}
							}
							csvReader.close();

							FileInputStream fstream2 = new FileInputStream(file);
							DataInputStream in4 = new DataInputStream(fstream2);
							BufferedReader br2 = new BufferedReader(new InputStreamReader(in4));
							String value3 = "";

							while((value3 = br2.readLine()) != null)
							{
								value3 = value3.replaceAll("(?m)\\~$", "");

								if (value3.startsWith("AT7,ET,AG")) {
									String year = "";
									String month = "";
									String day = "";
									String hours = "";
									String minutes = "";
									String[] at7 = value3.split(",");
									year = at7[5].substring(0, 4);
									month = at7[5].substring(4,6);
									day = at7[5].substring(6,8);
									hours = at7[6].substring(0,2);
									minutes = at7[6].substring(2,4);
									eta = year + "-" + month + "-" + day + "T" + hours + ":" + minutes + ":" + "00.000" + "-05:00";

								}
							}
							br2.close();

							exportLogger.log(1,"UPDATE_TRIP: START*************");
							exportLogger.log(4,"Trip_id from file: " + trip_id);
							String urlString2 = "************ + trip_id + "/eta";
							exportLogger.log(1,urlString2);

							tmpBuff2.append("{");
							tmpBuff2.append("\"eta\":" + "\"" + eta + "\"");
							tmpBuff2.append("}");

							exportLogger.log(4,"POST body =\n" + tmpBuff2);

							URL url2 = new URL(urlString2);
							HttpsURLConnection conn2 = (HttpsURLConnection)url2.openConnection();
							conn2.setDoOutput(true);
							conn2.setUseCaches(false);
							conn2.setRequestMethod("POST");
							conn2.setRequestProperty("Content-Type", "application/json");
							conn2.setRequestProperty("Authorization", "bearer " + token);
							conn2.setDoOutput(true);

							OutputStream postStream2 = conn2.getOutputStream();
							postStream2.write(tmpBuff2.toString().getBytes());

							int status2 = conn2.getResponseCode();
							exportLogger.log(1,"HTTP Status code for update_Trip = " + status2);
							postStream2.close();

							InputStreamReader reader2 = new InputStreamReader(conn2.getInputStream());
							in3 = new BufferedReader(reader2);

							String json2 = in3.readLine();			
							String prettyjson2 = toPrettyFormat(json2);
							exportLogger.log(4,"Response body = " + prettyjson2);
							exportLogger.log(1,"UPDATE_TRIP: FINISH*************");
						}

						catch(Throwable ex) {
							ex.printStackTrace();
						}

					}

					else if(event_code.equals("D1"))
					{

						try
						{
							BufferedReader in3 = null;
							String trip_id = "";
							exportLogger.log(1,"CLOSE_TRIP: START*************");

							BufferedReader csvReader = new BufferedReader(new FileReader(************);
							String value2;

							while((value2 = csvReader.readLine()) != null)
							{
								if(value2.startsWith(loadid.toString()))
								{
									String[] b11 = value2.split(",");
									trip_id = b11[1];
								}
							}
							csvReader.close();
							exportLogger.log(4,"Trip_id from file: " + trip_id);

							String urlString = ************" + trip_id + "/close";

							URL url = new URL(urlString);
							HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
							conn.setDoOutput(true);
							conn.setUseCaches(false);
							conn.setRequestMethod("POST");
							conn.setRequestProperty("Content-Type", "application/json");
							conn.setRequestProperty("Authorization", "bearer " + token);
							conn.setDoOutput(true);

							OutputStream postStream = conn.getOutputStream();

							int status = conn.getResponseCode();
							exportLogger.log(1,"HTTP Status code for close_Trip = " + status);
							postStream.close();

							InputStreamReader reader = new InputStreamReader(conn.getInputStream());
							in3 = new BufferedReader(reader);

							String json = in3.readLine();			
							String prettyjson = toPrettyFormat(json);
							exportLogger.log(4,"Response body = " + prettyjson);

							exportLogger.log(1,"CLOSE_TRIP: FINISH*************");
						}

						catch(Throwable ex) {
							ex.printStackTrace();
						}
					}		

					br.close();
					file.delete();
				}

				catch(Throwable ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}
