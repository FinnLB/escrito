package de.unidue.ltl.escrito.io.generic;

import au.com.bytecode.opencsv.CSVReader;
import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public abstract class CsvReader<T> extends JCasCollectionReader_ImplBase {
    public static final String PARAM_INPUT_FILE = "InputFile";
    @ConfigurationParameter(name = PARAM_INPUT_FILE, mandatory = true)
    protected String inputFileString;
    protected URL inputFileURL;

    public static final String PARAM_ENCODING = "Encoding";
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = false, defaultValue = "UTF-8")
    private String encoding;

    public static final String PARAM_SEPARATOR = "Separator";
    @ConfigurationParameter(name = PARAM_SEPARATOR, mandatory = false, defaultValue = ",")
    private char separator;

    public static final String PARAM_PROMPT_ID = "PromptID";
    @ConfigurationParameter(name = PARAM_PROMPT_ID, mandatory = false, defaultValue = "-1")
    protected String requestedPromptId;

    public static final String PARAM_CORPUSNAME = "corpusName";
    @ConfigurationParameter(name = PARAM_CORPUSNAME, mandatory = false)
    protected String corpusName;

    protected int currentIndex;

    protected Queue<T> items;

    @Override
    public void initialize(UimaContext aContext)
            throws ResourceInitializationException {
        items = new LinkedList<>();
        try {
            inputFileURL = ResourceUtils.resolveLocation(inputFileString, this, aContext);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            inputFileURL.openStream(),
                            encoding
                    )
            );
            CSVReader csvReader = new CSVReader(reader, separator);
            String[] nextLine;
            String[] headings = csvReader.readNext();
            while ((nextLine = csvReader.readNext()) != null) {
                T newItem = createItem(nextLine, headings);
                items.add(newItem);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new ResourceInitializationException(ioe);
        }
    }

    public T createItem(String[] values, String[] headings) {
        return null;
    }

    @Override
    public Progress[] getProgress()
    {
        return new Progress[] { new ProgressImpl(currentIndex, currentIndex, Progress.ENTITIES) };
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return !items.isEmpty();
    }
}
