package de.unidue.ltl.escrito.examples.basicexamples.essay;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import de.unidue.ltl.escrito.examples.basics.Experiments_ImplBase;
import de.unidue.ltl.escrito.examples.basics.FeatureSettings;
import de.unidue.ltl.escrito.io.essay.MewsReader;
import de.unidue.ltl.escrito.io.shortanswer.Asap2Reader;
import meka.core.F;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.core.Constants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class WekaClassificationExampleMEWS extends Experiments_ImplBase implements Constants {

	public static void main(String[] args) throws Exception {
		File trainFile = new File(System.getenv("DKPRO_HOME")+"/datasets/mews/data_ordinal_train.csv");
		File testFile = new File(System.getenv("DKPRO_HOME")+"/datasets/mews/data_ordinal_test.csv");
		if(!trainFile.exists() || !testFile.exists())
			createTrainTestSplit(
					new File(System.getenv("DKPRO_HOME") + "/datasets/mews/data_ordinal.csv"),
					0.8);

		runMEWSBaselineExperiment(
				System.getenv("DKPRO_HOME")+"/datasets/mews/data_ordinal_train.csv",
				System.getenv("DKPRO_HOME")+"/datasets/mews/data_ordinal_test.csv",
				"en");
	}

	private static void createTrainTestSplit(File csvFile, double trainRatio) throws IOException {
		File trainFile = new File(csvFile.getParentFile(),
				csvFile.getName().replace(".csv", "_train.csv"));

		File testFile = new File(csvFile.getParentFile(),
				csvFile.getName().replace(".csv", "_test.csv"));

		try(InputStream is = Files.newInputStream(csvFile.toPath());
			InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader br = new BufferedReader(isr);
			CSVReader csvReader = new CSVReader(br);

			OutputStream osTrain = Files.newOutputStream(trainFile.toPath());
			OutputStreamWriter oswTrain = new OutputStreamWriter(osTrain, StandardCharsets.UTF_8);
			BufferedWriter bwTrain = new BufferedWriter(oswTrain);
			CSVWriter csvWriterTrain = new CSVWriter(bwTrain);

			OutputStream osTest = Files.newOutputStream(testFile.toPath());
			OutputStreamWriter oswTest = new OutputStreamWriter(osTest, StandardCharsets.UTF_8);
			BufferedWriter bwTest = new BufferedWriter(oswTest);
			CSVWriter csvWriterTest = new CSVWriter(bwTest)) {

			List<String[]> allLines = csvReader.readAll();
			int splitPos = (int) (allLines.size() * trainRatio);

			List<String[]> train = allLines.subList(0, splitPos - 1);

			List<String[]> test = new LinkedList<>();
			test.add(allLines.get(0));
			test.addAll(allLines.subList(splitPos, allLines.size() - 1));

			csvWriterTrain.writeAll(train);
			csvWriterTest.writeAll(test);

		} catch (CsvException e) {
			throw new RuntimeException(e);
		}
	}


	protected static void runMEWSBaselineExperiment(String trainData, String testData,
													String languageCode) throws Exception {
		String EXPERIMENT_NAME = "MEWS_Train_Test_Example";
			CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(MewsReader.class,
					MewsReader.PARAM_INPUT_FILE, trainData);

			CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(MewsReader.class,
					MewsReader.PARAM_INPUT_FILE, testData);
			runBaselineExperiment( EXPERIMENT_NAME, readerTrain, readerTest, languageCode);
	}

	private static void runBaselineExperiment(String experimentName, CollectionReaderDescription readerTrain,
			CollectionReaderDescription readerTest, String languageCode) throws Exception {
		Map<String, Object> dimReaders = new HashMap<String, Object>();
		dimReaders.put(DIM_READER_TRAIN, readerTrain);
		dimReaders.put(DIM_READER_TEST, readerTest);

		Dimension<String> learningDims = Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL);
		Dimension<Map<String, Object>> learningsArgsDims = getStandardWekaClassificationArgsDim();

		ParameterSpace pSpace = null;
			pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders), learningDims,
					Dimension.create(DIM_FEATURE_MODE, FM_UNIT), FeatureSettings.getFeatureSetsDimBaseline(),
					learningsArgsDims);
			runTrainTest(pSpace, experimentName, getPreprocessing(languageCode));
	}
}
