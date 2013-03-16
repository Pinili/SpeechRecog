package SpeechRecogEngine;
/*
 * This code belongs to 
 * Krishna Brahmam, Dept. of CSE, IIT Guwahati
 */

import java.io.Serializable;

/**
 * Class representing a Hidden Markov Model
 * @author Krishna Brahmam
 */
public class Model implements Serializable {
    private int N;          // No. of states
    private int M;          // No. of observations in each state
    private double[]  pi;   // Initial state probability distribution
    private double[][] A;   // State transition probability matrix
    private double[][] b;   // Observation probability matrix
    
    /**
     * Class constructor for Model
     * @param N     The number of states
     * @param M     The number of observations per state
     */
    public Model(int N, int M){
        this.M = M;
        this.N = N;
        pi = new double[N];
        A = new double[N][N];
        b = new double[N][M];
    }
    
    /**
     * Initializes the model to a Bakis (or left-to-right) model
     */
    public void initializeModel(){
        // Initialize 'pi' matrix
        for(int i=0;i<N;i++){
            if(i==0) pi[i] = 1;
            else pi[i] = 0;
        }
        // Initialize 'A' matrix
        for(int i=0;i<N;i++){
            for(int j=0;j<N;j++){
                if(i == j && i != N-1){
                    A[i][j] = 0.5;
                    j++;
                    A[i][j] = 0.5;
                }
                else if(i==j && i == N-1){
                    A[i][i] = 1;
                }
                else{
                    A[i][j] = 0;
                }
            }
        }
        // Initialize 'b' matrix
        for(int i=0;i<N;i++){
            for(int j=0;j<M;j++){
                b[i][j] = 1.0/M;
            }
        }
    }
    
    /**
     * Get number of states in the Hidden Markov Model
     * @return The number of states, N, in the Hidden Markov Model
     */
    public int getNumberOfStates(){
        return N;
    }
    
    /**
     * Get number of observations per state in the Hidden Markov Model
     * @return The number of observations per state, M, in the Hidden Markov Model
     */
    public int getNumberOfObservations(){
        return M;
    }
    
    /**
     * Get the value of the element at <code>index</code> in the initial state 
     * probability distribution
     * @param   index   The index of the element whose value is to be returned
     * @return  The value of the element at <code>index</code> in the initial state 
     *          probability distribution
     */
    public double pi(int index){
        if(index >= pi.length || index < 0){
            System.err.println("Array Index out of bounds");
            System.exit(-1);
        }
        return pi[index];
    }
    
    /**
     * Set the value of the element at <code>index</code> to <code>val</code>. 
     * @param index     The index of the element whose value is to be set
     * @param val       The value to which the element at <code>index</code> has to be set
     */
    public void setPi(int index, double val){
        if(index >= pi.length || index < 0){
            System.err.println("Array Index out of bounds");
            System.exit(-1);
        }
        pi[index] = val;
    }
    
    /**
     * Get the value of the element at <code>[state,index]</code> in the 'b' matrix
     * @param state     The state whose observation symbol is under consideration
     * @param index     The index of the observation symbol
     * @return  The probability of the system being in state <code>state</code> and 
     *          outputting an observation symbol <code>index</code>.
     */
    public double b(int state, int index){
        if(index >= M || index < 0 || state < 0 || state >= N){
            System.err.println("Illegal state or index number");
            System.exit(-1);
        }
        return b[state][index];
    }
    
    /**
     * Set the value of an element at <code>[state,index]</code> in 'b' matrix
     * to <code>val</code>.
     * @param state     The state whose observation symbol is under consideration
     * @param index     The index of the observation symbol
     * @param val       The value to which the element at <code>[state,index]</code> has to be set
     */
    public void setB(int state, int index, double val){
        if(index >= M || index < 0 || state < 0 || state >= N){
            System.err.println("Illegal state or index number");
            System.exit(-1);
        }
        b[state][index] = val;
    }
    
    /**
     * Get the transition probability from <code>state1</code> to <code>state2</code>
     * @param state1    From
     * @param state2    To
     * @return  The transition probability from <code>state1</code> to <code>state2</code>
     */
    public double a(int state1, int state2){
        if(state1 >= N || state1 < 0 || state2 < 0 || state2 >= N){
            System.err.println("Illegal state number");
            System.exit(-1);
        }
        return A[state1][state2];
    }
    
    /**
     * Set the transition probability from <code>state1</code> to <code>state2</code> to <code>val</code>.
     * @param state1    From
     * @param state2    To
     * @param val   The new transition probability from <code>state1</code> to <code>state2</code>
     */
    public void setA(int state1, int state2, double val){
        if(state1 >= N || state1 < 0 || state2 < 0 || state2 >= N){
            System.err.println("Illegal state number");
            System.exit(-1);
        }
        A[state1][state2] = val;
    }
}
