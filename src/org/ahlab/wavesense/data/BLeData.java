package org.ahlab.wavesense.data;

/**
 * Created by Shanaka on 7/31/2015.
 */
public class BLeData {
    private final static String TAG = BLeData.class.getSimpleName();

    private static BLeData instance = null;

    int numberOfInputs;

    int[] integerBuffer;
    int bufferFilled;
    int i;

    private BLeData() {
        //set this according to the recognition manager
        //numberOfInputs = 22;

        //Log.w(TAG, "BLeData constructor");
        integerBuffer = new int[numberOfInputs];

        bufferFilled = 0;
        i = 0;
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
        System.arraycopy(integerBuffer, 0, integerBufferCopy, 0, integerBuffer.length);
        return integerBufferCopy;
    }

    public void resetBLeDataBuffer(){
        bufferFilled = 0;
    }

    public void addData(int data){
        integerBuffer[bufferFilled++] = data;
    }

    public int getIterationNumber(){
        i++;
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
        this.integerBuffer = new int[numberOfInputs];
    }

    public int getBufferFilled(){
        return bufferFilled;
    }
}
