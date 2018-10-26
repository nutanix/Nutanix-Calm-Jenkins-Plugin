package com.calm.Interface;
import org.json.JSONObject;

import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class Rest {
    private String BASE_URL = "https://<pcip>:9440/api/nutanix/v3/";
    private final static String DEFAULT_POST_PARAMS = "{\"length\":250}";
    private final static String GET = "GET";
    private final static String POST = "POST";
    private final static String DELETE = "DELETE";
    private final String PRISMCENTRALIP;
    private final String USERNAME;
    private final String PASSWORD;
    private final boolean VERIFY_CERTIFICATES;
    public Rest(String prismCentralIp, String userName, String password, boolean verify) {
        PRISMCENTRALIP = prismCentralIp;
        USERNAME = userName;
        PASSWORD = password;
        BASE_URL = BASE_URL.replace("<pcip>", PRISMCENTRALIP);
        VERIFY_CERTIFICATES = verify;
    }

    public JSONObject get(String relativeURL)throws Exception{
        return send_request(relativeURL, GET);
    }

    public JSONObject post(String relativeURL, String... payload)throws Exception{

        if(payload.length > 0)
            return send_request(relativeURL, POST, payload[0]);
        return send_request(relativeURL, POST);
    }

    public JSONObject delete(String relativeURL) throws Exception{
        return send_request(relativeURL, DELETE);
    }

    private JSONObject send_request(String relativeURL, String requestType, String... payload) throws Exception{
        if (!VERIFY_CERTIFICATES) {
            trustCertificates();//skip certificate validation
        }

        //Connect to url
        URL url = new URL(BASE_URL + relativeURL);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        try {

            String encoding = DatatypeConverter.printBase64Binary((USERNAME + ":" + PASSWORD).getBytes());
            httpURLConnection.setRequestMethod(requestType);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Authorization", "Basic " + encoding);
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");

            //send payload for post request
            if(requestType.equals(POST)){
                httpURLConnection.setDoOutput(true);
                OutputStream os = httpURLConnection.getOutputStream();
                if(payload.length > 0)
                    os.write(payload[0].getBytes());
                else
                    os.write(DEFAULT_POST_PARAMS.getBytes());
                os.flush();
                os.close();
            }

            //return the response on success
            int responseCode = httpURLConnection.getResponseCode();
            switch (responseCode){
                case 200 :  BufferedReader bufferedReader =
                        new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = bufferedReader.readLine()) != null)
                        response.append(inputLine);
                    return new JSONObject(response.toString());
                default :   //TODO: Throw user defined exception
                    throw new Exception("URL: " + url.toString() + "\n" +
                            "Payload: " + (payload.length > 0?payload[0]:null) + "\n" +
                            "Response code: "+ responseCode + "\n" +
                            "Response: " + httpURLConnection.getResponseMessage());

            }
        }
        catch (Exception e){
            System.out.println("Error occurred while sending request");
            throw e;
        }
        finally {
            //close connection
            httpURLConnection.disconnect();
        }
    }

    private void trustCertificates() throws Exception{
        //Security.addProvider(new Provider());
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                if (!urlHostName.equals(session.getPeerHost())) {
                    System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
                }
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }
}
