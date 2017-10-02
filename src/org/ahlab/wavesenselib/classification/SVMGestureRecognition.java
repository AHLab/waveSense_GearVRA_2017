package org.ahlab.wavesenselib.classification;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

/**
 * Created with IntelliJ IDEA.
 * User: Nipuna_Sam
 * Date: 3/4/14
 * Time: 5:25 PM
 * To change this template use File | Settings | File Templates.
 */

public class SVMGestureRecognition {

    public static int numberOfDataitems=0;
    public static int numberOfStates=18;
    static Instances testingInstances;
    static String state;
    static Classifier classifier;
    static String csvPath;
    public static String modelPath ="config/devices/ring/models/Gesture.model";

    public SVMGestureRecognition(){
        //getting the JsonObject and assigning it to the jo
        JSONObject jo = RecognitionManager.varJson;
        String device_Name = RecognitionManager.device_Name;
        try{
            csvPath = jo.getString("csvPathG");
            csvPath = "config"+"/devices/" + device_Name + "/csv/" + csvPath;
            modelPath = "config"+"/devices/" + device_Name+ "/models/" + "Gesture.model";
        }
        catch(JSONException cannotFindJSONVariable){
            cannotFindJSONVariable.printStackTrace();
        }
    }

    public static Instances createTrainingDataset()
    {
        Instances trainingInstances = null;
        try {
            //creating the training dataset;
            CSVLoader trainingLoader =new CSVLoader();
            trainingLoader.setSource(new File(csvPath));
            trainingInstances=trainingLoader.getDataSet();
            trainingInstances.setClassIndex(trainingInstances.numAttributes()-1);

        }catch(IOException ex) {
            System.err.println(ex.toString());
            System.err.println("File not found (gesture.csv)");
        }
        return trainingInstances;
    }
    public static void trainClassifier(Instances trainingInstances) throws Exception {

        try{
            BayesNet bn = new BayesNet();
            //String[] options={"-B","20"};
            String[] options={"-D","-Q",
                    "weka.classifiers.bayes.net.search.local.K2", "--", "-P", "1", "-S", "BAYES", "-E",
                    "weka.classifiers.bayes.net.estimate.SimpleEstimator", "--", "-A", "0.5"};
            bn.setOptions(options);
            for (String s : bn.getOptions()) {
                System.err.println(s);
            }

            classifier=bn;

            //build the classifier for the training instances
            classifier.buildClassifier(trainingInstances);
            //Evaluating the classifer
            Evaluation eval = new Evaluation(trainingInstances);
            eval.crossValidateModel(classifier,trainingInstances,10,new Random(123));
            //printing the summary of training
            String strSummary = eval.toSummaryString();
            System.err.println("-----------TRAINING SUMMARY---------");
            System.err.println(strSummary);
            System.err.println("------------------------------------");
            double[][] cmMatrix = eval.confusionMatrix();
            for (int i = 0; i < cmMatrix.length; i++) {
                for (double v : cmMatrix[i]) {
                    System.err.print(v + " ");
                }
                System.err.println();
            }
            //Serializing the classifier object in to a file
            weka.core.SerializationHelper.write(modelPath,classifier);
        }catch(Exception ex) {
            Logger.getLogger(SVMRecognition.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Instances createTestingInstanceProgrammatically(int[] data)
    {
        //creating an instance and adding it to the testing instances
        testingInstances.delete();
        Instance instance =  new Instance(numberOfDataitems+1);
        for (int i = 0; i < numberOfDataitems; i++) {
            instance.setValue(testingInstances.attribute(i), data[i]);
        }
        instance.setDataset(testingInstances);
        testingInstances.add(instance);
        return testingInstances;
    }

    public static void testingTheClassifier(Instances testingInstances)
    {
        try {
            Classifier classifier = RecognitionManager.getInstance(null).getClassifierSVMG(); //load the trained classifer from the file - desrialization
            for(int i=0;i<testingInstances.numInstances();i++)             //classifier classfying the data for the instance in the testing  instances
            {
                double score =  classifier.classifyInstance(testingInstances.instance(i)); //classifier.distributionForInstance() for probabilities
                testingInstances.instance(i).setClassValue(score);
                state= testingInstances.classAttribute().value((int) score);
            }
        } catch (Exception ex) {
            Logger.getLogger(SVMRecognition.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void initTestintInstances(){ //creating a testing data set programmatically
        FastVector fv = new FastVector(numberOfDataitems+1);
        //The below lines set the attributes for the testing instances.
        //The attributes should be the same
        //as in the training dataset used for training the classifier
        for (int i = 0; i < numberOfDataitems; i++) {
            fv.addElement(new Attribute("zsense/ahlab/org/zsenselibrary/data" +i));
        }
        FastVector nomVals = new FastVector();
        for (int i=0; i<numberOfStates; i++)
            nomVals.addElement("g"+i);
        fv.addElement(new Attribute("state",nomVals));
        testingInstances = new Instances("testInstance",fv  ,0);
        testingInstances.setClassIndex(testingInstances.numAttributes()-1);
    }

    public int getPrediction(ArrayList<Integer> data){
        int[] datac=new int[numberOfDataitems];
        for (int i = 0; i < numberOfDataitems; i++) {
         datac[i]=data.get(i);
        }
        testingTheClassifier(createTestingInstanceProgrammatically(datac));
        String num=state.substring(1,state.length());
        return Integer.parseInt(num);
    }
    public  void init (Classifier classifierSVMG) throws Exception
    {
        /*//getting the JsonObject and assigning it to the jo
        JSONObject jo = RecognitionManager.varJson;
        String device_Name = RecognitionManager.device_Name;
        csvPath = (String) jo.get("csvPathG");
        csvPath   = "config"+"/devices/" + device_Name + "/csv/" + csvPath;
        modelPath = "config"+"/devices/" + device_Name+ "/models/" + "Gesture.model";*/
        //training the classifier.
        try {
            classifier= classifierSVMG;
        }
        catch (Exception ex){
            System.err.println("Classifier(gesture) not found. Training a new classifier(gesture) for gesture recognition");
        }
        if(classifier==null)
            trainClassifier(createTrainingDataset());
            initTestintInstances();
    }


}