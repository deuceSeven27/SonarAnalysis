package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception{

        String USERNAME = "";
        String PASSWORD = "";
        String ASSIGNMENT = "";

        WebAuthAndRequest wr = new WebAuthAndRequest();


        if(args.length > 0){
            USERNAME = "a1675993";
            PASSWORD = "rajeshdurai27$$$";
            ASSIGNMENT = "pracexam1p2";
        }else{
            //get username
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter username: ");
            USERNAME = sc.nextLine();
            //get password, but don't display on screen
            /*Console c;
            char[] passwordChar;
            //get the console to hide input
            if(((c = System.console()) != null) && (passwordChar = c.readPassword("%s", "Password: ")) != null){
                PASSWORD = passwordChar.toString();
            }else{
                System.out.println("No password entered! Exiting...");
                System.exit(0);
            }*/
            System.out.println("Enter password: ");
            PASSWORD = sc.nextLine();

            System.out.println("Enter assignment php url: ");
            ASSIGNMENT = sc.nextLine();
        }



        String loginPage = wr.getPage("https://login.adelaide.edu.au/cas/login" +
                "?service=https%3A%2F%2Fmyuni.adelaide.edu.au%2Flogin%2Fcas");

        CookieHandler.setDefault(new CookieManager());

        String postParams = wr.getLoginElements(loginPage, USERNAME, PASSWORD);

        wr.sendPost("https://login.adelaide.edu.au/cas/login?service=https%3A%2F%2Fmyuni.adelaide.edu.au%2Flogin%2Fcas", postParams);


        //navigate the php
        //select the course details
        wr.getPage("https://cs.adelaide.edu.au/services/websubmission/?sub_year=2015&sub_period=s2&sub_course=pssd&sub_assign=" + ASSIGNMENT);
        //go to the classlist page
        wr.getPage("https://cs.adelaide.edu.au/services/websubmission/?menu=Classlist");


        //get each student so we can make requests for their feedback pages
        ClassListParser clp = new ClassListParser();

        //create meta directory
        MainHelper.createDirectory("./" + ASSIGNMENT + "/meta", "meta");
        //get the classList file

        ArrayList<String> classList = clp.readAndParseClassList("./" + ASSIGNMENT +  "/meta/students.csv");
        for (String s : classList){
            System.out.println(s);
        }

        //create tarGets directory
        MainHelper.createDirectory("./" + ASSIGNMENT + "/tarGets", "tarGets");

        //now make request for each student
        int processCount = 1;

        for (String id : classList){
            System.out.println("Processing " + processCount + "/" + classList.size() + ": " + id);
            //this gets to their feedback page
            String studentPage = wr.getPage("https://cs.adelaide.edu.au/services/websubmission/?menu=" +
                    "View%20Feedback&sub_output_select=feedback&sub_alt_user=" + id);
            //then select the svn revision we need for that user
            Document d = Jsoup.parse(studentPage);

            /*
            select the revisions by targeting like
            * <a href="?sub_output_select=revision-279"> Aug 20 10:36 r279(100) </a>
            * */
            Elements revisions = d.select("a[href*=revision]");

            //get revisions only if work has been submitted at all
            if(revisions.size() > 0){
                //select the latest revision
                studentPage = wr.getPage("https://cs.adelaide.edu.au/services/websubmission/" + revisions.get(0).attr("href"));

                //finally download the source tar file

                System.out.println("Downloading latest submission for " + id + "...");
                URL dlLink = new URL("https://cs.adelaide.edu.au/services/websubmission/download.php?download_file=exported.tgz");
                ReadableByteChannel rbc = Channels.newChannel(dlLink.openStream());
                FileOutputStream fos = new FileOutputStream("./" + ASSIGNMENT + "/tarGets/" + id + ".tgz");
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                System.out.println("Download succeeded!\n");
            }

            processCount++;

        }

    }

    private static class MainHelper{
        public static void createDirectory(String path, String folderName){
            File dest = new File(path);
            if(!dest.exists()){
                System.out.println("Creating directory structure for " + folderName);
                if(dest.mkdirs()){
                    System.out.println( folderName + " directory created.");
                }else{
                    System.out.println("Failed to create directory, or parent directories already created...");
                }
            }
        }
    }

}
