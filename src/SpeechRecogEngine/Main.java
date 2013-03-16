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
 * The Main class contains methods to train and test the system. These methods 
 * provide interfaces to other classes such as HMMAnalyzer and HMMRecognizer. 
 * 
 * @see HMMAnalyzer 
 * @see HMMRecognizer
 * @author Krishna Brahmam
 */
public class Main {

    private int order;              // The order in Linear Predictive Analysis 
    private int codeBookSize;       // The size of the VQ codebook: the number of codebook vectors 
    private int frameLength;        // The length of a frame in LPA
    private int frameShift;         // The shift in time between two successive frames

    private int N;                  // No. of states in a HMM
    private int min_T = 50;         // The minimum duration of speech required for training
    
    private Recognizer rc;          
    private HMMAnalyzer hmm;
    private double[] weights;
    
    /**
     * Class Constructor of Main. 
     * @param order         The order in Linear Predictive Analysis
     * @param codeBookSize  The size of the VQ codebook
     * @param frameLength   The length of a frame in LPA
     * @param frameShift    The shift in time between two successive frames
     */
    public Main(int order, int codeBookSize, int frameLength, int frameShift, int N, double[] weights){
        this.order = order;
        this.codeBookSize = codeBookSize;
        this.frameLength = frameLength;
        this.frameShift = frameShift;
        this.weights = weights;
        this.N = N;
    }
    
