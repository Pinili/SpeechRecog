package SpeechRecogEngine;
/*
 * This code belongs to 
 * Krishna Brahmam, Dept. of CSE, IIT Guwahati
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Recognizes the test speech samples using Wav2TextConverter, LPCAnalyzer. 
 * @see Wav2TextConverter
 * @see LPCAnalyzer
 * @author Krishna Brahmam
 */
public class Recognizer {
    
    private int order;              // The order in the LP analysis
    private Wav2TextConverter wv;   
    private int frameLength;        // The length of the frame
    private int frameShift;         // The interval between successive frames
    private int min_T;              // The minimum duration of speech required to recognize
    private LPCAnalyzer lpc;    
    private Scanner scanner;        
    private Vector input;           // The set of extracted feature vectors
    private Vector codebook;        // The VQ codebook
    private double[] weights;       // The set of weights to be used in Tokhura's distance
    
    /**
     * Class constructor for Recognizer
     * @param order         The order in LPC
     * @param frameLength   The length of the frame
     * @param frameShift    The shift interval between two successive frames
     * @param T             The number of observation symbols in a sequence
     */
    public Recognizer(int order, int frameLength, int frameShift, int T, double[] weights){
        this.input = new Vector();
        this.order = order;
        this.frameLength = frameLength;
        this.frameShift = frameShift;
        this.min_T = T;
        this.weights = weights;
    }
    
    /**
     * Reads the sample values of the test speech signal
     * @param file      The name of the file containing sample values of the test speech signal
     */
    private void readInput(String file){
        String in;
        StringTokenizer strtok;

        int i;
        double[] temp;
        
        try {
            scanner = new Scanner(new File(file)).useDelimiter("\\n");
        } catch (FileNotFoundException ex) {
            System.err.println("WARNING: "+ex.getMessage());
        }
        
        while(scanner.hasNext()){
            i = 0;
            temp = new double[order];
            in = scanner.next();
            strtok = new StringTokenizer(in," ");
            while(strtok.hasMoreTokens()){
                temp[i] = Double.parseDouble(strtok.nextToken());
                i++;
            }
            input.addElement(temp);
        }
        scanner.close();
    }

    /**
     * Reads input from file <code>file</code>. The input contains 'tab' as a delimiter.
     * @param file
     */
    private void readInputTab(String file){
        String in;
        StringTokenizer strtok;

        int i;
        double[] temp;

        try {
            scanner = new Scanner(new File(file)).useDelimiter("\\n");
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
            input.addElement(temp);
        }
        scanner.close();
    }

    /**
     * Extracts sample values from the test speech samples and obtains the feature vectors. 
     * Observation sequences are obtained for the test speech samples using the vector quantized 
     * codebook. 
     * @param filename      The name of the file containing the list of test samples
     * @param dir           The directory containing the test samples
     * @param observations  The observation sequences of each test sample
     * @param output        The output strings
     */
    public void run(String filename, String dir, Vector observations, String output){
        String in,f;
        Scanner s = null;
        double sum = 0;
        
        int[] temp;
        
        if(codebook == null){
            System.err.println("Loading codebook");
            loadCodeBook(output);
        }
        
        try {
            s = new Scanner(new File(filename));
        } catch (FileNotFoundException ex) {
            System.err.print(ex.getMessage());
            System.exit(-1);
        }
        while(s.hasNext()){
            f = s.next();
            in = dir +"/"+ f;
            sum = 0;
            try {
                // Convert WAV to text (sample values)
                wv = new Wav2TextConverter(in, "obs.dat");
                wv.convert();
                // Extract LPCC vectors
                lpc = new LPCAnalyzer(order, frameShift, frameLength, "cep.dat");
                lpc.start("obs.dat");
                readInput("cep.dat");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if(input.size() < min_T){
                System.err.println("Input too small. Discarding input.");
            }
            else{
                // Obtain the observation sequences
                temp = new int[input.size()];
                for(int i=0;i<input.size();i++){
                    temp[i] = q((double[]) input.elementAt(i));
                    sum += computeDistance((double[]) input.elementAt(i),(double[]) codebook.elementAt(temp[i]));
                }
                observations.addElement(temp);
            }
            sum = sum/input.size();
            System.out.println("Distortion for "+f+": "+sum);
            input.clear();
        }
    }

    /**
     * Tests a set of feature vectors by reading from file <code>filename</code>
     * against the codebook <code>output</code>.
     * @param filename      The name of the file that contains the feature vectors
     * @param output        The name of the codebook to be used for testing
     */
    public void testDeltaCepstrum(String filename, String output){
        double sum = 0;
        int[] temp;
        if(codebook == null){
            System.err.println("Loading codebook: "+ output);
            loadCodeBook(output);
        }
        readInputTab(filename);
        temp = new int[input.size()];
        for(int i=0;i<input.size();i++){
           temp[i] = q((double[]) input.elementAt(i));
           sum += computeDistance((double[]) input.elementAt(i),(double[]) codebook.elementAt(temp[i]));
        }
        sum = sum/input.size();
        System.out.println("Distortion for "+output+": "+sum);
        input.clear();
    }
    
    /**
     * Reads the binary codebook
     * @param file      The name of the binary codebook file
     */
    private void loadCodeBook(String file){
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(file));
            codebook = (Vector) inputStream.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Computes the distance between two feature vectors
     * <code>x</code> and <code>y</code>.
     * @param x     Feature Vector 1
     * @param y     Feature Vector 2
     * @return      The distance between two feature vectors
     *              <code>x</code> and <code>y</code>
     */
    private double computeDistance(double[] x, double[] y){
        double sum = 0;
        if(weights == null){
            weights = new double[order];
            for(int i=0;i<order;i++) weights[i] = 1;
        }
        if(weights.length != order){
            System.err.println("Incorrect weights");
            System.exit(-1);
        }
        for(int i=0;i<x.length;i++){
            sum += (weights[i]*Math.pow(x[i] - y[i],2));
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Vector quantizes the input feature vector <code>x</code>. 
     * @param x     The input feature vector 
     * @return      The index of the codebook vector that represents the region the input vector
     *              lies in
     */
    private int q(double[] x){
        if(codebook == null){
            System.out.println("Codebook not loaded");
            System.exit(-1);
        }
        int index = 0;
        double min_distance = computeDistance((double []) codebook.elementAt(index),x);
        double temp;
        // Compute the distance and find out the closest codebook vector
        for(int i=1;i<codebook.size();i++){
            temp = computeDistance((double[])codebook.elementAt(i),x);
            if(min_distance > temp){
                min_distance = temp;
                index = i;
            }
        }
        return index;
    }
}