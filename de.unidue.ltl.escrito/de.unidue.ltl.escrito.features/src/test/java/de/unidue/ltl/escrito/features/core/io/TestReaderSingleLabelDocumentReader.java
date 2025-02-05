package de.unidue.ltl.escrito.features.core.io;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;

import org.dkpro.tc.api.io.TCReaderSingleLabel;
import org.dkpro.tc.api.type.JCasId;
import org.dkpro.tc.api.type.TextClassificationOutcome;
import org.dkpro.tc.api.type.TextClassificationTarget;

public class TestReaderSingleLabelDocumentReader
    extends TextReader
    implements TCReaderSingleLabel
{
    public static final String PARAM_SUPPRESS_DOCUMENT_ANNOTATION = "PARAM_SUPPRESS_DOCUMENT_ANNOTATION";
    @ConfigurationParameter(name = "PARAM_SUPPRESS_DOCUMENT_ANNOTATION", mandatory = true, defaultValue = "false")
    private boolean suppress;

    int jcasId;

    @Override
    public void getNext(CAS aCAS) throws IOException, CollectionException
    {
        super.getNext(aCAS);

        JCas jcas;
        try {
            jcas = aCAS.getJCas();
            JCasId id = new JCasId(jcas);
            id.setId(jcasId++);
            id.addToIndexes();
        }
        catch (CASException e) {
            throw new CollectionException();
        }

        TextClassificationOutcome outcome = new TextClassificationOutcome(jcas);
        outcome.setOutcome(getTextClassificationOutcome(jcas));
        outcome.addToIndexes();

        if (!suppress) {
            new TextClassificationTarget(jcas, 0, jcas.getDocumentText().length()).addToIndexes();
        }
    }

    @Override
    public String getTextClassificationOutcome(JCas jcas) throws CollectionException
    {
        return "test";
    }
}