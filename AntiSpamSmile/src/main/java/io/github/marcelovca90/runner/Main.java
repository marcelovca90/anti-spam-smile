package io.github.marcelovca90.runner;

import java.io.File;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.primes.Primes;

import io.github.marcelovca90.common.ClassType;
import io.github.marcelovca90.helper.DatasetHelper;
import io.github.marcelovca90.helper.FeatureSelectionHelper;
import io.github.marcelovca90.helper.MethodHelper;
import io.github.marcelovca90.helper.ValidationHelper;
import smile.classification.Classifier;
import smile.classification.DecisionTree;
import smile.classification.RandomForest;
import smile.data.AttributeDataset;

public class Main
{
    private static final String METADATA_PATH = "C:\\Users\\marcelovca90\\git\\anti-spam-weka-data\\2017_BASE2\\metadata.txt";

    @SuppressWarnings (
    { "rawtypes", "unchecked" })
    public static void main(String[] args) throws Exception
    {
        Class<? extends Classifier>[] clazzes = new Class[]
        {
                DecisionTree.class, RandomForest.class
        };

        for (Class clazz : clazzes)
        {
            for (Triple<String, Integer, Integer> metadatum : DatasetHelper.loadMetadata(METADATA_PATH))
            {
                // read data
                AttributeDataset ham = DatasetHelper.read(metadatum.getLeft() + File.separator + "ham", ClassType.HAM);
                AttributeDataset spam = DatasetHelper.read(metadatum.getLeft() + File.separator + "spam", ClassType.SPAM);
                AttributeDataset dataset = DatasetHelper.mergeDataSets(ham, spam);

                // select features
                int noFeaturesBefore = dataset.attributes().length;
                dataset = FeatureSelectionHelper.sumSquaresRatio(dataset);
                int noFeaturesAfter = dataset.attributes().length;

                // initialize rng seed
                int seed = 2;

                System.out.println(String.format("%s with (%d -> %d) features", clazz.getName(), noFeaturesBefore, noFeaturesAfter));

                // perform 10 executions
                for (int run = 0; run < 10; run++)
                {
                    // shuffle data
                    dataset = DatasetHelper.shuffle(dataset, seed);

                    // build train/test data
                    Pair<AttributeDataset, AttributeDataset> pair = DatasetHelper.split(dataset, 0.5);
                    AttributeDataset train = pair.getLeft();
                    AttributeDataset test = pair.getRight();
                    double[][] trainx = train.toArray(new double[train.size()][]);
                    int[] trainy = train.toArray(new int[train.size()]);
                    double[][] testx = test.toArray(new double[test.size()][]);
                    int[] testy = test.toArray(new int[test.size()]);

                    // train and test classifier
                    MethodHelper.init(trainx, trainy);
                    Classifier classifier = MethodHelper.forClass(clazz);
                    ValidationHelper.aggregate(classifier, testx, testy);

                    // update rng seed
                    seed = Primes.nextPrime(seed + 1);
                }

                ValidationHelper.consolidate(clazz);

                System.out.println();
            }
        }
    }
}
