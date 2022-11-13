package com.company;

import com.company.utils.LocalCollectors;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

  private static final String DATASET_PATH =
    "C:\\Users\\Михаил\\Desktop\\Университет\\3 курс\\CИИ\\Лабораторные\\3\\Project\\resources\\dataset.csv";
  private static final int COUNT_OF_LINES = 8124;
  private static final int CLASS_COLUMN_INDEX = 0;

  public static void main(String[] args) throws FileNotFoundException {
    var dataWithClass = readCsv(DATASET_PATH, COUNT_OF_LINES);
    List<String> classes = dataWithClass.stream()
      .map(l -> l.get(CLASS_COLUMN_INDEX))
      .collect(LocalCollectors.toArrayList(dataWithClass.size()));

    var dataWithoutClass = getDataWithoutClass(dataWithClass);

    var tree = Tree.makeTree(columnNames.subList(1, columnNames.size()), classes, dataWithoutClass);
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
      .collect(LocalCollectors.toArrayList(data.size()));
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
}
