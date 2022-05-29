package de.unidue.ltl.escrito.io.essay;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.unidue.ltl.escrito.core.types.LearnerAnswer;
import de.unidue.ltl.escrito.io.generic.CsvReader;
import de.unidue.ltl.escrito.io.util.Utils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.dkpro.tc.api.type.TextClassificationOutcome;
import org.dkpro.tc.api.type.TextClassificationTarget;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class MewsReader extends CsvReader<MewsItem> {

    protected static final String LANGUAGE = "en";

    protected int currentIndex;


    public MewsReader() {
        super();
        this.corpusName = "MEWS";
    }

    @Override
    public void getNext(JCas jcas)
            throws IOException, CollectionException
    {
        MewsItem item = items.poll();
        getLogger().debug(item);
        //String itemId = String.valueOf(item.getPromptId()+"_"+item.getAnswerId());
        try {
            jcas.setDocumentLanguage(LANGUAGE);
            jcas.setDocumentText(Utils.cleanString(item.getEssayText()));
            DocumentMetaData dmd = DocumentMetaData.create(jcas);
            dmd.setDocumentId(item.getDocumentID());
            //dmd.setDocumentTitle(item.getText());
            dmd.setDocumentUri(inputFileURL.toURI().toString());
            dmd.setCollectionId(item.getDocumentID());
        } catch (URISyntaxException e) {
            throw new CollectionException(e);
        }

        LearnerAnswer learnerAnswer = new LearnerAnswer(jcas, 0, jcas.getDocumentText().length());
        learnerAnswer.setPromptId(String.valueOf(0));
        learnerAnswer.addToIndexes();


        String category = "Code1_Subjektiver_Gesamteindruck_0_0";
        /*for(String category : item.getCategories())*/ {
            TextClassificationTarget unit = new TextClassificationTarget(jcas, 0, jcas.getDocumentText().length());
            // will add the token content as a suffix to the ID of this unit
            unit.setSuffix(item.getDocumentID());
            unit.addToIndexes();

            TextClassificationOutcome outcome = new TextClassificationOutcome(jcas, 0, jcas.getDocumentText().length());
            outcome.setOutcome(Integer.toString(item.getRating(category).getScore()));
            outcome.addToIndexes();
        }

        currentIndex++;
    }

    @Override
    public MewsItem createItem(String[] values, String[] headings) {
        Map<String, MewsItem.Rating> ratings = new HashMap<>();
        for (int i = 1; i < headings.length-1; i += 4) {
            ratings.put(headings[i], new MewsItem.Rating(Integer.parseInt(values[i]), values[i+1]));
        }
        return new MewsItem(values[ArrayUtils.indexOf(headings, "DocumentID")].hashCode() + "",
                values[ArrayUtils.indexOf(headings, "DocumentText")], ratings);
    }
}
