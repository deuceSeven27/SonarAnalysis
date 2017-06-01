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

        if(args.length != 1){
            System.out.println("Please input the classlist!");
            System.exit(1);
        }

        String USERNAME = "a1675993";
        String PASSWORD = "rajeshdurai27$$$";
        String ASSIGNMENT = "";
        String COURSE = "adsa";
        String SEMESTER = "s2";
        String YEAR = "2015";


        downloadProject(USERNAME, PASSWORD, YEAR, COURSE, SEMESTER, ASSIGNMENT, args[0]);

    }

    public static  void downloadProject(String USERNAME, String PASSWORD, String YEAR, String COURSE, String SEMESTER, String ASSIGNMENT, String classListPath) throws Exception{
        WebAuthAndRequest wr = new WebAuthAndRequest();

        //traversing the login page
        String loginPage = wr.getPage("https://login.adelaide.edu.au/cas/login" +
                "?service=https%3A%2F%2Fmyuni.adelaide.edu.au%2Flogin%2Fcas");

        CookieHandler.setDefault(new CookieManager());

        String postParams = wr.getLoginElements(loginPage, USERNAME, PASSWORD);

        wr.sendPost("https://login.adelaide.edu.au/cas/login?service=https%3A%2F%2Fmyuni.adelaide.edu.au%2Flogin%2Fcas", postParams);


        //navigate the php
        //select the course details
        wr.getPage("https://cs.adelaide.edu.au/services/websubmission/?sub_year=" + YEAR + "&sub_period=" + SEMESTER + "&sub_course=" + COURSE + "&sub_assign=" + ASSIGNMENT);
        //go to the classlist page
        wr.getPage("https://cs.adelaide.edu.au/services/websubmission/?menu=Classlist");


        /*
        *
        * WARNING GET CLASS LIST MANUALLY!
        *
        * */
        //get each student so we can make requests for their feedback pages
        ClassListParser clp = new ClassListParser();

        //create meta directory
        MainHelper.createDirectory("./" + ASSIGNMENT + "/meta", "meta");

        ArrayList<String> classList; // stores the id numbers of students

        try{
            classList = clp.readAndParseClassList(classListPath);

            //create tarGets directory
            MainHelper.createDirectory("./" + ASSIGNMENT + "/tarGets", "tarGets");

            //now make request for each student
            for (String id : classList){
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

                for (Element e : revisions){
                    System.out.println(e.text());
                    System.out.println(e.attr("href") + "\n");
                }


                //select the latest revision if it exists
                if(revisions.size() > 0){
                    System.out.println("Downloading for " + id + "...");
                    String revMark = MainHelper.getMark(revisions.get(0).text());

                    studentPage = wr.getPage("https://cs.adelaide.edu.au/services/websubmission/" + revisions.get(0).attr("href"));

                    //finally download the source tar file
                    URL dlLink = new URL("https://cs.adelaide.edu.au/services/websubmission/download.php?download_file=exported.tgz");
                    ReadableByteChannel rbc = Channels.newChannel(dlLink.openStream());
                    FileOutputStream fos = new FileOutputStream("./" + ASSIGNMENT + "/tarGets/" + id + "_" + revMark + ".tgz");
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }else{
                    // TODO: 14/05/17 keep track of non-submissions in a file?
                    System.out.println("No submission for " + id);
                }

            }
        }catch(FileNotFoundException f){
            //default to csv file we stored
           f.printStackTrace();
           System.exit(1);
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
    }

}
