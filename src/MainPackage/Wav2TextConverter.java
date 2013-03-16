package MainPackage;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

	/**
	 * Extracts the sample values from speech signals stored in WAV format
	 */
	public class Wav2TextConverter {
	   
	    private BufferedInputStream bfread;
	    private BufferedWriter bfwrite;
	    private BufferedWriter bfmark;
	    private byte[] buffer;
	    private int temp;
	    private int interval = 160;
	    private Vector x;
	    private Vector startMark;
	    private Vector endMark;
	    private double normalizationFactor = 15000;

	   
	    /**
	     * Class constructor for Wav2TextConverter
	     * @param input     Input file name
	     * @param output    Output file name
	     * @throws java.io.IOException
	     */
	    public Wav2TextConverter(String input, String output) throws IOException{
	        try {
	            bfread = new BufferedInputStream(new FileInputStream(input));
	            bfwrite = new BufferedWriter(new FileWriter(output));
	            //System.out.println(input.replaceFirst(".wav", ".lab"));
	            bfmark = new BufferedWriter(new FileWriter("mark.dat"));
	           
	            buffer = new byte[2];
	            x = new Vector();
	            startMark = new Vector();
	            endMark = new Vector();
	        } catch (FileNotFoundException ex) {
	            System.err.println(ex.getMessage());
	        }
	    }
	   
	    /**
	     * Method to start the conversion process
	     */
	    public void convert(){
	        try {
	            printHeader();
	            printData();
	        } catch (IOException ex) {
	            System.err.println(ex.getMessage());
	        }
	    }

	    /**
	     * Method to record the data section of a WAV file. In a WAV format, the higher byte
	     * comes second. So, the higher byte is read second into <code>buffer[1]</code>. The lower byte
	     * which is read first is then masked with <code>0x000000FF</code> for the lower
	     * eight bits(removing the sign bit). The 16-bit data value is constructed by
	     * shifting the higher byte by 8 bits and then ORing with the lower byte.
	     */
	    private void printData(){
	        try {
	            while (bfread.read(buffer) >= 0) {
	                temp = 0;
	                temp = buffer[1];
	                temp <<= 8;
	                temp |= (0x000000FF & buffer[0]);
	                x.addElement(new Double(temp));
	            }
	            process();
	            write();
	        } catch (IOException ex) {
	            System.err.println(ex.getMessage());
	        }
	    }
	   
	    private void write(){
	        try {
	            int i,j;
	            for(i=0,j=0;i<x.size();i++){
	                bfwrite.write(((Double) x.elementAt(i)).doubleValue() + "\n");
	            }
	            bfwrite.flush();
	            bfwrite.close();
	           
	            for(i=0;i<startMark.size();i++){
	                bfmark.write(
	                        ((Integer) startMark.elementAt(i)).intValue()+"\t" +
	                        ((Integer) endMark.elementAt(i)).intValue() + "\n");
	            }
	            bfmark.flush();
	            bfmark.close();
	        } catch (IOException ex) {
	            System.err.println(ex.getMessage());
	        }
	    }
	   
	    private void process(){
	        double dc = 0;
	        double Emin = 0;
	       
	        // Perform DC Shift
	        for(int i=0;i<x.size();i++)
	            dc += ((Double) x.elementAt(i)).doubleValue();
	        dc = dc/x.size();
	       
	        if(dc != 0){
	            for(int i=0;i<x.size();i++)
	                x.set(i,((Double) x.elementAt(i)) - dc);
	        }
	       
	        // Normalize
	        normalize();
	       
	        /*/ Calculate Emin
	        for(int i=0;i<(interval/2);i++){
	            Emin += (  (((Double) x.elementAt(i)).doubleValue())
	                     * (((Double) x.elementAt(i)).doubleValue())
	                    );
	            //x.removeElementAt(i);
	        }
	       
	        //System.out.println("Min. E: "+Emin);
	       
	        // Tokenize
	        int start = 0;
	        int end = start + interval;
	        int begin = 0;
	        boolean active = false;
	        double E;
	        int maxRounds = (int) Math.ceil(x.size()/interval);
	        for(int rounds=0;rounds < maxRounds;rounds++){
	            E = energy(start,end);
	            if(0.1*E <= Emin){
	                if(active){
	                    startMark.addElement(begin);
	                    endMark.addElement(start-1);
	                    active = false;
	                }
	            }
	            else{
	                if(!active){
	                    begin = start;
	                    active = true;
	                }
	            }
	            start = end;
	            end = Math.min(start+interval,x.size());
	        }
	        if(active){
	            startMark.addElement(begin);
	            endMark.addElement(start-1);
	            active = false;
	        }//*/
	    }
	   
	    private double energy(int start, int end){
	        double sum = 0;
	        for(int i=start;i<end;i++){
	            sum += ( (((Double) x.elementAt(i)).doubleValue())
	                    *(((Double) x.elementAt(i)).doubleValue())
	                   );
	        }
	        return sum;
	    }
	   
	    /**
	     * Method to normalize the input sample values before processing for LPC
	     */
	    private void normalize(){
	        double max = ((Double) x.firstElement()).doubleValue();
	        double tmp;
	        for(int i=1;i<x.size();i++){
	            tmp = ((Double)x.elementAt(i)).doubleValue();
	            if(max < tmp)
	                max = tmp;
	        }
	        for(int i=0;i<x.size();i++){
	            tmp = ((Double) x.elementAt(i)).doubleValue();
	            tmp = (tmp/max) * normalizationFactor;
	            x.setElementAt(tmp, i);
	        }
	    }
	   
	    /**
	     * Skip the header of the WAV file. The method skips the first 44 bytes of
	     * the WAV file which is the header.
	     * @throws java.io.IOException
	     */
	    private void printHeader() throws IOException{
	        bfread.skip(44);
	    }
	   
	    public static void main(String args[]) throws IOException{
	        Wav2TextConverter wv = new Wav2TextConverter("C:\\Documents and Settings\\Administrator\\Desktop\\testsamples\\i1.wav","sample.dat");
	        wv.convert();
	    }
	}
