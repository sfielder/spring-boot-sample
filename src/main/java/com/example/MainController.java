package com.example;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.*;
import java.sql.*;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@Controller
@EnableAutoConfiguration
public class MainController {

  @RequestMapping("/home")
  String home() {
      return "home";
  }

  @RequestMapping(value="/submit/", method = RequestMethod.POST)
  @ResponseBody
  String submit(@RequestBody String body) {
    System.out.println("body => " + body);
    Map<String,String> records = parseParams(body);

    String inviteeName = records.get("name");
    //split name into first and last for salesforce API
    String[] tmpStrList = inviteeName.split(" ");
    String inviteeFirstName = tmpStrList[0];
    String inviteeLastName = tmpStrList[1];
    String inviteeEmail = records.get("email");
    String inviteePhone = records.get("phone");
    String apptDate = records.get("date");
    Boolean sendSms = records.get("send_sms").equals("true");

    try {
      Connection connection = getConnection();

      Statement stmt = connection.createStatement();
      createTables(connection);

      createInvitee(connection, inviteeName, inviteeEmail, inviteePhone);
      createAppointment(connection, inviteeEmail, apptDate, sendSms);

      //Adding an extra method for Heroku Connect 'salesforce' schema
      createHerokuConnectAppointment(connection, inviteeFirstName, inviteeLastName, inviteeEmail, inviteePhone, apptDate);

      if (sendSms) {
        sendSms(inviteePhone, apptDate);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "There was an error: " + e.getMessage();
    }

    return "ok";
  }

  private void sendSms(String phoneNumber, String date) {
    String blowerIoUrlStr = System.getenv("BLOWERIO_URL");

    if (null != blowerIoUrlStr) {
      try {
        String data = "to=" + URLEncoder.encode(phoneNumber, "UTF-8") +
          "&message=" + URLEncoder.encode("You have a new appt on " + date, "UTF-8");

        URL blowerIoUrl = new URL(blowerIoUrlStr + "messages");
        final String username = blowerIoUrl.getUserInfo().split(":")[0];
        final String password = blowerIoUrl.getUserInfo().split(":")[1];

        disableCertificateValidation();
        Authenticator.setDefault (new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication (username, password.toCharArray());
            }
        });

        HttpsURLConnection con = (HttpsURLConnection)blowerIoUrl.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.getOutputStream().write(data.getBytes("UTF-8"));

        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String tmp = "BLOWERIO Response:\n";
        while((tmp = reader.readLine()) != null) {
            System.out.println(tmp);
        }
      } catch (Exception e) {
        String errMsg = "There was an SMS error: " + e.getMessage();
        e.printStackTrace();
      }
    } else {
      System.out.println("No BlowerIO URL set");
    }
  }

  private Map<String,String> parseParams(String body) {
    String[] args = body.split("&");

    Map<String,String> records = new HashMap<String,String>();

    for (String arg : args) {
      String[] parts = arg.split("=");
      String key = parts[0];
      String val = parts.length > 1 ? URLDecoder.decode(parts[1]) : null;
      records.put(key, val);
    }

    return records;
  }

  //Added for Heroku Connect
  private void createHerokuConnectAppointment(Connection connection, String firstName, String lastName, String email, String phone, String date) throws SQLException {
      PreparedStatement pstmt = connection.prepareStatement(
          "INSERT INTO salesforce.contact (appointment__c, email, phone, firstname, lastname) VALUES (?,?,?,?,?)");
      pstmt.setString(1, date);
      pstmt.setString(2, email);
      pstmt.setString(3, phone);
      pstmt.setString(4, firstName);
      pstmt.setString(5, lastName);
      // pstmt.executeUpdate();
  }


  private void createAppointment(Connection connection, String inviteeId, String date, Boolean sendSms) throws SQLException {
    PreparedStatement pstmt = connection.prepareStatement(
        "INSERT INTO appointments (invitee_id, date) VALUES (?,?)");
    pstmt.setString(1, inviteeId);
    pstmt.setString(2, date);
    pstmt.executeUpdate();
  }

  private void createInvitee(Connection connection, String name, String email, String phone) throws SQLException {
    PreparedStatement pstmt = connection.prepareStatement(
        "SELECT name FROM invitees WHERE email=?");
    pstmt.setString(1, email);
    ResultSet rs = pstmt.executeQuery();

    if (!rs.next()) {
      pstmt = connection.prepareStatement(
          "INSERT INTO invitees (name, email, phone) VALUES (?,?,?)");
      pstmt.setString(1, name);
      pstmt.setString(2, email);
      pstmt.setString(3, phone);
      pstmt.executeUpdate();
    }
  }

  private void createTables(Connection connection) throws SQLException {
    Statement stmt = connection.createStatement();
    stmt.executeUpdate("CREATE TABLE IF NOT EXISTS appointments (" +
      "invitee_id  varchar(250)," +
      "date        varchar(250),"+
      "send_sms    boolean" +
    ")");
    stmt.executeUpdate("CREATE TABLE IF NOT EXISTS invitees (" +
      "name        varchar(250)," +
      "email       varchar(250)," +
      "phone       varchar(250)," +
      "CONSTRAINT  unique_email UNIQUE(email)" +
    ")");
  }

  private Connection getConnection() throws URISyntaxException, SQLException {
    URI dbUri = new URI(System.getenv("DATABASE_URL"));

    String username = dbUri.getUserInfo().split(":")[0];
    String password = dbUri.getUserInfo().split(":")[1];
    String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + dbUri.getPath();

    return DriverManager.getConnection(dbUrl, username, password);
  }

  public void disableCertificateValidation() {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
          }

          @Override
          public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

          }

          @Override
          public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

          }
      }};

      // Ignore differences between given hostname and certificate hostname
      HostnameVerifier hv = new HostnameVerifier() {
          @Override
          public boolean verify(String hostname, SSLSession session) {
              return true;
          }
      };

      // Install the all-trusting trust manager
      try {
          SSLContext sc = SSLContext.getInstance("SSL");
          sc.init(null, trustAllCerts, new SecureRandom());
          HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
          HttpsURLConnection.setDefaultHostnameVerifier(hv);
      } catch (Exception e) {
          // Do nothing
      }
  }
}
