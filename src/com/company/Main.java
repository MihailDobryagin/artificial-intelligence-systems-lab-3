package com.company;

import com.company.tree.Tree;
import com.company.utils.Pair;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.abs;

public class Main {

  private static final String DATASET_PATH =
    "C:\\Users\\Михаил\\Desktop\\Университет\\3 курс\\CИИ\\Лабораторные\\3\\Project\\resources\\dataset.csv";
  private static final String JSON_FILE_PATH = "resources/tree.json";
  private static final int COUNT_OF_LINES = 8124;
  private static final double DATA_RATIO = 0.8;
  private static final int CLASS_COLUMN_INDEX = 0;

  public static void main(String[] args) throws IOException {
    var wholeData = readCsv(DATASET_PATH, COUNT_OF_LINES);
    var splitted = splitData(wholeData, DATA_RATIO);
//    var dataWithClass = readCsv(DATASET_PATH, COUNT_OF_LINES);
    var dataWithClass = splitted.first;
    List<String> classes = dataWithClass.stream()
      .map(l -> l.get(CLASS_COLUMN_INDEX))
      .collect(Collectors.toCollection(ArrayList::new));

    var dataWithoutClass = getDataWithoutClass(dataWithClass);

    var columnNamesWithoutClass = columnNames.subList(1, columnNames.size());

    var randomColumnIndexes = new HashSet<Integer>();
    {
      var currentColumnIndexes = IntStream
        .range(0, columnNamesWithoutClass.size())
        .boxed()
        .collect(Collectors.toCollection(LinkedList::new));

      var random = new Random();
      for (int i = 0; i < 5; i++) {
        var randNumber = abs(random.nextInt()) % currentColumnIndexes.size();
        randomColumnIndexes.add(currentColumnIndexes.remove(randNumber));
      }
    }

    System.out.println(
      "Target columns: " +
        randomColumnIndexes.stream().sorted().map(columnNamesWithoutClass::get).collect(Collectors.toList())
    );


    var tree = Tree.makeTree(columnNamesWithoutClass, randomColumnIndexes, classes, dataWithoutClass, 0.5);


    //////////////////////////////////////////////////////////////////////////
    // Test //

    dataWithClass = splitted.second;
    classes = dataWithClass.stream()
      .map(l -> l.get(CLASS_COLUMN_INDEX))
      .collect(Collectors.toCollection(ArrayList::new));

    dataWithoutClass = getDataWithoutClass(dataWithClass);

    double accuracy;

    {
      int count_of_true = 0;
      for (int i = 0; i < classes.size(); i++) {
        var prediction = tree.predictClass(dataWithoutClass.get(i));
        if (prediction.equals(classes.get(i)))
          count_of_true++;
      }
      accuracy = ((double) count_of_true) / ((double) classes.size());
    }

    System.out.println("accuracy : " + accuracy);
    System.out.println();

    var possibleClasses = new HashSet<>(classes);
    var good_prediction = new HashMap<String, Integer>();
    var bad_prediction = new HashMap<String, Integer>();
    var missed = new HashMap<String, Integer>();

    {
      possibleClasses.forEach(possibleClass -> {
        good_prediction.put(possibleClass, 0);
        bad_prediction.put(possibleClass, 0);
        missed.put(possibleClass, 0);
      });

      for (int i = 0; i < classes.size(); i++) {
        var prediction = tree.predictClass(dataWithoutClass.get(i));
        var curClass = classes.get(i);
        if (prediction.equals(classes.get(i))) {
          var prevValue = good_prediction.get(curClass);
          prevValue++;
          good_prediction.put(curClass, prevValue);
        } else {
          var prevValue = bad_prediction.get(curClass);
          prevValue++;
          bad_prediction.put(curClass, prevValue);
          prevValue = missed.get(prediction);
          prevValue++;
          missed.put(prediction, prevValue);
        }
      }
    }

    good_prediction.forEach((key, value) -> {
      double precision = ((double) value) / ((double) (value + missed.get(key)));
      double recall = ((double) value) / ((double) (value + bad_prediction.get(key)));
      System.out.println("Class '" + key + "'");
      System.out.println("precision : " + precision);
      System.out.println("   recall : " + recall);
      System.out.println();
    });

    drawAucRoc(tree, classes, dataWithoutClass);
    drawAucPr(tree, randomColumnIndexes, classes, dataWithoutClass);
  }

