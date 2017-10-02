package org.ahlab.wavesenselib.data;

/**
 * Created by Shanaka on 7/31/2015.
 */
public class BLeData {
    private final static String TAG = BLeData.class.getSimpleName();

    private static BLeData instance = null;

    int numberOfInputs;

    int[] completeintegerBuffer;
    int[] incompleteintegerBuffer;
    int bufferFilled;
    int i;

    private BLeData() {
        //set this according to the recognition manager
        //numberOfInputs = 22;

        //Log.w(TAG, "BLeData constructor");
        //incompleteintegerBuffer = new int[numberOfInputs];
        //completeintegerBuffer = new int[numberOfInputs];

        bufferFilled = 0;
        i = -1;
    }

    public static BLeData getInstance() {
        //Log.w(TAG, "BLeData getInstance");
        if(instance == null) {
            //Log.w(TAG, "BLeData getInstance - inside");
            instance = new BLeData();
        }
        return instance;
    }

    public int[] getBLeDataBuffer(){
        int[] integerBufferCopy = new int[numberOfInputs];
        System.arraycopy(completeintegerBuffer, 0, integerBufferCopy, 0, completeintegerBuffer.length);
        completeintegerBuffer = new int[numberOfInputs];
        return integerBufferCopy;
    }

    public void resetBLeDataBuffer(){
        bufferFilled = 0;
        incrementI();
    }
    
    private void incrementI(){
    	i++;
    }

    public void addData(int data){
        incompleteintegerBuffer[bufferFilled++] = data;
        if(bufferFilled == numberOfInputs)
        	System.arraycopy(incompleteintegerBuffer, 0, completeintegerBuffer, 0, incompleteintegerBuffer.length);
    }

    public int getIterationNumber(){        
        return i;
    }

    public void resetIterationNumber(){
        i = 0;
    }

    public int getNumberOfInputs(){
        return numberOfInputs;
    }

    public void setNumberOfInputs(int numberOfInputs){
        this.numberOfInputs = numberOfInputs;
        this.incompleteintegerBuffer = new int[numberOfInputs];
        this.completeintegerBuffer = new int[numberOfInputs];
    }

    public int getBufferFilled(){
        return bufferFilled;
    }
}
