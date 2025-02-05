package de.unidue.ltl.escrito.core.learningcurve;


import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import org.apache.commons.io.FileUtils;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.storage.StorageService.AccessMode;
import org.dkpro.lab.task.Discriminator;
import org.dkpro.lab.task.impl.ExecutableTaskBase;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.task.InitTask;
import org.dkpro.tc.ml.weka.core.MekaTrainer;
import org.dkpro.tc.ml.weka.core.WekaTrainer;
import org.dkpro.tc.ml.weka.core._eka;
import org.dkpro.tc.ml.weka.task.WekaTestTask;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.BinaryCasReader;
import de.unidue.ltl.escrito.core.Utils;




/**
 * Builds the classifier from the training data and performs classification on the test data.
 * Repeats that for different amounts of training data in order to build a learning curve.
 * 
 */
public class LearningCurveTask
//extends WekaTestTask
extends ExecutableTaskBase
implements Constants
{
	//	@Discriminator
	//	private List<String> classificationArguments;   
	//	@Discriminator
	//	private String featureMode;
	//	@Discriminator
	//	private String learningMode;

	@Discriminator(name = DIM_CLASSIFICATION_ARGS)
	protected List<Object> classificationArguments;

	@Discriminator(name = DIM_FEATURE_SEARCHER_ARGS)
	protected List<String> featureSearcher;

	@Discriminator(name = DIM_ATTRIBUTE_EVALUATOR_ARGS)
	protected List<String> attributeEvaluator;

	@Discriminator(name = DIM_LABEL_TRANSFORMATION_METHOD)
	protected String labelTransformationMethod;

	@Discriminator(name = DIM_NUM_LABELS_TO_KEEP)
	protected int numLabelsToKeep;

	@Discriminator(name = DIM_APPLY_FEATURE_SELECTION)
	protected boolean applySelection;

	@Discriminator(name = DIM_FEATURE_MODE)
	protected String featureMode;

	@Discriminator(name = DIM_LEARNING_MODE)
	protected String learningMode;

	@Discriminator(name = DIM_BIPARTITION_THRESHOLD)
	protected String threshold;

	// Should this be static? If not, how can I caccess it in the report?
	@Discriminator(name = "dimension_iterations")
	public static Integer ITERATIONS = 100;

	@Discriminator(name = "dimension_number_of_training_instances")
	public static int[] NUMBER_OF_TRAINING_INSTANCES = {4,8,16,32,64,128,256};


	@Override
	public void execute(TaskContext aContext)
			throws Exception
	{
		boolean multiLabel = false;
		Map<String, String> instanceId2TextMap = Utils.getInstanceId2TextMapTrain(aContext);
		File arffFileTrain = Utils.getFile(aContext, TEST_TASK_INPUT_KEY_TRAINING_DATA,
				FILENAME_DATA_IN_CLASSIFIER_FORMAT, AccessMode.READONLY);
		File arffFileTest = Utils.getFile(aContext, TEST_TASK_INPUT_KEY_TEST_DATA,
				FILENAME_DATA_IN_CLASSIFIER_FORMAT, AccessMode.READONLY);

		Instances trainData_fix = _eka.getInstances(arffFileTrain, multiLabel);
		Instances testData_fix = _eka.getInstances(arffFileTest, multiLabel);

		System.out.println("Execute LearningCurveTask ...");
		for (Integer numberInstances : NUMBER_OF_TRAINING_INSTANCES) {
			for (int iteration=0; iteration<ITERATIONS; iteration++) {
				System.out.println(numberInstances+"\t"+iteration);
				
				Instances trainData = new Instances(trainData_fix);
				Instances testData = new Instances(testData_fix);
				
				if (numberInstances > trainData.size()) {
					System.out.println("Not enough training data!");
					continue;
				}

				File model = aContext.getFile(MODEL_CLASSIFIER, AccessMode.READWRITE);				
				WekaTrainer trainer = new WekaTrainer();
				//Classifier cl = trainer.train(trainData, model, getParameters(classificationArguments));

				//Classifier cl = Utils.getClassifier(learningMode, classificationArguments);
				//				Classifier cl = AbstractClassifier.forName(classificationArguments.get(0), classificationArguments
				//						.subList(1, classificationArguments.size()).toArray(new String[0]));

				Instances copyTestData = new Instances(testData);
				testData = _eka.removeInstanceId(testData, multiLabel);


				Random generator = new Random();
				generator.setSeed(System.nanoTime());
				trainData.randomize(generator);

				// remove fraction of training data that should not be used for training
				for (int i = trainData.size() - 1; i >= numberInstances; i--) {
					trainData.delete(i);
				}
				Instances copyTrainData = new Instances(trainData);
				trainData = _eka.removeInstanceId(trainData, multiLabel);


				// file to hold prediction results
				File evalOutput = new File(aContext.getStorageLocation(TEST_TASK_OUTPUT_KEY,
						AccessMode.READWRITE)
						.getPath()
						+ "/" + Constants.EVAL_FILE_NAME + "_" + numberInstances + "_" + iteration);

				File trainItemIds = new File(aContext.getStorageLocation(TEST_TASK_OUTPUT_KEY,
						AccessMode.READWRITE)
						.getPath()
						+ "/" + Constants.EVAL_FILE_NAME + "_" + numberInstances + "_" + iteration + "_itemIds.txt");

				// train the classifier on the train set split - not necessary in multilabel setup, but
				// in single label setup
				
			//	Classifier cl = trainer.train(trainData, model, Utils.getParameters(classificationArguments));
			// Wir chreiben das Model nicht jedes Mal!
				Classifier cl = trainWithoutSerialization(trainData, model, Utils.getParameters(classificationArguments));

				
				//cl.buildClassifier(trainData);

				
				weka.core.SerializationHelper.write(evalOutput.getAbsolutePath(),
						Utils.getEvaluationSinglelabel(cl, trainData, testData));
				testData = Utils.getPredictionInstancesSingleLabel(testData, cl);
				testData = _eka.addInstanceId(testData, copyTestData, multiLabel);
				BufferedWriter bw = new BufferedWriter(new FileWriter(trainItemIds));
				for (Instance inst : copyTrainData){
					//				bw.write(inst.stringValue(0)+"\n");
					String instanceId = inst.stringValue(copyTrainData.attribute(Constants.ID_FEATURE_NAME).index());
					instanceId = instanceId.substring(instanceId.indexOf("_0_")+3);
					// TODO comment in again
					bw.write(instanceId+"\t"+instanceId2TextMap.get(instanceId)
					+"\t"+inst.classValue()+"\n");
				//	System.out.println(instanceId+"\t"+instanceId2TextMap.get(instanceId));
				}
				bw.close();
				//                // Write out the predictions
				//                DataSink.write(aContext.getStorageLocation(TEST_TASK_OUTPUT_KEY, AccessMode.READWRITE)
				//                        .getAbsolutePath() + "/" + PREDICTIONS_FILENAME + "_" + trainPercent, testData); 
			} 	
		}
		File arffFileTest2 = Utils.getFile(aContext, TEST_TASK_INPUT_KEY_TEST_DATA,
				FILENAME_DATA_IN_CLASSIFIER_FORMAT, AccessMode.READONLY);
		Instances testData = _eka.getInstances(arffFileTest2, multiLabel);
		File testItemIds = new File(aContext.getStorageLocation("",
				AccessMode.READWRITE)
				.getPath()
				+ "/" + "testItemIds.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(testItemIds));
		for (Instance inst : testData){
			bw.write(inst.value(0)+"\n");
		}
		bw.close();
	}


	private Classifier trainWithoutSerialization(Instances data, File model, List<String> parameters) throws Exception {
		  String algoName = parameters.get(0);
	        List<String> algoParameters = parameters.subList(1, parameters.size());

	        // build classifier
	        Classifier cl = AbstractClassifier.forName(algoName, algoParameters.toArray(new String[0]));
	        cl.buildClassifier(data);

	        return cl;
	}

	
	

}
