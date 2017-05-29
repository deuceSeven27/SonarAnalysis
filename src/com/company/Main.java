package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {


    //if args = ["test"], defaults, else
    //must be fed in values
    public static void main(String[] args) throws Exception{

        String USERNAME = "";
        String PASSWORD = "";
        String ASSIGNMENT = "";


        if(args[0].equals("test")){
            USERNAME = "a1675993";
            PASSWORD = "rajeshdurai27$$$";

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

        //run in loop or comment out and run individually
        /*for(int exam = 1; exam < 4; exam++){
            for(int question = 1; question < 4; question++){
                ASSIGNMENT = "pracexam" + exam + "p" + question;
                downloadProject(USERNAME, PASSWORD, ASSIGNMENT);
            }
        }*/



        downloadProject(USERNAME, PASSWORD, "pracexam2p3");

    }

    public static  void downloadProject(String USERNAME, String PASSWORD, String ASSIGNMENT) throws Exception{
        WebAuthAndRequest wr = new WebAuthAndRequest();

        //traversing the login page
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
        MainHelper.createDirectory("./" + ASSIGNMENT + "/meta");
        //TODO: get the class list online
        String csvPath = "./" + ASSIGNMENT + "/meta/students.csv";

        ArrayList<String> classList; // stores the id numbers of students

        try{
            classList = clp.readAndParseClassList(csvPath);
        }catch(FileNotFoundException f){
            //default to csv file we stored
            System.out.println("No file at " + csvPath + ".\n" + "Using default file...\n");
            classList = clp.readAndParseClassList("./students.csv");
        }

        for (String s : classList){
            System.out.println(s);
        }

        //create tarGets directory
        MainHelper.createDirectory("./" + ASSIGNMENT + "/tarGets");

        int counter = 1;
        //now make request for each student
        for (String id : classList){
            //this gets to their feedback page
            String studentPage = wr.getPage("https://cs.adelaide.edu.au/services/websubmission/?menu=" +
                    "View%20Feedback&sub_output_select=feedback&sub_alt_user=" + id);
            //then select the svn revision we need for that user
            Document d = Jsoup.parse(studentPage);
            System.out.println("Downloading for " + id + "... (" + counter + "/" + classList.size() + ")");
            counter++;

            /*
            select the revisions by targeting like
            * <a href="?sub_output_select=revision-279"> Aug 20 10:36 r279(100) </a>
            * */
            Elements revisions = d.select("a[href*=revision]");

            /*for (Element e : revisions){
                System.out.println(e.text());
                System.out.println(e.attr("href") + "\n");
            }*/

            // TODO: 5/29/2017 download all submissions rather than just most recent one
            if(revisions.size() > 0){
                //make dir for this student
                MainHelper.createDirectory("./" + ASSIGNMENT + "/tarGets/" + id);
                int revNumber = revisions.size() - 1;
                for (Element e : revisions){

                    /*for each submission, */
                    System.out.println(id + ", (" + (revisions.size() - revNumber) + "/" + revisions.size() + ") : " + e.text() + "...");

                    String revMark = MainHelper.getMark(e.text());

                    studentPage = wr.getPage("https://cs.adelaide.edu.au/services/websubmission/" + e.attr("href"));

                    //finally download the source tar file
                    URL dlLink = new URL("https://cs.adelaide.edu.au/services/websubmission/download.php?download_file=exported.tgz");
                    ReadableByteChannel rbc = Channels.newChannel(dlLink.openStream());
                    FileOutputStream fos = new FileOutputStream("./" + ASSIGNMENT + "/tarGets/" + id + "/" + id + "-" + revNumber + "_" + revMark + ".tgz");
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    revNumber--;
                }

            }else{
                // TODO: 14/05/17 keep track of non-submissions in a file?
                System.out.println("No submission for " + id);
            }

        }
    }

    private static class MainHelper{
        public static void createDirectory(String path){
            File dest = new File(path);
            if(!dest.exists()){
                if(dest.mkdirs()){
                    System.out.println( path + " directory created.");
                }else{
                    System.out.println("Failed to create directory, or parent directories already created...");
                }
            }
        }
        //gets the mark from the string showing the revision from websubmission
        //text looks like Aug 20 10:55 r115(100)
        public static String getMark(String text){

            StringBuilder sb = new StringBuilder(text);
            int startBracket = sb.indexOf("(");
            sb.delete(0, startBracket + 1);

            int endBracket = sb.indexOf(")");
            sb.deleteCharAt(endBracket);

            return sb.toString();
        }
    }

}
