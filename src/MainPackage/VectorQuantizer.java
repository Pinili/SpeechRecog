package MainPackage;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JOptionPane;

/**
 * Generates a VQ codebook from a set of input feature vectors using LBG algorithm.
 */
public class VectorQuantizer {
   
    private final boolean DEBUG = false;
   
    private Vector codebook;        // The codebook
    private Vector input;           // Input set of feature vectors
    private Hashtable hyperspace;   // The regions represented by the centroids in the codebook
    private int[] density;          // The number of vectors in each region
   
    private int p;                  // Order of each input vector
    private double e = 0.05;        // epsilon: used in LBG while splitting
    private int codeBookSize;       // The size of codebook
   
    private double oldDistortion = 0;
    private double currentDistortion = 0;
    private int iteration = 0;
   
    private BufferedWriter out;
    private BufferedWriter outd;
   
    /**
     * Class constructor for VectorQuantizer
     * @param p         The order in LPC. Indicates the size of each input vector
     * @param M         The size of the codebook
     * @param input     The input set of feature vectors
     */
    public VectorQuantizer(int p, int M, Vector input){
        this.p = p;
        this.codeBookSize = M;
        this.input = input;
        this.codebook = new Vector(M);
        this.hyperspace = new Hashtable(M);
        this.density = new int[M];
       
        if(DEBUG){
            try {
                out = new BufferedWriter(new FileWriter("CodeBook.txt"));
                outd = new BufferedWriter(new FileWriter("Distortion.txt"));
           
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
   
    /**
     * Computes centroid of a region, <code>target</code>
     * @param target    The region whose centroid is to be computed
     * @return          The centroid vector of the <code>target</code>
     */
    private double[] computeCentroid(Vector target){
        double[] temp = new double[p];
        for(int i=0;i<p;i++){
            double sum = 0;
            for(int j=0;j<target.size();j++){
                sum += ((double[]) target.elementAt(j))[i];
            }
            temp[i] = sum/target.size();
        }
        return temp;
    }
   
    /**
     * Splits each codebook vector into two so that the final size of the new
     * codebook is twice the size of the old codebook.
     */
    private void splitCodeBook(){
        int n = codebook.size();
        double[] temp;
        for(int m = 0;m < n;m++){
            temp = new double[p];
            java.lang.System.arraycopy(codebook.elementAt(m), 0, temp, 0, temp.length);
            codebook.add(scaleVector(1+e,temp));
            scaleVector(1-e,(double[]) codebook.elementAt(m));
        }
    }
   
    /**
     * Auxiliary function to calculate (1 +/- epsilon)*y during splitting the codebook
     * @param scalar    The scalar with which the <code>vector</code> has to be multiplied
     * @param vector    The vector to be multiplied by the scalar
     * @return          The product matrix  
     */
    private double[] scaleVector(double scalar, double[] vector){
        int i;
        for(i=0;i<vector.length;i++){
            vector[i] = scalar*vector[i];
        }
        return vector;
    }
   
    /**
     * Classifies the input set of vectors into regions in <code>hyperspace</code>
     * by computing distances between input vectors and each of the codebook vectors.
     */
    private void classifyVectors(){
        double min;
        double temp;
        int index = 0;
        Vector v;
        for(int n=0;n<input.size();n++){
            min = 0;
            // Find the minimum distance and the index of the codebook vector at which this happens
            for(int m=0;m<codebook.size();m++){
                temp = computeDistance((double []) (input.elementAt(n)),(double []) (codebook.elementAt(m)));
                if(m == 0){
                    min = temp;
                    index = m;
                }
                else if(min > temp){
                    min = temp;
                    index = m;
                }
            }
            // Classify the input vector to 'index' cell of hyperspace
            if(density[index] == 0){
                v = new Vector();
                v.add(input.elementAt(n));
                hyperspace.put(index, v);
                density[index]++;
            }
            else{
                ((Vector) hyperspace.get(index)).add(input.elementAt(n));
                density[index]++;                        
            }
        }
    }
   
    /**
     * Computes the Euclidean distance between two vectors <code>x</code> and <code>y</code>.
     * @param x    
     * @param y
     * @return  The distance between the vectors x and y
     */
    private double computeDistance(double[] x, double[] y){
        double sum = 0;
        for(int i=0;i<x.length;i++){
            sum += Math.pow(x[i] - y[i],2);
        }
        return Math.sqrt(sum);
    }
   
    /**
     * Checks for empty cells in the hyperspace. Eliminates them by splitting the
     * region with highest density.
     */
    private void checkEmptyCells(){
        int count = 0;
        int max_density = density[0];
        int max_index = 0;
       
        // Find the cell with highest density
        for(int i=0;i<codebook.size();i++){
            if(density[i] > max_density){
                max_density = density[i];
                max_index = i;
            }
        }

        // Find if there are empty cells
        for(int i=0;i<codebook.size();i++){
            Vector v;
            if(density[i] == 0){
                // If there are empty cells, then transfer one-half of the region with highest
                // density into the empty cell.
                for(count = max_density/2;count>0;count--){
                    double[] t = (double[]) ((Vector) hyperspace.get(max_index)).remove(0);
                    density[max_index]--;
                    if(density[i] == 0){
                        v = new Vector();
                        v.add(t);
                        hyperspace.put(i,v);
                        density[i]++;
                    }
                    else{
                        ((Vector) (hyperspace.get(i))).add(t);
                        density[i]++;                        
                    }
                }
                max_density = density[0];
                max_index = 0;
                // Find the new region with maximum density
                for(int j=1;j<codebook.size();j++){
                    if(density[j] > max_density){
                        max_density = density[j];
                        max_index = j;
                    }
                }
            }
        }
    }
   
    /**
     * Update the codebook with the new centroid for each region in the hyperspace.
     */
    private void updateCodeBook(){
        Vector v;
        for(int i=0;i<hyperspace.size();i++){
            v = (Vector) hyperspace.get(i);
            if(v!=null) codebook.set(i,computeCentroid(v));
            density[i] = 0;
        }
        hyperspace.clear();
    }
   
    /**
     * The generalized Lloyd's algorithm or the k-means algorithm.
     * @throws java.io.IOException
     */
    private void generalizedLloydsAlgorithm() throws IOException{
        if(DEBUG)
            outd.write("\nIteration: "+iteration+"\n");
        do{
            oldDistortion = currentDistortion;
            classifyVectors();
            checkEmptyCells();
            updateCodeBook();
            currentDistortion = computeDistortion();
            if(DEBUG)
                writeDistortion(currentDistortion);
        }while(Math.abs(currentDistortion - oldDistortion) > 0.01);
    }
   
    /**
     * Writes the given distortion to <code>Distortion.txt</code>. This is used for
     * debugging.
     * @param distortion    The distortion to be written
     */
    private void writeDistortion(double distortion){
        try {
            outd.write(String.format("%f\n", distortion));
            outd.flush();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
   
    /**
     * Writes the distortion when a splitting ocurs. This is used for debugging.
     */
    private void writeDistortion(){
        try {
            outd.write("\nSplit:\n");
            writeDistortion(computeDistortion());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
   
    /**
     * Computes the overall average distortion for the input set of vectors and
     * the codebook.
     * @return  The overall average distortion for the input set of vectors and
     *          the codebook
     */
    private double computeDistortion(){
        double[] x;
        double sum = 0;
        for(int i=0;i<input.size();i++){
            x = (double[]) input.elementAt(i);
            sum += computeDistance(x,(double[]) codebook.elementAt( q(x) ));
        }
        return sum/input.size();
    }
   
    /**
     * Vector Quantizer for an input vector <code>x</code>.
     * @param x     The input vector which has to be quantized
     * @return      The index of the codebook vector which is the centroid of the region
     *              containing the vector <code>x</code>.
     */
    private int q(double[] x){
        int index = 0;
        double min_distance = computeDistance((double []) codebook.elementAt(index),x);
        double temp;
        for(int i=1;i<codebook.size();i++){
            temp = computeDistance((double[])codebook.elementAt(i),x);
            if(min_distance > temp){
                min_distance = temp;
                index = i;
            }
        }
        return index;
    }

    /**
     * Write codebook into <code>CodeBook.txt</code> for a given iteration of
     * the LBG algorithm. This is used for debugging.
     * @param iteration The iteration for which the codebook vectors are to be written
     */
    private void writeCodeBook(int iteration){
        double[] temp;
        try {
            out.write("\nIteration: "+iteration+"\n");
            for (int i = 0; i < codebook.size(); i++) {
                temp = (double[]) codebook.elementAt(i);
                for (int j = 0; j < temp.length; j++) {
                    out.write(String.format("%f",temp[j]) + " ");
                }
                out.write("\n");
            }
            out.flush();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
   
    /**
     * Writes the final codebook to file <code>filename</code>.
     * If mode is <code>true</code>, then the codebook is written in text format. If
     * it is <code>false</code>, then the codebook is written in binary format.
     * @param filename  The name of the codebook file
     * @param mode      The mode of the codebook. If <code>true</code>, the codebook is written
     *                  in text format. If <code>false</code>, the codebook is written in binary format.
     */
    private void writeCodeBook(String filename, boolean mode){
        double[] temp;
        if(mode){
            // Write codebook in text format
            BufferedWriter cb;
            System.out.println("Writing final codebook");
            try {
                cb = new BufferedWriter(new FileWriter(filename));
                for (int i = 0; i < codebook.size(); i++) {
                    temp = (double[]) codebook.elementAt(i);
                    for (int j = 0; j < temp.length; j++) {
                        cb.write(String.format("%f",temp[j]) + " ");
                    }
                    cb.write("\n");
                }
                cb.flush();
                cb.close();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
        else{
            // Write codebook in binary format
            ObjectOutputStream outputStream = null;
            try {
                outputStream = new ObjectOutputStream(new FileOutputStream(filename));
                outputStream.writeObject(codebook);
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
    }
   
    /**
     * Generates VQ codebook by carrying out LBG algorithm. Records the output, i.e. the codebook
     * itself at the end of the method.
     */
    public void generateCodeBook(String cb){
        System.out.println("\nGenerating CodeBook for "+input.size()+" vectors");
        iteration = 0;
        codebook.addElement(computeCentroid(input));
        if(DEBUG)
            writeCodeBook(iteration);
        try {
            while (codebook.size() < codeBookSize) {
                iteration++;
                currentDistortion = 0;
                splitCodeBook();
                if(DEBUG)
                    writeDistortion();
                generalizedLloydsAlgorithm();
                if(DEBUG)
                    writeCodeBook(iteration);
            }
            if(DEBUG){
                out.close();
                outd.close();
            }
            writeCodeBook(cb+".txt",true);
            writeCodeBook(cb,false);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
        System.out.println("CodeBook Generated");
    }
}
