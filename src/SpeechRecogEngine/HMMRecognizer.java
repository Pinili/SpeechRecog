package SpeechRecogEngine;
/*
 * This code belongs to 
 * Krishna Brahmam, Dept. of CSE, IIT Guwahati
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Scanner;
import java.util.Vector;


/**
 * Recognizes the speech signal using the observation sequences, codebook and
 * the saved HMMs.
 * @author Krishna Brahmam
 */
public class HMMRecognizer {
    private Vector codebook;        // The VQ codebook
    private Vector models;          // The saved models
    
    private Vector tempModels;
    private Vector tempModelNames;
    
    private int N;                  // The number of states in an HMM
    private HMMAnalyzer hmm;        
    private String index;           // The name of the index file
    private String cb;              // The name of the codebook file
    private String[] output;        // The array of output strings
    private BufferedWriter bfw;
    
    private int current;
    
    /**
     * Class constructor for HMMRecognizer. 
     * @param N         The number of states in an HMM
     * @param cb        The name of the codebook file
     * @param index     The name of the index file
     * @param output    The array of output strings
     */
    public HMMRecognizer(int N, String cb, String index, String[] output){
        this.N = N;
        this.cb = cb;
        this.index = index;
        this.output = output;
        
        this.models = new Vector();
        this.tempModels = new Vector();
        this.tempModelNames = new Vector();
    }

    /**
     * Reads the binary codebook saved during vector quantization in VectorQuantizer
     * @param file      The name of the file containing the codebook
     * @see VectorQuantizer
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
     * Reads the binary models saved in HMMAnalyzer. The method calls loadModel()
     * for each model name listed in the <code>index</code> file.
     * @param index     The name of the index file
     * @see HMMAnalyzer
     */
    private void loadModels(String index, Vector model){
        String temp;
        Scanner s;
        try {
            s = new Scanner(new File(index));
            while(s.hasNext()){
                s.next();
                temp = s.next();
                loadModel(temp,model);
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        }
    }

    /**
     * Loads the temporary(intermediate) models listed in the file <code>index</code>
     * into the Vector <code>model</code>.
     * @param index     The name of the file containing the list of HMMs
     * @param model     The vector that contains the intermediate models listed
     *                  in <code>index</code>.
     */
    private void loadTempModels(String index, Vector model){
        Scanner s;
        String temp;
        System.err.println("Loading temp models");
        try {
            s = new Scanner(new File(index));
            while(s.hasNext()){
                temp = s.next();
                tempModelNames.addElement(temp);
                loadModel(temp,model);
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        }
        System.err.println("Finished loading temp models");
    }
    
    /**
     * Reads the binary model <code>modelname</code> stored in the filesystem. The 
     * models read are stored in the Vector <code>models</code>
     * @param modelname     The name of the model that is to be read
     */
    private void loadModel(String modelname, Vector model){
        
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(modelname));
            model.addElement(inputStream.readObject());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    /**
     * Records the observation sequence in <code>obs[]</code> using an HMMAnalyzer
     * @param hmm   The HMMAnalyzer used to obtain the observation sequence
     * @param obs   The array that is to contain the observation sequence
     * @see HMMAnalyzer
     */
    private void record(HMMAnalyzer hmm, int[] obs){
        Model m;
        String modelName;
        try {
            bfw = new BufferedWriter(new FileWriter(current+".res"));
            current++;
            for(int i=0;i<tempModels.size();i++){
                m = (Model) tempModels.elementAt(i);
                modelName = (String) tempModelNames.elementAt(i);
                bfw.write(modelName + "\t" + hmm.forwardProcedure(m, obs));
                bfw.newLine();
                
            }
            bfw.flush();
            bfw.close();
        } catch (IOException ex) {
                System.err.println(ex.getMessage());
        }
    }
    
    
    /**
     * Carries out the recognition by loading the codebook and models. The probability 
     * of the observation sequence given each model is found and the one with the highest 
     * probability is selected from the <code>output</code> array of Strings.
     * @param obs       The observation sequence
     */
    public void recognize(int[] obs){
        Model m;
        double max = 0;
        int max_index = -1;
        double prob;
        hmm = new HMMAnalyzer(obs.length,N, false);
        
        if(codebook == null){
            System.err.println("Loading codebook");
            loadCodeBook(cb);
        }
        if(models.isEmpty() || tempModels.isEmpty()){
            System.err.println("Loading models");
            loadModels(index, models);
            //loadTempModels("HMMList", tempModels);
        }
        
        record(hmm, obs);
        
        // Calculate the probabilities of the observation sequence given each model
        for(int i=0;i<models.size();i++){
            m = (Model) models.elementAt(i);
            prob = hmm.forwardProcedure(m, obs);
            //System.out.println(prob);
            if(max < prob){
                max = prob;
                max_index = i;
            }
        }
        // Recognize the output as the one with the highest probability and print the corresponding output
        if(max_index >= 0) System.out.println("RECOGNIZED AS: "+output[max_index]);
        else{
            System.out.println("Sorry, cannot recognize.");
        }
    }
}