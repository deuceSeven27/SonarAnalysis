package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Created by rajeshdurai on 5/04/17.
 */
/*feed this class the students.csv file from the websubmission classlist page
* and it parses and returns an ArrayList with student ids*/
public class ClassListParser {

    public ArrayList<String> classList = new ArrayList<String>();

    /*returns a list of student IDs to be parsed*/
    public ArrayList<String> readAndParseClassList(String file) throws FileNotFoundException{

        String line;

        File studentList = new File(file);

        if(!studentList.exists()){
           throw new FileNotFoundException();
        }

        try(BufferedReader br = new BufferedReader(new FileReader(file))){

            while((line = br.readLine()) != null){
                String[] parts = line.split(",");
                //don't include non-enrolments & lecturer code
                if(!(parts[2].equals("\"0000_Submission - No Enrolment\""))){
                    classList.add(parts[0]);
                }

            }

        }catch(Exception e){
            System.err.println(e.getMessage());
        }
        classList.remove(0); //remove the header from the list
        return classList;
    }

}
