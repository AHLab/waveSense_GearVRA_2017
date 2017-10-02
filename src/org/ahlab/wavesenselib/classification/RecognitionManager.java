package org.ahlab.wavesenselib.classification;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.ahlab.wavesenselib.data.Location;
import org.ahlab.wavesenselib.ui.MainActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import weka.classifiers.Classifier;
import weka.core.SerializationHelper;

/**
 * Created by Shanaka on 7/20/2015.
 */
public class RecognitionManager {
	private final static String TAG = RecognitionManager.class.getSimpleName();

	private static RecognitionManager instance = null;

	private static Classifier classifierSVMG, classifierSVMH, classifierSVML, classifierSVMZ;
	private static SVMRecognition svm;
	private static SVMLevelRecognition svml;
	private static SVMZRecognition svmz;
	private static SVMGestureRecognition svmg;
	private static int numberOfGuestureInputs, numberOfGestures,
			currentGesture, stopSize, gestureSize, g, constellationNumber;
	public static JSONObject varJson;
	public static String device_Name;
	private static ArrayList<Integer> gestureRaw, gestureParse, gestureToWrite;
	static boolean acquireData, updateGesture;
	public static JSONObject mainConfig, devicesObject, deviceObject;
	public static String[] deviceNames;
	public static String deviceConfigPath;
	public static String filenameFinal, filenameIntermid, csvPathH, modelPathH,
			csvPathG, modelPathG, csvPathL, modelPathL, csvPathZ, modelPathZ,
			gesture1, gesture2, gesture3;
	public static JSONObject configJson;

	private Context appContext;

	private static boolean recognize = true;
	private static int NUMBER_OF_INPUTS;
	private static int NUMBER_OF_STATES_F1, NUMBER_OF_STATES_F2,
			NUMBER_OF_STATES_F3;

