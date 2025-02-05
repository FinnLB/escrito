package de.unidue.ltl.escrito.features.ngrams;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ExternalResourceDescription;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.features.Instance;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.io.JsonDataWriter;
import org.dkpro.tc.core.util.TaskUtils;
import org.dkpro.tc.features.ngram.meta.PosNGramMC;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.Gson;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.unidue.ltl.escrito.features.core.io.TestReaderSingleLabel;
import de.unidue.ltl.escrito.features.core.io.TestReaderSingleLabelDocumentReader;


public class MixedNGramsFeatureExtractorTest
extends LuceneMetaCollectionBasedFeatureTestBase
{

   @Before
   public void setupLogging()
   {
       super.setup();
       featureClass = MixedNGrams.class;
       metaCollectorClass = LuceneMixedNGramMetaCollector.class;
   }

   private Collection<? extends String> getUniqueFeatureNames(List<Instance> instances)
   {
       Set<String> s = new HashSet<>();

       for (Instance i : instances) {
           for (Feature f : i.getFeatures()) {
               s.add(f.getName());
           }
       }

       return s;
   }

   private int getUniqueOutcomes(List<Instance> instances)
   {
       Set<String> outcomes = new HashSet<String>();
       instances.forEach(x -> outcomes.addAll(x.getOutcomes()));
       return outcomes.size();
   }

   @Override
   protected void evaluateMetaCollection(File luceneFolder) throws Exception
   {
       Set<String> entries = getEntriesFromIndex(luceneFolder);
       assertEquals(40, entries.size());
   }

   @Override
   protected void evaluateExtractedFeatures(File output) throws Exception
   {
       List<Instance> instances = readInstances(output);
       assertEquals(4, instances.size());
       assertEquals(1, getUniqueOutcomes(instances));

       Set<String> featureNames = new HashSet<String>(getUniqueFeatureNames(instances));
       assertEquals(30, featureNames.size());
       System.out.println(featureNames);
       assertTrue(featureNames.contains("MixedNGramsNormalizedFeatureExtractor_ADJ"));
       assertTrue(featureNames.contains("MixedNGramsNormalizedFeatureExtractor_ADJ_than"));
   }

   @Override
   protected void runMetaCollection(File luceneFolder, AnalysisEngineDescription metaCollector)
       throws Exception
   {

       CollectionReaderDescription reader = getMetaReader();

       AnalysisEngineDescription segmenter = AnalysisEngineFactory
               .createEngineDescription(BreakIteratorSegmenter.class);

       AnalysisEngineDescription posTagger = AnalysisEngineFactory.createEngineDescription(
               OpenNlpPosTagger.class, OpenNlpPosTagger.PARAM_LANGUAGE, "en");

       SimplePipeline.runPipeline(reader, segmenter, posTagger, metaCollector);
   }

   @Override
   protected CollectionReaderDescription getMetaReader() throws Exception
   {
       return CollectionReaderFactory.createReaderDescription(TestReaderSingleLabelDocumentReader.class,
               TestReaderSingleLabelDocumentReader.PARAM_SOURCE_LOCATION, "src/test/resources/ngrams/*.txt");
   }

   @Override
   protected CollectionReaderDescription getFeatureReader() throws Exception
   {
       return getMetaReader();
   }

   @Override
   protected Object[] getMetaCollectorParameters(File luceneFolder)
   {
       return new Object[] { PosNGramsNormalizedFeatureExtractor.PARAM_UNIQUE_EXTRACTOR_NAME, "123",
    		   PosNGramsNormalizedFeatureExtractor.PARAM_NGRAM_USE_TOP_K, "30", PosNGramsNormalizedFeatureExtractor.PARAM_SOURCE_LOCATION,
               luceneFolder.toString(), PosNGramMC.PARAM_TARGET_LOCATION,
               luceneFolder.toString() };
   }

   @Override
   protected Object[] getFeatureExtractorParameters(File luceneFolder)
   {
       return getMetaCollectorParameters(luceneFolder);
   }

   protected void runFeatureExtractor(File luceneFolder,
           AnalysisEngineDescription featureExtractor)
       throws Exception
   {

       CollectionReaderDescription reader = getFeatureReader();

       AnalysisEngineDescription segmenter = AnalysisEngineFactory
               .createEngineDescription(BreakIteratorSegmenter.class);

       AnalysisEngineDescription posTagger = AnalysisEngineFactory.createEngineDescription(
               OpenNlpPosTagger.class, OpenNlpPosTagger.PARAM_LANGUAGE, "en");

       SimplePipeline.runPipeline(reader, segmenter, posTagger, featureExtractor);
   }
}

