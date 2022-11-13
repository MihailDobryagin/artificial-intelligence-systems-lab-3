package com.company.tree;

import com.company.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class Node {
  final Node parentNode;
  final int columnIdx;
  final String columnValue;

  private Node(Node parentNode, int columnIdx, String columnValue) {
    this.parentNode = parentNode;
    this.columnIdx = columnIdx;
    this.columnValue = columnValue;
  }

  static List<Node> makeNodes(Node parentNode, Set<Integer> allowedColumnIndexes, List<String> classes, List<? extends List<String>> attrValues) {
    if (allowedColumnIndexes.isEmpty()) {
      throw new IllegalArgumentException("Allowed columns must not be empty");
    }

    Pair<Integer, Double> maxGainRatio = null;

    for (int columnIdx : allowedColumnIndexes) {
      var gainRation = calcGainRatio(columnIdx, classes, attrValues);
      System.out.printf("%5d | %f\n", columnIdx, gainRation);
      if (maxGainRatio == null || maxGainRatio.second < gainRation) {
        maxGainRatio = Pair.of(columnIdx, gainRation);
      }
    }

    System.out.println("------------------------------");
    System.out.println(maxGainRatio.first);

    var targetAttrValues = new HashSet<String>();

    for (List<String> localAttrValues : attrValues) {
      targetAttrValues.add(localAttrValues.get(maxGainRatio.first));
    }

    Pair<Integer, Double> finalMaxGainRatio = maxGainRatio;
    return targetAttrValues.stream()
      .map(value -> new Node(parentNode, finalMaxGainRatio.first, value))
      .collect(Collectors.toList());
  }

  private static double calcGainRatio(int targetAttrIndex, List<String> classes, List<? extends List<String>> attrValues) {
    double info = calcInfo(classes, attrValues);
    double targetInfo = calcInfoByAttr(targetAttrIndex, classes, attrValues);
    double splitInfo = calcSplitInfoByAttr(targetAttrIndex, attrValues);

    return (info - targetInfo) / splitInfo;
  }

  private static double calcInfoByAttr(
    int targetAttrIndex, List<String> classes, List<? extends List<String>> attrValues
  ) {
    if (classes.size() != attrValues.size()) {
      var message = "Size of classes is " + classes.size() + ". " +
        "But size of attrValues is " + attrValues.size();
      throw new IllegalArgumentException(message);
    }
    int size = classes.size();
    var attrsByTargetAttrValue = new HashMap<String, List<List<String>>>();
    var classesByTargetAttrValue = new HashMap<String, List<String>>();

    for (int i = 0; i < size; i++) {
      String targetAttrValue = attrValues.get(i).get(targetAttrIndex);
      var curAttrValues = attrValues.get(i);
      var curClass = classes.get(i);

      var attrsInMap = attrsByTargetAttrValue.get(targetAttrValue);
      var classesInMap = classesByTargetAttrValue.get(targetAttrValue);

      if (classesInMap == null) {
        classesInMap = new LinkedList<>();
        attrsInMap = new LinkedList<>();
        classesByTargetAttrValue.put(targetAttrValue, classesInMap);
        attrsByTargetAttrValue.put(targetAttrValue, attrsInMap);
      }

      classesInMap.add(curClass);
      attrsInMap.add(curAttrValues);
    }

    return classesByTargetAttrValue.entrySet().stream()
      .mapToDouble(entry -> {
        var targetAttrValue = entry.getKey();
        var curClasses = entry.getValue();
        var curAttrs = attrsByTargetAttrValue.get(targetAttrValue);

        return ((double) curClasses.size()) / ((double) size) * calcInfo(curClasses, curAttrs);
      })
      .sum();
  }

  private static double calcSplitInfoByAttr(
    int targetAttrIndex, List<? extends List<String>> attrsValues
  ) {
    int size = attrsValues.size();

    var frequenciesOfTargetTypeValues = new HashMap<String, Integer>();

    for (List<String> attr : attrsValues) {
      String targetAttrValue = attr.get(targetAttrIndex);
      int count = frequenciesOfTargetTypeValues.getOrDefault(targetAttrValue, 0);
      count++;
      frequenciesOfTargetTypeValues.put(targetAttrValue, count);
    }

    double result = frequenciesOfTargetTypeValues.values().stream()
      .mapToDouble(count -> {
        double quotient = ((double) count) / ((double) size);
        return quotient * log2(quotient);
      })
      .sum();

    return -result;
  }

  private static double calcInfo(List<String> classes, List<? extends List<String>> attrs) {
    int size = attrs.size();
    var frequencies = new HashMap<String, Integer>();

    for (int i = 0; i < size; i++) {
      var classValue = classes.get(i);
      Integer prevResult = frequencies.getOrDefault(classValue, 0);
      prevResult++;
      frequencies.put(classValue, prevResult);
    }

    double result = frequencies.values().stream()
      .mapToDouble(count -> ((double) count) / ((double) size) * log2(count))
      .sum();
    return -result;
  }


  private static double log2(double x) {
    double r = 1;
    double l = 0;
    while (Math.pow(2., r) < x) {
      r++;
    }

    for (int i = 0; i < 100; i++) {
      double med = (l + r) / 2.;
      if (Math.pow(2., med) < x) {
        l = med;
      } else {
        r = med;
      }
    }

    return (l + r) / 2.;
  }
}