  private static Pair<List<List<String>>, List<List<String>>> splitData(
    List<? extends List<String>> data, double ratio
  ) {
    int size = data.size();
    var shuffledData = new ArrayList<List<String>>(data);
    Collections.shuffle(shuffledData);

    int borderIdx = (int) Math.round(size * ratio);

    return Pair.of(shuffledData.subList(0, borderIdx), shuffledData.subList(borderIdx, size));
  }

  private static void drawAucRoc(Tree tree, List<String> classes, List<? extends List<String>> attrValues) {
    XYChart chart = QuickChart.getChart(
      "AUC ROC", "FPR", "TPR", "border", new double[]{0., 1.}, new double[]{0., 1.}
    );

    var x = new LinkedList<>(List.of(0.));
    var y = new LinkedList<>(List.of(0.));

    int size = classes.size();

    ArrayList<Pair<Integer, Pair<String, Double>>> predictedClassesWithProbByOriginalIdx = new ArrayList<>(size);

    int countOfPositives = 0;

    for (int i = 0; i < size; i++) {
      var prediction = tree.predict(attrValues.get(i));
      predictedClassesWithProbByOriginalIdx.add(Pair.of(i, prediction));
      if (prediction.first.equals(classes.get(i))) {
        countOfPositives++;
      }
    }

    predictedClassesWithProbByOriginalIdx.sort((p1, p2) -> p2.second.second.compareTo(p1.second.second));

    int passedPositives = 0;
    for (int i = 0; i < size; i++) {
      var originalClassIdx = predictedClassesWithProbByOriginalIdx.get(i).first;
      if (predictedClassesWithProbByOriginalIdx.get(i).second.first.equals(classes.get(originalClassIdx))) {
        passedPositives++;
        x.add(x.getLast());
        double step = 1. / (double) countOfPositives;
        y.add(passedPositives * step);
      } else {
        double step = 1 / (double) (size - countOfPositives);
        x.add((i + 1 - passedPositives) * step);
        y.add(y.getLast());
      }
    }

    XYSeries upperFunc = chart.addSeries("curve", x, y);
    upperFunc.setMarker(SeriesMarkers.NONE);
    var swingWrapper = new SwingWrapper<>(chart);
    swingWrapper.displayChart();
  }