    /**
     * Method to train a system. Analyzes the input set of speech samples using 
     * LPA and feature vectors are extracted and a VQ codebook is generated. If
     * <code>mode</code> is <code>true</code> then the HMMs are generated after 
     * LPA using the VQ codebook generated. Feature extraction and codebook 
     * generation are performed using the Analyzer class whereas the HMMs are 
     * built using the Recognizer and HMMAnalyzer classes.
     * 
     * @param codebook  The name of the codebook file
     * @param index     The name of the index file
     * @param train     The name of the file containing a list of training speech samples
     * @param train_dir The name of the directory containing the training speech samples
     * @param test_dir  The name of the directory containing the test speech samples. 
     *                  The test speech samples are used for generating HMMs.
     * @param mode      If true, then HMMs are generated. Else stops after VQ 
     *                  codebook generation.
     * @see Analyzer
     * @see Recognizer
     * @see HMMAnalyzer
     */
    private int train(String codebook, String index, String train, String train_dir, String test_dir, boolean mode){
        Scanner s;
        Analyzer an;
        String in;
        String out;
        int i = 0;
        Vector obs = new Vector();      // Vector to store the observation sequences
        Vector input = new Vector();    // Vector to store the feature vectors
        
        try {
            // Extracting the feature vectors
            if (!new File(codebook).exists()) {
                an = new Analyzer(order, codeBookSize, frameLength, frameShift, input, weights);
                an.run(train, train_dir, codebook);
            }
            if(mode){
                i = 0;
                hmm = new HMMAnalyzer(min_T, N, true);
                rc = new Recognizer(order, frameLength, frameShift, min_T, weights);
                s = new Scanner(new File(index));       // Open index file
                // Read names of the files and generates the corresponding HMMs
                while (s.hasNext()) {
                    in = s.next();
                    out = s.next();
                    System.out.println(in + " => " + out);
                    
                    // Get observation sequences from the test speech samples and codebook
                    rc.run(in, test_dir, obs, codebook);
                    // Generate HMM for the set of observation sequences
                    hmm.run(obs, codeBookSize, out);
                    obs.clear();
                    i++;
                }
                hmm.close();
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        }
        return i;
    }
    
    /**
     * Method to train a system. Analyzes the input set of speech samples using 
     * LPA and feature vectors are extracted and a VQ codebook is generated. The 
     * <code>mode</code> is set <code>true</code> so that the HMMs are generated 
     * after LPA using the VQ codebook generated. Feature extraction and codebook 
     * generation are performed using the Analyzer class whereas the HMMs are built 
     * using the Recognizer and HMMAnalyzer classes.
     * 
     * @param codebook  The name of the codebook file
     * @param index     The name of the index file
     * @param train     The name of the file containing a list of training speech samples
     * @param train_dir The name of the directory containing the training speech samples
     * @param test_dir  The name of the directory containing the test speech samples. 
     *                  The test speech samples are used for generating HMMs.
     * @see Analyzer
     * @see Recognizer
     * @see HMMAnalyzer
     */
    public int train(String codebook, String index, String train, String train_dir, String test_dir){
        // Internally, call the private train method with <code>mode</code> set to <code>true</mode>
        return train(codebook, index, train, train_dir, test_dir, true);
    }
    
    /**
     * Method to train a system. Analyzes the input set of speech samples using 
     * LPA and feature vectors are extracted and a VQ codebook is generated. The 
     * <code>mode</code> is set to <code>false</code>. Feature extraction and codebook 
     * generation are performed using the Analyzer class.
     * 
     * @param codebook
     * @param train
     * @param train_dir
     * @see Analyzer
     */
    public void train(String codebook, String train, String train_dir){
        train(codebook,"", train, train_dir, "", false);
    }
    
    /**
     * Method to test the HMMs generated in the training phase. Uses the Recognizer class 
     * to extract the observation sequences and HMMRecognizer to recognize using HMMs.
     * 
     * @param file      The test file containing the list of test speech sampels
     * @param test_dir  The name of the directory containing the test speech samples
     * @param cb        The name of the codebook
     * @param index     The name of the index file
     * @param output    The array of output strings to be printed after recognition
     * @see Recognizer
     * @see HMMRecognizer
     */
    public void test(String file, String test_dir, String cb, String index, String[] output){
        Vector obs = new Vector();
        rc = new Recognizer(order, frameLength, frameShift, min_T, weights);
        // Record the observation sequences in Vector <code>obs</code>
        rc.run(file, test_dir, obs, cb);
        // Recognize the test data using the codebook <code>cb</code> and the observation sequences
        HMMRecognizer hmmr = new HMMRecognizer(N, cb, index, output);
        for(int i=0;i<obs.size();i++){
            hmmr.recognize((int[]) obs.elementAt(i));
        }
    }

    /**
     * Test using only VQ
     * @param file
     * @param dir
     * @param cb
     */
    public void test(String file, String dir, String cb){
        Vector obs = new Vector();
        rc = new Recognizer(order, frameLength, frameShift, min_T, weights);
        // Record the observation sequences in Vector <code>obs</code>
        rc.run(file, dir, obs, cb);
    }

    /**
     * Test using only VQ against the codebook formed using deltaCepstrums. In general,
     * the coefficients are taken directly from the file <code>filename</code> and tested
     * against the codebook <code>cb</code>
     * @param filename      The name of the file that contains the input feature vectors
     * @param cb            The name of the codebook to be used for testing
     */
    public void testDeltaCepstrum(String filename, String cb){
        rc = new Recognizer(order, frameLength, frameShift, min_T, weights);
        // Record the observation sequences in Vector <code>obs</code>
        rc.testDeltaCepstrum(filename, cb);
    }

    /**
     * Reads input (feature vectors) from the file <code>filename</code> into the
     * Vector <code>inputV</code>.
     * @param filename      The name of the file that contains the feature vectors
     * @param inputV        The Vector that is to contain the feature vectors
     */
    public void readInput(String filename, Vector inputV){
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
     * The main method
     * @param args
     */
    public static void main(String[] args){
            int order = 12;             // Order in LPC = 12
            int codeBookSize = 64;      // Size of codebook
            int frameLength = 300;      // Length of frame in LPC
            int frameShift = 100;

            // String t is the weights given as a string, each weight separated
            // by a space.
            String t="1 4       9       16      25      36      49      56.25   64      72.25   81";

            double[] w = new double[12];
            StringTokenizer strtok = new StringTokenizer(t,"\t");
            int i=0;
            while(strtok.hasMoreTokens()){
                w[i] = Double.parseDouble(strtok.nextToken());
                i++;
            }
            
            Vector input = new Vector();
            // Strings to be output when recognized
//          String[] output = {"a","j","k","s","v"};
            Main m = new Main(order, codeBookSize, frameLength, frameShift, 6, w);
            // Train the system
            //m.train("v_dcep.cbk", "index.dat", "v.dat", "C:\\new", "C:\\new");
            //m.train("random.cbk", "files.dat", "C:\\new data");
            //m.readInput("dcep.dat", input);
            VectorQuantizer vq = new VectorQuantizer(order, codeBookSize, input, w);
            vq.generateCodeBook("v2_dcep.cbk");
            // Test the system
//           m.test("test.dat", "C:\\Documents and Settings\\Krishna Brahmam\\Desktop\\BTP2\\new", "a_cep.cbk","index.dat",output);
//           m.test("test.dat", "C:\\Documents and Settings\\Krishna Brahmam\\Desktop\\BTP2\\new", "a2_dcep.cbk");
//           m.testDeltaCepstrum("dcep.dat", "v2_dcep.cbk");
    }
}