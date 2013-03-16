package SpeechRecogEngine;
/*
 * This code belongs to 
 * Krishna Brahmam, Dept. of CSE, IIT Guwahati
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Vector;

/**
 * Builds the Hidden Markov Models using the Forward Procedure, Backward Procedure,
 * Viterbi algorithm and Expectation Modification.
 * @author Krishna Brahmam
 */
public class HMMAnalyzer {
    
    private final boolean DEBUG = false;
    
    private double[][] alpha;           
    private double[][] beta;
    private double[][] gamma;
    private double[][][] xi;
    private int[] stateSequence;
    private int min_T;
    private int N;
    private int qStar;
    private BufferedWriter bfwHMM;
    private boolean mode;
    
    /**
     * Class constructor for HMMAnalyzer
     * @param T     The number of observations
     * @param N     The number of states in an HMM
     */
    public HMMAnalyzer(int T, int N, boolean mode){
        this.min_T = T;
        this.N = N;
        this.mode = mode;
        try {
            if(mode)
                bfwHMM = new BufferedWriter(new FileWriter("HMMList"));
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
    
    /**
     * Performs the Forward Procedure
     * @param lambda    The Hidden Markov Model for which the forward procedure is to be carried out
     * @param obs       The observation sequence
     * @return          The probability of the observation sequence given the model
     */
    public double forwardProcedure(Model lambda, int[] obs){
        if(obs.length < min_T){
            System.err.println("Observation sequence incomplete");
            return 0;
        }
        alpha = new double[obs.length][N];
        if(DEBUG) System.out.println("Carrying out forward procedure");
        if(lambda == null){
            System.err.println("Model is NULL");
            System.exit(-1);
        }
        double sum;
        // Initialization
        for(int i=0;i<N;i++){
            alpha[0][i] = lambda.pi(i)* lambda.b(i, obs[0]);
        }
        // Induction
        for(int t=0;t<obs.length-1;t++){
            for(int j=0;j<N;j++){
                sum = 0;
                for(int i=0;i<N;i++){
                    sum += alpha[t][i] * lambda.a(i, j);
                }
                alpha[t+1][j] = sum * lambda.b(j, obs[t+1]);
            }
        }
        // Termination
        sum = 0;
        for(int i=0;i<N;i++){
            sum += alpha[obs.length-1][i];
        }
        return sum;
    }
    
    /**
     * Performs the Backward Procedure
     * @param lambda       The Hidden Markov Model for which the backward procedure is to be carried out
     * @param obs          The observation sequence
     */
    private void backwardProcedure(Model lambda, int[] obs){
        if(obs.length < min_T){
            System.err.println("Observation sequence incomplete");
            return;
        }
        beta = new double[obs.length][N];
        if(DEBUG) System.out.println("Carrying out backward procedure");
        double sum;
        // Initialization
        for(int i=0;i<N;i++){
            beta[obs.length-1][i] = 1;
        }
        // Induction
        for(int t=obs.length-1;t>0;t--){
            for(int i=0;i<N;i++){
                sum = 0;
                for(int j=0;j<N;j++){
                     sum += (lambda.a(i, j) * lambda.b(j, obs[t]) * beta[t][j]);
                }
                beta[t-1][i] = sum;
            }
        }
    }
    
    /**
     * Performs the Viterbi search algorithm
     * @param lambda       The Hidden Markov Model for which the Viterbi algorithm is to be carried out
     * @param obs          The observation sequence
     * @return             The probability of the optimal state sequence given the model 
     *                     and the observation sequence
     */
    public double runViterbiAlgorithm(Model lambda, int[] obs){
        int[][] psi = new int[obs.length][N];
        double[][] delta = new double[obs.length][N];
        
        if(obs.length < min_T){
            System.err.println("Observation sequence incomplete");
            return 0;
        }
        if(DEBUG) System.out.println("Carrying out Viterbi's Algorithm");
        double pStar;
        // Initialization
        for(int i=0;i<N;i++){
            delta[0][i] = lambda.pi(i) * lambda.b(i, obs[0]);
            psi[0][i] = 0;
        }
        
        // Recursion
        double max;
        double temp;
        for(int t=1;t<obs.length;t++){
            for(int j=0;j<N;j++){
                psi[t][j] = 0;
                max = delta[t-1][0] * lambda.a(0, j);
                for(int i=1;i<N;i++){
                    temp = delta[t-1][i] * lambda.a(i, j);
                    if(temp > max){
                        max = temp;
                        psi[t][j] = i;
                    }
                }
                delta[t][j] = max * lambda.b(j, obs[t]);
            }
        }
        
        // Termination
        pStar = delta[obs.length-1][0];
        qStar = 0;
        for(int i=1;i<N;i++){
            if(delta[obs.length-1][i] > pStar){
                pStar = delta[obs.length-1][i];
                qStar = i;
            }
        }
        
        if(DEBUG){
            System.out.println("Prob: "+pStar);
            printOptimalStateSequence(obs.length,psi);
        }
        return pStar;
    }
    
    /**
     * Performs the expectation modification procedure
     * @param lambda                    The model for which the expectation modification process has to be carried
     * @param obs                       The observation sequence
     * @param probabilityOfObservation  The probability of observation sequence of the new model
     * @return                          The new re-estimated model
     */
    private Model expectationModification(Model lambda, int[] obs, double probabilityOfObservation){
        gamma = new double[obs.length][N];
        xi = new double[N][N][obs.length-1];
        if(DEBUG) System.out.println("running EM method");
        Model m = new Model(N,lambda.getNumberOfObservations());
        // Calculating xi
        for(int t=0;t<obs.length-1;t++){
            for(int i=0;i<N;i++){
                for(int j=0;j<N;j++){
                    xi[i][j][t] = alpha[t][i] * lambda.a(i, j) * lambda.b(j, obs[t+1]) * beta[t+1][j];
                    xi[i][j][t] = xi[i][j][t] / probabilityOfObservation;
                }
            }
        }
        // Calculate gamma
        for(int t=0;t<obs.length;t++){
            for(int i=0;i<N;i++){
                gamma[t][i] = (alpha[t][i] * beta[t][i])/probabilityOfObservation;
            }
        }
        // Re-estimate pi (may not be required)
        if(DEBUG) System.out.println("Re-estimating PI");
        for(int i=0;i<lambda.getNumberOfStates();i++){
            m.setPi(i, gamma[0][i]);
        }
        // Re-estimate A
        if(DEBUG) System.out.println("Re-estimating A");
        for(int i=0, j=0;i<lambda.getNumberOfStates();i++){
            for(j=0;j<lambda.getNumberOfStates();j++){
                m.setA(i, j, expectedTransitions(i,j, obs.length)/expectedTransitions(i,obs.length));
            }
            if(i==j) System.out.println(expectedTransitions(i,j, obs.length)/expectedTransitions(i,obs.length));
        }
        // Re-estimate B
        if(DEBUG) System.out.println("Re-estimating B");
        for(int j=0;j<lambda.getNumberOfStates();j++){
            for(int k=0;k<lambda.getNumberOfObservations();k++){
                m.setB(j, k, expectedTimes(j,k,obs)/expectedTimes(j, obs.length));
            }
        }
        adjustB(m);
        return m;
    }
    
    /**
     * Re-adjust B so that there are no zero valued probabilities
     * @param model     The model whose 'b' matrix has to be re-adjusted
     */
    private void adjustB(Model model){
        Vector floor = new Vector();
        Vector ceil = new Vector();
        double floorValue = Math.pow(10, -300);
        double x;
        double min = 1;
        for(int n=0;n<model.getNumberOfStates();n++){
            for(int m=0;m<model.getNumberOfObservations();m++){
                // Find the maximum element
                x = model.b(n, m);
                if( x < floorValue){
                    floor.addElement(m);
                }
                else{
                    if(min > x){
                        min = x;
                    }
                    ceil.addElement(m);
                }
            }
            if(!floor.isEmpty()){
                // Distribute the maximum value to all zero-valued elements (if any)
                x = (floorValue * floor.size()) / ceil.size();
                for(int m=0;m<model.getNumberOfObservations();m++){
                    if(floor.contains(m)){
                        model.setB(n, m, model.b(n, m)+floorValue);
                    }
                    else if(ceil.contains(m)){
                        model.setB(n, m, model.b(n, m)-x);
                    }
                }
            }
            double sum = 0;
            for(int i=0;i<model.getNumberOfObservations();i++){
                sum += model.b(n, i);
            }
            if(DEBUG) System.out.println("Row "+n+" => "+sum);
            floor.clear();
            ceil.clear();
        }
    }
    
    /**
     * Finds the expected number of transitions from state <code>i</code> to state <code>j</code>.
     * @param i     From
     * @param j     To  
     * @param T     The number of observations
     * @return      The expected number of transitions from state <code>i</code> to state <code>j</code>
     */
    private double expectedTransitions(int i, int j, int T){
        double temp = 0;
        for(int t=0;t<T-1;t++){
            temp += xi[i][j][t];
        }
        return temp;
    }
    
    /**
     * Finds the expected number of transitions from state <code>i</code>.
     * @param i     From
     * @param T     The number of observations
     * @return      The expected number of transitions from state <code>i</code>
     */
    private double expectedTransitions(int i, int T){
        double temp = 0;
        for(int t=0;t<T-1;t++){
            temp += gamma[t][i];
        }
        return temp;
    }
    
    /**
     * Finds the expected number of time the system be in state <code>i</code> and the observation symbol is <code>k</code>.
     * @param i     The state in which the system might be in
     * @param k     The observation symbol 
     * @param obs   The observation sequence
     * @return      The expected number of time the system be in state <code>i</code> and the observation symbol is <code>k</code>.
     */
    private double expectedTimes(int i, int k, int[] obs){
        double temp = 0;
        for(int t=0;t<obs.length;t++){
            if(obs[t] == k)
                temp += gamma[t][i];
        }
        return temp;
    }
    
    /**
     * Finds the expected number of times the system is in state <code>i</code>
     * @param i     The state in which the system might be in  
     * @param T     The number of observations
     * @return      The expected number of times the system is in state <code>i</code>
     */
    private double expectedTimes(int i, int T){
        double temp = 0;
        for(int t=0;t<T;t++){
            temp += gamma[t][i];
        }
        return temp;
    }
    
    /**
     * Prints the optimal state sequence
     * @param T     The number of observations
     * @param psi   The array for backtracking found during Viterbi algorithm
     */
    public void printOptimalStateSequence(int T, int[][] psi){
        stateSequence = new int[T];
        backTrack(stateSequence, psi, T);
        for(int i=0;i<stateSequence.length;i++){
            System.out.print(stateSequence[i]+" ");
        }
        System.out.println();
    }
    
    /**
     * Backtracks to find the optimal state sequence using the 'psi' array determined during 
     * the Viterbi algorithm.
     * @param stateSequence     The array which records the optimal state sequence 
     * @param psi               The array used for backtracking. Filled during Viterbi algorithm
     * @param T                 The number of observations
     */
    private void backTrack(int[] stateSequence, int[][] psi, int T){
        stateSequence[T-1] = qStar;
        for(int t=T-2;t>=0;t--){
            stateSequence[t] = psi[t+1][stateSequence[t+1]];
        }
    }
    
    /**
     * Starts the HMM building process: Forward Procedure, Backward Procedure, Viterbi algorithm,
     * Re-estimation using Expectation Modification.
     * @param lambda    The model for which the analysis is to be done
     * @param obs       The observation sequence
     * @return          The 'best' model giving the highest probability of optimal state sequence
     */
    private Model analyze(Model lambda, int[] obs){
        double pStar = 0, pStar1 = -1;
        double probabilityOfObservation = 0;
        Model m = lambda;
        Model m1;
        int iterations = 0;
        do{
            if(DEBUG){
                System.out.println("*************** Iteration "+iterations+" ******************");
            }
            else{
                System.out.println("Iteration: "+iterations+" Probability: "+pStar);
            }
            m1 = m;
            probabilityOfObservation = forwardProcedure(m1,obs);
            backwardProcedure(m1,obs);
            if(pStar1 == 0) pStar = runViterbiAlgorithm(m1,obs);
            else pStar = pStar1;
            m = expectationModification(m1,obs, probabilityOfObservation);
            pStar1 = runViterbiAlgorithm(m,obs);
            iterations++;
        }while( pStar1 > pStar && iterations < 500000);
        return m1;
    }
    
    /**
     * Carries out the averaging of all the models
     * @param models    The models  
     * @param M         The number of observation symbols per state
     * @return          The averaged model
     */
    private Model average(Vector models, int M){
        if(models.size() == 0) return null;
        if(DEBUG) System.err.println("Averaging "+models.size()+" models");
        Model model = new Model(N,M);
        double sum;
        double num = models.size();
        for(int i=0;i<N;i++){
            // Average of PI
            sum = 0;
            for(int m=0;m<models.size();m++){
                sum += ((Model) models.elementAt(m)).pi(i);
            }
            model.setPi(i, sum / num);
            // Average of A
            for(int j=0;j<N;j++){
                sum = 0;
                for(int m=0;m<models.size();m++){
                    sum += ((Model) models.elementAt(m)).a(i, j);
                }
                model.setA(i, j, sum / num);
            }
            // Average of B
            for(int j=0;j<M;j++){
                sum = 0;
                for(int m=0;m<models.size();m++){
                    sum += ((Model) models.elementAt(m)).b(i, j);
                }
                model.setB(i, j, sum / num);
            }
        }
        return model;
    }
    
    /**
     * Writes the model to a file in binary format
     * @param m         The model to be written to the file
     * @param filename  The file name to which the model has to be written
     */
    private void writeModelToFile(Model m, String filename){
        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(filename));
            outputStream.writeObject(m);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Carries out the HMM building process, averaging all the models estimated 
     * and then carrying the entire process twice on the averaged models. The
     * intermediate models are also stored in the file named 'HMMList'.
     * @param observations      The Vector of observation sequences
     * @param M                 The number of observation symbols per state
     * @param output            The name of the file to which the averaged model is to written
     */
    public void run(Vector observations, int M, String output){
        Model model, model1;
        String prefix = output.replaceFirst(".hmm", "");
        String temp;
        int t,i;

        Vector models = new Vector();
                
        int[] obs;
        // Build HMMs
        for(i=0;i<observations.size();i++){
            model = new Model(N,M);
            model.initializeModel();
            obs = (int[]) observations.elementAt(i);
            model = analyze(model,obs);
            models.addElement(model);
        }
        for(t=0;t<2;t++){
            // Average all models
            model = average(models,M);
            // Record intermediate models
            try{
                for(i=0;i<models.size();i++){
                    temp = prefix +"_"+ t +"_"+ i+".hmm";
                    writeModelToFile((Model) models.elementAt(i),temp);
                    if(mode) bfwHMM.write(temp+"\n");
                }
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
            models.clear();
            // Build HMMs
            for(i=0;i<observations.size();i++){
                model1 = model;
                obs = (int[]) observations.elementAt(i);
                model1 = analyze(model1,(int[]) observations.elementAt(i));
                models.addElement(model1);
            }
            // Carry this process twice
        }
        // The averaged model is the final model
        model = average(models,M);
        // Record intermediate models
        try{
            for(i=0;i<models.size();i++){
                temp = prefix +"_"+ t +"_"+ i+".hmm";
                writeModelToFile((Model) models.elementAt(i),temp);
                if(mode) bfwHMM.write(temp+"\n");
            }
            if(mode) bfwHMM.flush();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        System.out.println("Writing Model to "+output);
        writeModelToFile(model,output);
    }

    /**
     * Close the buffered stream
     */
    public void close(){
        try {
            if(mode) bfwHMM.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}