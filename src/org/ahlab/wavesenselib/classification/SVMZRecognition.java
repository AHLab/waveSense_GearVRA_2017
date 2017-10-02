package org.ahlab.wavesenselib.classification;

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

public class SVMZRecognition {
	private final static String TAG = SVMZRecognition.class.getSimpleName();

	public static int numberOfDataitems = 0;
	public static int numberOfStates = 20;
	static Instances testingInstances;
	static String state;
	static Classifier classifier;
	static String csvPath;
	public static String modelPath = "";
	private static String device_Name;

	public SVMZRecognition() {
		// getting the JsonObject and assigning it to the jo
		JSONObject jo = RecognitionManager.varJson;
		device_Name = RecognitionManager.device_Name;
		try {
			csvPath = jo.getString("csvPathZ");
			csvPath = "config" + "/devices/" + device_Name + "/csv/" + csvPath;
			modelPath = "config" + "/devices/" + device_Name + "/models/"
					+ "Z.model";
		} catch (JSONException cannotFindJSONVariable) {
			cannotFindJSONVariable.printStackTrace();
		}
	}

	public static Instances createTestingInstanceProgrammatically(int[] data) {
		// creating an instance and adding it to the testing instances
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
			// load the trained classifer from the file - desrialization
			Classifier classifier = RecognitionManager.getInstance(null)
					.getClassifierSVMZ();

			// classifier classfying the data for the instance in the testing
			// instances
			for (int i = 0; i < testingInstances.numInstances(); i++) {
				double score = classifier.classifyInstance(testingInstances
						.instance(i));
				testingInstances.instance(i).setClassValue(score);
				state = testingInstances.classAttribute().value((int) score);
			}
		} catch (Exception ex) {
			Logger.getLogger(SVMZRecognition.class.getName()).log(Level.SEVERE,
					null, ex);
		}
	}

	static void initTestintInstances() {
		// creating a testing data set programmatically
		FastVector fv = new FastVector(numberOfDataitems + 1);
		// The below lines set the attributes for the testing instances.
		// The attributes should be the same
		// as in the training dataset used for training the classifier
		for (int i = 0; i < numberOfDataitems; i++) {
			fv.addElement(new Attribute("zsense/ahlab/org/zsenselibrary/data"
					+ i));
		}
		FastVector nomVals = new FastVector();
		for (int i = 0; i < numberOfStates; i++)
			nomVals.addElement("Z" + i);
		fv.addElement(new Attribute("state", nomVals));
		testingInstances = new Instances("testInstance", fv, 0);
		testingInstances.setClassIndex(testingInstances.numAttributes() - 1);

	}

	public int getPrediction(int[] data) {
		testingTheClassifier(createTestingInstanceProgrammatically(data));
		String num = state.substring(1, state.length());
		// Log.i(TAG, "State: " + state);
		return Integer.parseInt(num);
	}

	public void init(Classifier classifierSVMZ) throws Exception {
		try {
			classifier = classifierSVMZ;
		} catch (Exception ex) {
			System.err
					.println("Classifier not found. Training a new classifier for Z recognition");
		}
		initTestintInstances();
	}

}