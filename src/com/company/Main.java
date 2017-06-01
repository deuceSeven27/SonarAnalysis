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
import java.util.Arrays;
import java.util.Scanner;

public class Main {


    //if args = ["test"], defaults, else
    //must be fed in values
    public static void main(String[] args) throws Exception{

        String USERNAME = "";
        String PASSWORD = "";
        //array of assignments
        String[] ASSIGNMENTS = {};
        String[] questionsToSearchArr = {"barbecue" , "rgbstreet", "elevatorlimit", "tomekphone"};
        ArrayList<String> questionsToSearch = new ArrayList<String>(Arrays.asList(questionsToSearchArr));


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
        }

        //run in loop or comment out and run individually
        /*for(int exam = 1; exam < 4; exam++){
            for(int question = 1; question < 4; question++){
                ASSIGNMENT = "pracexam" + exam + "p" + question;
                downloadProject(USERNAME, PASSWORD, ASSIGNMENT);
            }
        }*/



        downloadProject(USERNAME, PASSWORD, "week5practice", questionsToSearch);

    }

    public static  void downloadProject(String USERNAME, String PASSWORD, String ASSIGNMENT, ArrayList<String> questionsToSearch) throws Exception{
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
        MainHelper.createDirectory("./" + ASSIGNMENT + "/meta", "meta");
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
        MainHelper.createDirectory("./" + ASSIGNMENT + "/tarGets", "tarGets");

        //now make request for each student
        for (String id : classList){
            // TODO: 6/1/2017 Here is where we check what they submitted and take questions we want
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

            if (revisions.size() < 1){
                System.out.println("No submissions for " + id);
                continue; //skip this student if no revisions
            }

            //go to that page with the latest revision
            studentPage = wr.getPage("https://cs.adelaide.edu.au/services/websubmission/" + revisions.get(0).attr("href"));

            d = Jsoup.parse(studentPage);

            // TODO: 6/1/2017 Here we check the html table with marks
            // TODO to determine if they did that question

            String studentEntry = MainHelper.checkIfHtmlTableContains(d, questionsToSearch);

            if(studentEntry == null){
                System.out.println("No submissions for our questions for this student");
                continue;
            }

            //now do the download for the student
            System.out.println("Downloading for " + id + "...");

            //finally download the source tar file
            URL dlLink = new URL("https://cs.adelaide.edu.au/services/websubmission/download.php?download_file=exported.tgz");
            ReadableByteChannel rbc = Channels.newChannel(dlLink.openStream());
            FileOutputStream fos = new FileOutputStream("./" + ASSIGNMENT + "/tarGets/" + id + "_" + studentEntry + ".tgz");
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

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

        //check if the html table on the websub feedback page
        //for the student has questions we are looking for
        //if at least 1 looked for question is inside, then return
        //a string, of the studentid,  name of the question, and the mark, with delimiters
        // like axxxxxxx_(Socks-50_Elevator-20).tgz
        public static String checkIfHtmlTableContains(Document htmlPage, ArrayList<String> questionLookingFor){

            //get the table element
            Element marksTable = htmlPage.getElementsByTag("tbody").get(2);
            /*if(marksTable.size() < 1){
                System.out.println("no tables detected");
                return null;
            }*/



            Elements rows = marksTable.getElementsByTag("tr");

            StringBuilder sb = new StringBuilder("");

            //each element text() like "Barbecue 100"
            for (int i = 1 /*skip the header row*/; i < rows.size(); i++ ){
                System.out.println(rows.get(i).text());
                String[] parts = rows.get(i).text().split("\\s+"); //split on whitespace

                if( questionLookingFor.contains(parts[0].toLowerCase()) ){
                    sb.append(parts[0] + "-" + parts[1] + "_");
                }
            }


            if (sb.toString().equals("")){
                System.out.println("Nothing detected");
                return null; //return null if none found
            }else{

                sb.deleteCharAt(sb.length() - 1); //delete the last underscore _
                return sb.toString();
            }


        }
    }




}
