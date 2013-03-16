package SpeechRecogEngine;
/*
 * This code belongs to
 * Krishna Brahmam, Dept. of CSE, IIT Guwahati
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * A class for testing the SRE. 
 * @author Krishna Brahmam
 */
public class Test {

    private int order;
    private int frameLength;
    private int frameShift;
    private int min_T;
    private double[] weights;

    private Recognizer rc;

    /**
     * Class constructor for Test.
     * @param order     The order of the codebook
     */
    public Test(int order){
        this.order = order;
    }

    /**
     * Reads input (feature vectors) from file <code>filename</file> and records
     * in Vector <code>inputV</code>.
     * @param filename      The name of the file containing the feature vectors
     * @param inputV        The Vector that is to contain the feature vectors
     * @throws java.io.FileNotFoundException
     */
    public void readInput(String filename, Vector inputV) throws FileNotFoundException{
        String in;
        StringTokenizer strtok;
        Scanner scanner = null;

        int i;
        double[] temp;

        try {
            scanner = new Scanner(new File(filename)).useDelimiter("\\n");
        } catch (FileNotFoundException ex) {
            System.err.println("WARNING: "+ex.getMessage());
        }
        while(scanner.hasNext()){
            i = 0;
            temp = new double[order];
            in = scanner.next();
            strtok = new StringTokenizer(in,"\t");
            while(strtok.hasMoreTokens()){
                temp[i] = Double.parseDouble(strtok.nextToken());
                i++;
            }
            inputV.addElement(temp);
        }
        scanner.close();
    }

    /**
     * Tests the input feature vectors listed in <code>file</code> against the
     * codebook <code>output</code>.
     * @param file      The name of the file containing the feature vectors
     * @param output    The name of the codebook file
     */
    public void test(String file, String output){
        rc = new Recognizer(order, frameLength, frameShift, min_T, weights);
        rc.testDeltaCepstrum(file, output);
    }

    public static void main(String args[]) throws FileNotFoundException{
        int order = 2;
        int codebooksize = 16;

        Vector input = new Vector();
        Test testDemo = new Test(order);
        double[] w = null;

        /* VectorQuantizer vq = new VectorQuantizer(order, codebooksize, input, w);
        vq.generateCodeBook("11.cbk");*/
       testDemo.test("11.dat", "11.cbk");
    }
}