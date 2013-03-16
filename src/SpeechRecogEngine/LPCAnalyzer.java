package SpeechRecogEngine;
/*
 * This code belongs to 
 * Krishna Brahmam, Dept. of CSE, IIT Guwahati
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Extracts the Linear Predictive Cepstral Coefficients by taking the input file 
 * of sample values extracted by Wav2TextConverter.
 * @see Wav2TextConverter
 * @author Krishna Brahmam
 */ 
public class LPCAnalyzer {
    
    private int p;
    private double[] e;         
    private double[][] alpha;
    private double[] r;         // Auto-correlation values
    private double[] k;         // PARCOR coefficients
    private double[] c;         // Cepstral coefficients
    private Vector s;
    private Vector in;
    
    private double[] x;
    private double[] lpc;       // LP coefficients
    private int N;              // The length of a frame
    private int M;              // The shift or interval between successive frames 
    
    private BufferedWriter bfwr;
    
    /**
     * Class constructor for LPCAnalyzer
     * @param p         The oreder in LPC
     * @param M         The shift between successive frames
     * @param N         The length of a frame
     * @param output    The name of the file where cepstral coefficients are to
     *                  be recorded
     */
    public LPCAnalyzer(int p, int M, int N, String output){
        this.p = p;     // order
        this.M = M;     // shift
        this.N = N;     // frame length
        
        // Variable used in Durbin's algorithm
        e = new double[p+1];
        alpha = new double[p+1][p+1];
        r = new double[p+1];
        k = new double[p+1];
        c = new double[p+1];
        s = new Vector();
        in = new Vector();
        
        x = new double[N];
        lpc = new double[p];
        
        try {
            bfwr = new BufferedWriter(new FileWriter(output));
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    /**
     * Starts the linear predictive analysis by reading the sample values from 
     * <code>input</code>, normalizing it and processing it for the maximum 
     * no. of frames available.
     * @param input     
     * @throws java.io.IOException
     */
    public void start(String input) throws IOException{
        readSamples(input);
        //processSamples();
        process(((s.size()-N)/M)+1);
        bfwr.close();
    }
    
    
    
    /**
     * The Linear Predictive Analysis of a speech signal with <code>frame</code> frames.
     * @param frames    The number of frames to be used in LPC
     */
    private void process(int frames){
        for(int l=0;l<frames;l++){
            initialize();
            applyWindow(l);
            autoCorrelate();
            if(r[0] == 0){
                System.err.println("A unique solution does not exist");
                System.exit(-1);
            }
            else{
                e[0] = r[0];
            }
            LPCAnalysis();
            extractSolution();
            calculateCepstralCoefficients();
            writeResult();
        }
    }
    
    /**
     * Records the solutions in a separate array to be used for calculating
     * cepstral coefficients
     */
    private void extractSolution(){
        for(int i=0;i<p;i++){
            lpc[i] = alpha[i+1][p];
        }
    }
    
    /**
     * LPC Analysis using Durbin's algorithm
     */
    private void LPCAnalysis(){
        int i,j;
        double sum;
        for(i = 1;i <= p;i++){
            sum = 0;
            for(j = 1;j <= i-1;j++){
                sum += (alpha[j][i-1]*r[i-j]);
            }
            k[i] = (r[i] - sum)/e[i-1];
            alpha[i][i] = k[i];
            for(j=1;j<=i-1;j++){
                alpha[j][i] = alpha[j][i-1] - k[i]*alpha[i-j][i-1];
            }
            e[i] = (1 - k[i]*k[i]) * e[i-1];
        }
    }
    
    /**
     * Calculating cepstral coefficients from the linear predicitve coefficients
     */
    private void calculateCepstralCoefficients(){
        int i,j;
        double sum;
        for(i=0;i<c.length;i++){
            c[i] = 0;
        }
        for(i=1;i<c.length;i++){
            sum = 0;
            for(j=1;j<=i-1;j++){
                sum += ((j/(double) i)*c[j]*lpc[i-j-1]);
            }
            c[i] = lpc[i-1] + sum;
        }
    }
    
    /**
     * Initializing the arrays x, e, k, alpha
     */
    private void initialize(){
        for(int i=0;i<x.length;i++){
            x[i] = 0;
        }
        for(int i=0;i<p+1;i++){
            e[i] = k[i] = 0;
            for(int j=0;j<p+1;j++){
                alpha[i][j] = 0;
            }
        }
    }
    
    /**
     * Applies Hamming window to the frame numbered <code>L</code>
     * @param L The frame number 0, 1, 2...
     */
    private void applyWindow(int L){
        int n;
        for(int i = 0;i < N;i++){
            n = M*L + i;
            x[i] = ((Double ) s.elementAt(n)).doubleValue() * hammingWindow((double) i);
        }
    }
    
    /**
     * Performs calculations of the Hamming function for a given <code>n</code>
     * @param n The value for which the Hamming function has to be calculated
     * @return  The value of the evaluated Hamming function for the given value of n
     */
    private double hammingWindow(double n){
        return 0.54 - 0.46*Math.cos(2*Math.PI*n/(N-1));
    }
    
    /**
     * Auto-correlation method
     */
    private void autoCorrelate(){
        int i,j;
        for(i=0;i<p+1;i++){
            r[i] = 0;
            for(j=0;j<N-i;j++){
                r[i] += (x[j]*x[j+i]);
            }
        }
    }
    
    /**
     * Reads the sample values from the output of Wav2TextConverter.
     * @param filename  The name of the file containing the sample values
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    private void readSamples(String filename) throws FileNotFoundException, IOException{
        BufferedReader bfr = new BufferedReader(new FileReader(filename));
        String temp;
        while(bfr.ready()){
            temp = bfr.readLine();
            s.addElement(Double.parseDouble(temp));
        }
    }

    /**
     * A method supposed to extracts 'words' or spoken speech from a .wav file.
     * More functionality to be added. NOT TO BE USED NOW
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    private void processSamples() throws FileNotFoundException, IOException{
        BufferedReader bfr = new BufferedReader(new FileReader("mark.dat"));
        String temp;
        StringTokenizer strtok;
        int a,b;
        while(bfr.ready()){
            temp = bfr.readLine();
            strtok = new StringTokenizer(temp,"\t");

            a = Integer.parseInt(strtok.nextToken());
            b = Integer.parseInt(strtok.nextToken());

            System.out.println("Processing samples from "+a+" to "+b);
            for(int i=a;i<=b;i++){
                in.addElement(s.elementAt(i));
            }
            process(((in.size()-N)/M)+1);
            in.clear();
        }
    }
    
    /**
     * Writes or records the linear predicitve cepstral coefficients
     */
    private void writeResult(){
        try {
            for (int i = 1; i < c.length; i++) {
                bfwr.write(String.format("%f ", c[i]));
            }
            bfwr.write("\n");
            bfwr.flush();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}