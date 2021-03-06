//**********************************************************************
// Copyright (c) 2017 Telefonaktiebolaget LM Ericsson, Sweden.
// All rights reserved.
// The Copyright to the computer program(s) herein is the property of
// Telefonaktiebolaget LM Ericsson, Sweden.
// The program(s) may be used and/or copied with the written permission
// from Telefonaktiebolaget LM Ericsson or in accordance with the terms
// and conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.
// **********************************************************************
package io.github.marcelovca90.helper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import smile.classification.Classifier;
import smile.validation.Accuracy;
import smile.validation.ClassificationMeasure;
import smile.validation.FMeasure;
import smile.validation.Precision;
import smile.validation.Recall;

@SuppressWarnings("rawtypes")
public class ValidationHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationHelper.class);
    private static final Map<Class<? extends Classifier>, Map<Class<? extends ClassificationMeasure>, DescriptiveStatistics>> RESULTS = new HashMap<>();

    public static void aggregate(Classifier<double[]> classifier, double[][] x, int[] y)
    {
        RESULTS.putIfAbsent(classifier.getClass(), new LinkedHashMap<>());

        int[] truth = y;
        int[] prediction = new int[truth.length];
        for (int i = 0; i < x.length; i++)
            prediction[i] = classifier.predict(x[i]);

        double accuracy = 100.0 * new Accuracy().measure(truth, prediction);
        RESULTS.get(classifier.getClass()).putIfAbsent(Accuracy.class, new DescriptiveStatistics());
        RESULTS.get(classifier.getClass()).get(Accuracy.class).addValue(accuracy);
        assert !Double.isNaN(accuracy) : "accuracy must be a double";

        double precision = 100.0 * new Precision().measure(truth, prediction);
        RESULTS.get(classifier.getClass()).putIfAbsent(Precision.class, new DescriptiveStatistics());
        RESULTS.get(classifier.getClass()).get(Precision.class).addValue(precision);
        assert !Double.isNaN(precision) : "precision must be a double";

        double recall = 100.0 * new Recall().measure(truth, prediction);
        RESULTS.get(classifier.getClass()).putIfAbsent(Recall.class, new DescriptiveStatistics());
        RESULTS.get(classifier.getClass()).get(Recall.class).addValue(recall);
        assert !Double.isNaN(recall) : "recall must be a double";

        double fmeasure = 100.0 * new FMeasure().measure(truth, prediction);
        RESULTS.get(classifier.getClass()).putIfAbsent(FMeasure.class, new DescriptiveStatistics());
        RESULTS.get(classifier.getClass()).get(FMeasure.class).addValue(fmeasure);
        assert !Double.isNaN(fmeasure) : "fmeasure must be a double";
    }

    public static void consolidate(Class<? extends Classifier> clazz)
    {
        LOGGER.info(RESULTS.get(clazz).keySet().stream().map(k -> StringUtils.rightPad(k.getSimpleName(), 15)).collect(Collectors.joining("\t")));
        LOGGER.info(RESULTS.get(clazz).values().stream().map(v -> String.format("%.2f ± %.2f", v.getMean(), computeConfidenceInterval(v, 0.05))).collect(Collectors.joining("\t")));
    }

    private static double computeConfidenceInterval(DescriptiveStatistics statistics, double significance)
    {
        TDistribution tDist = new TDistribution(statistics.getN() - 1);
        double a = tDist.inverseCumulativeProbability(1.0 - significance / 2);
        return a * statistics.getStandardDeviation() / Math.sqrt(statistics.getN());
    }
}
