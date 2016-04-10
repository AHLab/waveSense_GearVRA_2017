package org.ahlab.wavesense.classification;

/**
 * Created with IntelliJ IDEA.
 * User: Nipuna_Sam and Akshika47
 * Date: 28/3/14
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class SVMRecognition {
    private final static String TAG = SVMRecognition.class.getSimpleName();

    public static int numberOfDataitems = 0;
    public static int numberOfStates = 20;
    static Instances testingInstances;
    static String state;
    static Classifier classifier;
    static String csvPath;
    public static String modelPath="config/devices/ring/models/Horizontal.model";
    private static String device_Name;

    public SVMRecognition(){
        //getting the JsonObject and assigning it to the jo
        JSONObject jo = RecognitionManager.varJson;
        device_Name = RecognitionManager.device_Name;
        try{
            csvPath = jo.getString("csvPathH");
            csvPath = "config"+"/devices/" + device_Name + "/csv/" + csvPath;
            modelPath = "config"+"/devices/" + device_Name+ "/models/" + "Horizontal.model";
        }
        catch(JSONException cannotFindJSONVariable){
            cannotFindJSONVariable.printStackTrace();
        }
    }

    public static Instances createTestingInstanceProgrammatically(int[] data) {
        //creating an instance and adding it to the testing instances
        testingInstances.delete();
        Instance instance = new Instance(numberOfDataitems + 1);

        for (int i = 0; i < numberOfDataitems; i++) {
            instance.setValue(testingInstances.attribute(i), data[i]);
        }
        instance.setDataset(testingInstances);
        testingInstances.add(instance);
        return testingInstances;
    }


    public static void testingTheClassifier(Instances testingInstances) {
        try {
            //load the trained classifer from the file - desrialization
            Classifier classifier = RecognitionManager.getInstance(null).getClassifierSVMH();

            //classifier classfying the data for the instance in the testing  instances
            for (int i = 0; i < testingInstances.numInstances(); i++) {
                double score = classifier.classifyInstance(testingInstances.instance(i));
                testingInstances.instance(i).setClassValue(score);
                state = testingInstances.classAttribute().value((int) score);
            }
        } catch (Exception ex) {
            Logger.getLogger(SVMRecognition.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void initTestintInstances() {
        //creating a testing data set programmatically
        FastVector fv = new FastVector(numberOfDataitems + 1);
        //The below lines set the attributes for the testing instances.
        //The attributes should be the same
        //as in the training dataset used for training the classifier
        for (int i = 0; i < numberOfDataitems; i++) {
            fv.addElement(new Attribute("zsense/ahlab/org/zsenselibrary/data" + i));
        }
        FastVector nomVals = new FastVector();
        for (int i = 0; i < numberOfStates; i++)
            nomVals.addElement("H" + i);
        fv.addElement(new Attribute("state", nomVals));
        testingInstances = new Instances("testInstance", fv, 0);
        testingInstances.setClassIndex(testingInstances.numAttributes() - 1);


    }

    public int getPrediction(int[] data) {
        testingTheClassifier(createTestingInstanceProgrammatically(data));
        String num = state.substring(1, state.length());
        //Log.i(TAG, "State: " + state);
        return Integer.parseInt(num);
    }

    public void init(Classifier classifierSVMH) throws Exception {
       try {
            classifier = classifierSVMH;
        } catch (Exception ex) {
            System.err.println("Classifier not found. Training a new classifier for gesture(Horizontal) recognition");
        }
        initTestintInstances();
    }

}