	private RecognitionManager(Context appContext) {
		this.appContext = appContext;

		try {
			initVariables();
		} catch (IOException | JSONException | ParseException e) {
			e.printStackTrace();
		}

		try {
			setAbsolutePath();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			setConstellation();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			initGesture();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			svmInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static RecognitionManager getInstance(Context appContext) {
		// Log.w(TAG, "RecognitionManager getInstance");
		if (instance == null) {
			// Log.w(TAG, "RecognitionManager getInstance inside");
			instance = new RecognitionManager(appContext);
		}
		return instance;
	}

	public int getNumberOfInputs() {
		return NUMBER_OF_INPUTS;
	}

	public Classifier getClassifierSVMG() {
		return classifierSVMG;
	}

	public Classifier getClassifierSVMH() {
		return classifierSVMH;
	}

	public Classifier getClassifierSVML() {
		return classifierSVML;
	}
	
	public Classifier getClassifierSVMZ() {
		return classifierSVMZ;
	}

	void svmInit() throws Exception {
		SVMRecognition.numberOfDataitems = 2 * NUMBER_OF_INPUTS - 1;
		SVMLevelRecognition.numberOfDataitems = 2 * NUMBER_OF_INPUTS - 1;
		SVMZRecognition.numberOfDataitems = 2 * NUMBER_OF_INPUTS - 1;
				
		AssetManager assetManager = appContext.getAssets();

		svml = new SVMLevelRecognition();
		classifierSVML = (Classifier) SerializationHelper.read(assetManager
				.open(SVMLevelRecognition.modelPath));
		svml.init(classifierSVML);
		
		svm = new SVMRecognition();
		classifierSVMH = (Classifier) SerializationHelper.read(assetManager
				.open(SVMRecognition.modelPath));
		svm.init(classifierSVMH);
		
		svmz = new SVMZRecognition();
		classifierSVMZ = (Classifier) SerializationHelper.read(assetManager
				.open(SVMZRecognition.modelPath));
		svmz.init(classifierSVMZ);

		if (recognize) {
			SVMGestureRecognition.numberOfStates = numberOfGestures;
			SVMGestureRecognition.numberOfDataitems = numberOfGuestureInputs;
			svmg = new SVMGestureRecognition();
			classifierSVMG = (Classifier) SerializationHelper.read(assetManager
					.open(SVMGestureRecognition.modelPath));
			// should be called after training generation
			svmg.init(classifierSVMG);
		}
	}

	static void initGesture() throws IOException {
		// Gesture Training output
		if (acquireData) {
			if (currentGesture == 0) {
				for (int i = 0; i < numberOfGuestureInputs; i++) {
				}
			}
			// Gesture Training output end
		}
		gestureRaw = new ArrayList<Integer>();
		gestureParse = new ArrayList<Integer>();
		gestureToWrite = new ArrayList<Integer>();

	}

	static void setConstellation() throws JSONException {

		JSONObject ports = configJson.getJSONObject("ConstellationConfig");
		NUMBER_OF_INPUTS = ports.getInt("NUMBER_OF_INPUTS");
		// NUMBER_OF_INPUTS = BLeData.getInstance().getNumberOfInputs();
		NUMBER_OF_STATES_F1 = ports.getInt("NUMBER_OF_STATES_F1");
		NUMBER_OF_STATES_F2 = ports.getInt("NUMBER_OF_STATES_F2");
		NUMBER_OF_STATES_F3 = ports.getInt("NUMBER_OF_STATES_F3");
		numberOfGestures = ports.getInt("numberOfGestures");

	}

	public int predict(int[] bufferSerial) throws InterruptedException {
		long timeStart = System.currentTimeMillis();
		int[] data = new int[SVMRecognition.numberOfDataitems];
		for (int i = 0; i < bufferSerial.length; i++) {
			data[i] = bufferSerial[i];
			if (i < bufferSerial.length - 1)
				data[i + bufferSerial.length] = bufferSerial[i + 1]
						- bufferSerial[i];
		}
		int horizontal = svm.getPrediction(data);
		int level = svml.getPrediction(data);
		int z = svmz.getPrediction(data);
		
		//int level = svml.getPrediction(data);
		
		// Enqueue Location
		/*if(level != 0 && horizontal != 0 && z != 0){
			MainActivity.wSLocationQueue.enqueueLocation(new Location(horizontal, level, z));
			Log.i(TAG, "Level & Horizontal & Z: " + level + " " + horizontal + " " + z);
		}
		else{
			Location lastLocation = MainActivity.wSLocationQueue.getLastLocation();
			if(lastLocation != null){
				if(lastLocation.getX() != -1 && lastLocation.getY() != -1 && lastLocation.getZ() != -1){
					MainActivity.wSLocationQueue.enqueueLocation(new Location(-1, -1, -1));
					Log.i(TAG, "Level & Horizontal & Z: " + -1 + " " + -1 + " " + -1);
					MainActivity.wSLocationQueue.enqueueLocation(new Location(-1, -1, -1));
					Log.i(TAG, "Level & Horizontal & Z: " + -1 + " " + -1 + " " + -1);
				}
			}
			else{
				MainActivity.wSLocationQueue.enqueueLocation(new Location(-1, -1, -1));
				Log.i(TAG, "Level & Horizontal & Z: " + -1 + " " + -1 + " " + -1);
			}
		}*/
		
		if(level != 0){
			MainActivity.wSLocationQueue.enqueueLocation(new Location(0, level, 0));
			Log.i(TAG, "Level & Horizontal & Z: " + level + " " + 0 + " " + 0);
		}
		else{
			Location lastLocation = MainActivity.wSLocationQueue.getLastLocation();
			if(lastLocation != null){
				if(lastLocation.getX() != -1 && lastLocation.getY() != -1 && lastLocation.getZ() != -1){
					MainActivity.wSLocationQueue.enqueueLocation(new Location(-1, -1, -1));
					Log.i(TAG, "Level & Horizontal & Z: " + -1 + " " + -1 + " " + -1);
					MainActivity.wSLocationQueue.enqueueLocation(new Location(-1, -1, -1));
					Log.i(TAG, "Level & Horizontal & Z: " + -1 + " " + -1 + " " + -1);
				}
			}
			else{
				MainActivity.wSLocationQueue.enqueueLocation(new Location(-1, -1, -1));
				Log.i(TAG, "Level & Horizontal & Z: " + -1 + " " + -1 + " " + -1);
			}
		}
		
		// recognize gesture
		int state = level * 100 + horizontal * 10 + z;
		//int state = level * 100;
		if (state != 0)
			Log.i(TAG, "State: " + state);
		// Log.i(TAG, "Level & Horizontal & Z: " + level + " " + horizontal + " " + z);
		/*if (horizontal == 0 || z == 0)
			state = 0;*/

		int numFingers = 0;
		if (level > NUMBER_OF_STATES_F1)
			numFingers = 1;
		if (level > NUMBER_OF_STATES_F2)
			numFingers = 2;
		if (level > NUMBER_OF_STATES_F3)
			numFingers = 3;

		gestureRaw.add(state);
		parseGesture();

		if (updateGesture) {
			updateGesture = false;
			// Log.i(TAG, "Gesture No: " + g);
			return g;
		} else {
			// Log.i(TAG, "Number of fingers: " + numFingers);
			return (numFingers + 1) * 10;
		}
	}

	private void parseGesture() {
		int len = gestureRaw.size();
		if (len >= stopSize) {
			boolean check = true;
			int finalState = gestureRaw.get(len - 1);
			for (int i = 0; i < stopSize; i++) {
				if (gestureRaw.get(len - 1 - i) != finalState) {
					check = false;
					break;
				}
			}
			if (check) {
				gestureParse.clear();
				int c1 = 1, pstate = gestureRaw.get(0);
				if (pstate != 0)
					gestureParse.add(pstate);
				for (Integer state : gestureRaw) {
					if (c1 == 1 && state == pstate)
						continue;
					else {
						if (state != 0)
							gestureParse.add(state);
						pstate = state;
						c1 = 0;
					}
				}
				gestureRaw.clear();
				while (gestureParse.size() >= 2
						&& gestureParse.get(gestureParse.size() - 1) == gestureParse
								.get(gestureParse.size() - 2)) {
					gestureParse.remove(gestureParse.size() - 1);
				}
				TreeSet<Integer> size = new TreeSet<Integer>();
				for (Integer integer : gestureParse) {
					size.add(integer);
				}
				if (gestureParse.size() >= gestureSize && size.size() > 2) {
					System.err.println(gestureParse);
					gestureToWrite.clear();
					int length = gestureParse.size(); // gestureToWrite size is
														// always
														// numberOfGuestureInputs
					int filled = 0;
					for (int i = 1; i <= length; i++) {
						int upto = (numberOfGuestureInputs - 1) * i / length;
						for (; filled <= upto; filled++) {
							gestureToWrite.add(gestureParse.get(i - 1));
						}
					}
					if (recognize) {
						g = svmg.getPrediction(gestureToWrite);
						Log.i(TAG, "Gesture No: " + g);
						updateGesture = true;
					}
				}
			}
		}
	}

	public void initVariables() throws IOException, JSONException,
			java.text.ParseException {
		/*
		 * following code will read a json object and assign the values to the
		 * variables in the code. if you want to change some value. please open
		 * json file named configuration.json in data folder. Then it will be
		 * assigned in to the working code
		 */
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(appContext
					.getAssets().open("config/mainConfig.json")));
			if (reader != null)
				Log.d(TAG, "Main Config file was open");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String line = reader.readLine();
		String temp = "";
		while (line != null) {
			temp = temp + line;
			line = reader.readLine();
		}
		mainConfig = new JSONObject(temp);
		devicesObject = mainConfig.getJSONObject("devices"); // devicesObject
																// gets the JSON
																// object which
																// has the
																// device names
																// and the
																// relative
																// paths to the
																// device config
																// file(many
																// devices)
		JSONArray s = devicesObject.names();
		deviceNames = new String[s.length()];
		for (int i = 0; i < s.length(); i++) {
			deviceNames[i] = s.getString(i);
		}

		device_Name = mainConfig.getString("device_Name");
		deviceObject = devicesObject.getJSONObject(device_Name); // deviceObject
																	// gets the
																	// JSON
																	// object
																	// which has
																	// the
																	// device
																	// name and
																	// the
																	// relative
																	// path to
																	// the
																	// device
																	// config
																	// file(Single
																	// device)
		deviceConfigPath = deviceObject.getString("path"); // getting the path
															// to the device
															// config files
		String mainConfigPath = "config";
		deviceConfigPath = mainConfigPath + "/" + deviceConfigPath; // adding
																	// the
																	// relative
																	// path to
																	// the ?
																	// configuration
																	// folder's
																	// path

		try {
			reader = new BufferedReader(new InputStreamReader(appContext
					.getAssets().open(
							"config/devices/" + device_Name
									+ "/json/variables.json")));
			if (reader != null)
				Log.d(TAG, "Variable file was open");
		} catch (IOException e) {
			e.printStackTrace();
		}
		line = reader.readLine();
		temp = "";
		while (line != null) {
			temp = temp + line;
			line = reader.readLine();
		}

		varJson = new JSONObject(temp); // getting the JsonObject and assigning
										// it to the configJson
		filenameFinal = varJson.getString("filenameFinal");
		filenameIntermid = varJson.getString("filenameIntermid");
		csvPathG = varJson.getString("csvPathG");
		csvPathH = varJson.getString("csvPathH");
		csvPathL = varJson.getString("csvPathL");
		csvPathZ = varJson.getString("csvPathZ");
		modelPathG = varJson.getString("modelPathG");
		modelPathH = varJson.getString("modelPathH");
		modelPathL = varJson.getString("modelPathL");
		modelPathZ = varJson.getString("modelPathZ");
		gesture1 = varJson.getString("gesture1");
		gesture2 = varJson.getString("gesture2");
		gesture3 = varJson.getString("gesture3");

		try {
			reader = new BufferedReader(new InputStreamReader(appContext
					.getAssets().open(deviceConfigPath)));
			if (reader != null)
				Log.d(TAG, "Device Configuration file was opened");
		} catch (IOException e) {
			e.printStackTrace();
		}

		line = reader.readLine();
		temp = "";
		while (line != null) {
			temp = temp + line;
			line = reader.readLine();
		}
		// getting the JsonObject and assigning it to the configJson
		configJson = new JSONObject(temp);

		// below the variables will be assigned by the values saved in the Json
		// Object
		constellationNumber = configJson.getInt("constellationNumber");
		numberOfGuestureInputs = configJson.getInt("numberOfGuestureInputs");
		updateGesture = configJson.getBoolean("updateGesture");
		stopSize = configJson.getInt("stopSize");
		gestureSize = configJson.getInt("gestureSize");
		currentGesture = configJson.getInt("currentGesture");
	}

	private static void setAbsolutePath() throws JSONException {

		// here are the absolute path values for the models and csv files

		filenameIntermid = "config/devices/" + device_Name + "/csv/"
				+ filenameIntermid;
		csvPathH = "config/devices/" + device_Name + "/csv/" + csvPathH;
		csvPathL = "config/devices/" + device_Name + "/csv/" + csvPathL;
		csvPathZ = "config/devices/" + device_Name + "/csv/" + csvPathZ;
		csvPathG = "config/devices/" + device_Name + "/csv/" + csvPathG;
		modelPathH = "config/devices/" + device_Name + "/models/" + modelPathH;
		modelPathL = "config/devices/" + device_Name + "/models/" + modelPathL;
		modelPathZ = "config/devices/" + device_Name + "/models/" + modelPathZ;
		modelPathG = "config/devices/" + device_Name + "/models/" + modelPathG;
	}

}
