package SpeechRecogEngine;
/*
 * This code belongs to 
 * Krishna Brahmam, Dept. of CSE, IIT Guwahati
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;

/**
 * The Analyzer class extracts the feature vectors from the input speech samples, 
 * and generates a VQ codebook using Wav2TextConverter, LPCAnalyzer and VectorQuantizer.
 * @see Wav2TextConverter
 * @see LPCAnalyzer
 * @see VectorQuantizer 
 * @author Krishna Brahmam
 */
public class Analyzer {
    
    private Wav2TextConverter wv;
    private LPCAnalyzer lpc;
    private VectorQuantizer vq;
    
    private Vector input;       
    private int order;          // The order in LPC
    private int codeBookSize;   // The size of the codebook
    private int frameLength;    // The length of the frame
    private int frameShift;     // The shift between successive frames
    
    private Scanner s;
    private double[] weights;
    
    /**
     * Class constructor for Analyzer. 
     * @param order         The order in LPC
     * @param codeBookSize  The size of the codebook
     * @param frameLength   The length of the frame in LPC
     * @param frameShift    The shift or interval between successive frames
     * @param input         Container for holding the feature vectors extracted to be used for VQ codebook generation
     * @param weights       Weights for the Tokhura's distance. 'null' for Euclidean distance.
     */
    public Analyzer(int order, int codeBookSize, int frameLength, int frameShift, Vector input, double[] weights){
        this.order = order;
        this.frameLength = frameLength;
        this.frameShift = frameShift;
        this.codeBookSize = codeBookSize;
        this.input = input;
        this.weights = weights;
    }
    
    /**
     * Read the input from file <code>filename</code>. The input file here refers 
     * to the file containing the sample values of a speech signal. The sample values
     * are obtained with the help of Wav2TextConverter
     * @param filename  The name of the file from which the input has to be read
     * @see Wav2TextConverter
     */
    private void readInput(String filename){
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
     * Method to start vector quantization
     * @param codebook      The name of the file to store the codebook
     */
    private void runVectorQuantization(String codebook){
        vq = new VectorQuantizer(order,codeBookSize,input, weights);
        vq.generateCodeBook(codebook);
    }

    /**
     * Dumps the cepstral coefficients to file 'dump.dat'. Useful to calculate
     * the variances and thus the weights for Tokhura's distance.
     */
    private void dump(){
        try {
            BufferedWriter bf = new BufferedWriter(new FileWriter(new File("dump.dat")));
            double[] temp;
            for(int i=0;i<input.size();i++){
                temp = (double[]) input.elementAt(i);
                for(int j=0;j<temp.length;j++){
                    bf.write(temp[j]+"\t");
                }
                bf.newLine();
            }
            bf.flush();
            bf.close();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
    
    /**
     * Method to start analysis procedure. The method starts by extracting the sample values
     * into the file <code>data.dat</code>. These values are used by LPCAnalyzer to 
     * extract the feature vectors of the speech sample. After extracting the feature vectors 
     * a codebook is generated using the VectorQuantizer class. <code>dump()</code> dumps the
     * cepstral coefficients into a file by name 'dump.dat'
     * @param inputFileName     The name of the input file containing the list of speech samples
     * @param directory         The name of the directory storing the speech samples
     *                          listed in <code>inputFileName</code>
     * @param codebook          The name of the file to contain the codebook
     * @see LPCAnalyzer
     * @see VectorQuantizer
     */
    public void run(String inputFileName, String directory, String codebook) {
        String in;
        try {
            s = new Scanner(new File(inputFileName));
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null,"Could not find "+inputFileName, "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
            System.out.println("****");
        while(s.hasNext()){
            in = directory +"\\"+ s.next();
            System.out.println("Processing file: " + in);
            try {
                // Convert speech signal to text(sample values)
                wv = new Wav2TextConverter(in, "data.dat");
                wv.convert();
                // Extract the feature vectors
                lpc = new LPCAnalyzer(order,frameShift,frameLength,"cep.dat");
                lpc.start("data.dat");
                // Record the feature vectors
                readInput("cep.dat");
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                System.exit(-1);
            }
        }
        // Dump the cepstral coefficients
        dump();
        // Generate codebook using VQ
        runVectorQuantization(codebook);//*/
    }
}