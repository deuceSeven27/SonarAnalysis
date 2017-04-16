package com.company;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by rajeshdurai on 4/04/17.
 */

/*logs into the adelaide uni authentication, then you can request any page hidden behind the login*/
public class WebAuthAndRequest {

    private List<String> cookies;
    private HttpsURLConnection uc;

    private final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.98 Safari/537.36";

    /*gets the login page and acts like a browser*/
    public String getPage(String page) throws Exception{
        URL url = new URL(page);
        uc = (HttpsURLConnection) url.openConnection();
        uc.setRequestMethod("GET");

        uc.setUseCaches(false);

        // act like a browser
        uc.setRequestProperty("User-Agent", USER_AGENT);
        uc.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        if (cookies != null) {
            for (String cookie : this.cookies) {
                uc.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
            }
        }
        int responseCode = uc.getResponseCode();
        System.out.println("Sending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode + "\n");
        BufferedReader in =
                new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Get the response cookies
        setCookies(uc.getHeaderFields().get("Set-Cookie"));

        return response.toString();
    }

    /*gets the login elements from the page with the login form*/
    public String getLoginElements(String loginPage, String username, String password) throws Exception{

        System.out.println("Extracting form's data...");

        //run jSoup to get the form elements to fill up
        Document d = Jsoup.parse(loginPage);
        Element form = d.getElementById("fm1");
        Elements inputElements = form.getElementsByTag("input");
        List<String> paramList = new ArrayList<String>();

        for (Element inputElement : inputElements) {
            String key = inputElement.attr("name");
            String value = inputElement.attr("value");

            if (key.equals("username"))
                value = username;
            else if (key.equals("password"))
                value = password;
            paramList.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
        }

        //return the required fields
        // build parameters list
        StringBuilder result = new StringBuilder();
        for (String param : paramList) {
            if (result.length() == 0) {
                result.append(param);
            } else {
                result.append("&" + param);
            }
        }
        return result.toString();
    }

    public List<String> getCookies() {
        return cookies;
    }

    public void sendPost(String url, String postParams) throws Exception{
        URL obj = new URL(url);
        uc = (HttpsURLConnection) obj.openConnection();

        // Acts like a browser
        uc.setUseCaches(false);
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Host", "login.adelaide.edu.au");
        uc.setRequestProperty("User-Agent", USER_AGENT);
        uc.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        for (String cookie : this.cookies) {
            uc.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
        }
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "https://login.adelaide.edu.au/cas/login?service=https%3A%2F%2Fmyuni.adelaide.edu.au%2Flogin%2Fcas");
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        uc.setRequestProperty("Content-Length", Integer.toString(postParams.length()));

        uc.setDoOutput(true);
        uc.setDoInput(true);

        // Send post request
        DataOutputStream wr = new DataOutputStream(uc.getOutputStream());
        wr.writeBytes(postParams);
        wr.flush();
        wr.close();

        int responseCode = uc.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + postParams);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in =
                new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

    }

    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }

}