  private static void drawAucPr(Tree tree, Set<Integer> allowedColumnIndexes, List<String> classes, List<? extends List<String>> attrValues) {
    int size = classes.size();
    var x = new LinkedList<Double>();
    var y = new LinkedList<Double>();
    for (double necessaryToBaseClassProbability = 0.; necessaryToBaseClassProbability <= 1.; necessaryToBaseClassProbability += 0.05) {
      var baseClass = classes.stream().distinct().min(String::compareTo).get();

      var good_prediction = new HashMap<String, Integer>();
      var bad_prediction = new HashMap<String, Integer>();
      var missed = new HashMap<String, Integer>();

      classes.stream().distinct().forEach(c -> {
        good_prediction.put(c, 0);
        bad_prediction.put(c, 0);
        missed.put(c, 0);
      });

      tree = tree.rebuildToNew(allowedColumnIndexes, classes, attrValues, necessaryToBaseClassProbability);
      for (int i = 0; i < size; i++) {
        var prediction = tree.predict(attrValues.get(i));
        var predictedClass = prediction.first;
        var trueClass = classes.get(i);

        if (trueClass.equals(predictedClass)) {
          int prevValue = good_prediction.getOrDefault(trueClass, 0);
          prevValue++;
          good_prediction.put(trueClass, prevValue);
        } else {
          int prevValue = bad_prediction.get(trueClass);
          prevValue++;
          bad_prediction.put(trueClass, prevValue);
          prevValue = missed.get(trueClass);
          prevValue++;
          missed.put(predictedClass, prevValue);
        }
      }

      double TP = good_prediction.get(baseClass);
      double precision = TP / (TP + missed.get(baseClass));
      double recall = TP / (TP + bad_prediction.get(baseClass));
//      System.out.println(recall);
//      System.out.println(precision);
//      System.out.println();
      x.add(recall);
      y.add(precision);

      System.out.println(recall);
    }

    XYChart chart = QuickChart.getChart(
      "AUC PR", "Recall", "Precision", "stub", List.of(0., 1.), List.of(0., 1.)
    );
    var stubSeries = chart.updateXYSeries("stub", List.of(0., 1.), List.of(0., 1.), null);
    stubSeries.setShowInLegend(false);
    stubSeries.setLineStyle(SeriesLines.NONE);
    var series = chart.addSeries("curve", x, y);
    series.setMarker(SeriesMarkers.NONE);
    var swingWrapper = new SwingWrapper<>(chart);
    swingWrapper.displayChart();
  }


  private static void writeTreeToFile(Tree tree) throws IOException {
    var treeJsonFile = new File(JSON_FILE_PATH);
    if (!treeJsonFile.exists() && !treeJsonFile.createNewFile()) {
      throw new IllegalStateException("Can't create file to write json");
    }
    var treeWriter = new FileWriter(treeJsonFile);
    treeWriter.write(tree.toJson());
    treeWriter.flush();
    treeWriter.close();
  }

  private static ArrayList<ArrayList<String>> getDataWithoutClass(List<? extends List<String>> data) {
    return data.stream()
      .map(values -> {
        var result = new ArrayList<String>(values.size() - 1);
        for (int i = 0; i < values.size(); i++) {
          if (i == CLASS_COLUMN_INDEX)
            continue;
          result.add(values.get(i));
        }
        return result;
      })
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private static ArrayList<ArrayList<String>> readCsv(String fileName, int countOfLines) throws FileNotFoundException {
    var scanner = new Scanner(new FileReader(fileName));

    var attrValues = scanner.nextLine().split(",");
    int columnsSize = attrValues.length;

    var result = new ArrayList<ArrayList<String>>(countOfLines);
    result.add(new ArrayList<>(columnsSize));
    for (String attrValue : attrValues) {
      result.get(0).add(attrValue);
    }

    for (int i = 1; i < countOfLines; i++) {
      attrValues = scanner.nextLine().split(",");
      if (columnsSize != attrValues.length) {
        throw new IllegalArgumentException("Expect $columnsSize, but get ${currentAttrs.size}");
      }
      var currValues = new ArrayList<String>(columnsSize);
      currValues.addAll(Arrays.asList(attrValues));
      result.add(currValues);
    }

    return result;
  }

  private static final List<String> columnNames = List.of(
    "class",
    "cap-shape",
    "cap-surface",
    "cap-color",
    "bruises",
    "odor",
    "gill-attachment",
    "gill-spacing",
    "gill-size",
    "gill-color",
    "stalk-shape",
    "stalk-root",
    "stalk-surface-above-ring",
    "stalk-surface-below-ring",
    "stalk-color-above-ring",
    "stalk-color-below-ring",
    "veil-type",
    "veil-color",
    "ring-number",
    "ring-type",
    "spore-print-color",
    "population",
    "habitat");

  private static final Map<String, Integer> columnIndexesByNames = IntStream.range(1, columnNames.size()).boxed()
    .collect(Collectors.toMap(columnNames::get, Function.identity()));
}
