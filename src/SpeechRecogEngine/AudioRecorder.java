package SpeechRecogEngine;
/*
 * This code belongs to
 * Krishna Brahmam, Dept. of CSE, IIT Guwahati
 */

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * AudioRecorder class records sound at 16kHz sample rate, mono channel, 16-bit
 * sized sample data in .wav format
 * @see AudioFormat
 * @see AudioSystem
 * @see TargetDataLine
 * @author Krishna Brahmam
 */
public class AudioRecorder extends Thread{
    protected static final float sampleRate = 16000F;
    protected static final int bitSize = 16;
    protected static final int channels = 1;
    protected static final boolean signed = true;
    protected static final boolean bigEndian = false;
    protected static final AudioFileFormat.Type fileType = 
            AudioFileFormat.Type.WAVE;
    
    private TargetDataLine targetLine;
    private AudioFormat format;
    private String filename;
    
    /**
     * Class constructor of AudioRecorder. 
     * @param targetLine    The target data-line
     * @param format        The format of the audio signal
     * @param filename      The name of the file to which the audio is to be saved
     */
    public AudioRecorder(TargetDataLine targetLine, AudioFormat format, String filename){
        this.targetLine = targetLine;
        this.filename = filename;
        this.format = format;
    }
    
    @Override
    public void run(){
        try {
            targetLine.open(format);
            targetLine.start();
            AudioSystem.write(new AudioInputStream(targetLine), fileType, 
                                                        new File(filename));
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (LineUnavailableException ex) {
            System.err.println(ex.getMessage());
        }
    }
}