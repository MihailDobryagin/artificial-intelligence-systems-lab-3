package com.company.tree;

import com.company.utils.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Node {
  final Node parent;
  final List<Node> children;
  final int columnIdx;
  final String columnValue;
  final String prevailingClassValue;
  final double classProbability;

  private Node(Node parent, int columnIdx, String columnValue, String prevailingClassValue, double classProbability) {
    this.parent = parent;
    this.columnIdx = columnIdx;
    this.columnValue = columnValue;
    this.prevailingClassValue = prevailingClassValue;
    this.classProbability = classProbability;
    this.children = new LinkedList<>();
  }

  static List<Node> makeNodes(
    Node parentNode,
    Set<Integer> allowedColumnIndexes,
    List<String> classes,
    List<? extends List<String>> attrValues,
    double probabilityToBaseClass // necessaryProbabilityToBaseClass
  ) {
    if (allowedColumnIndexes.isEmpty()) {
      throw new IllegalArgumentException("Allowed columns must not be empty");
    }

    Pair<Integer, Double> maxGainRatio = null;

    for (int columnIdx : allowedColumnIndexes) {
      var gainRatio = calcGainRatio(columnIdx, classes, attrValues);
      if (maxGainRatio == null || maxGainRatio.second < gainRatio) {
        maxGainRatio = Pair.of(columnIdx, gainRatio);
      }
    }

    var targetAttrValues = new HashSet<String>();
    var classFrequenciesByTargetAttrValue = new HashMap<String, HashMap<String, Integer>>();

    for (int i = 0; i < attrValues.size(); i++) {
      var localAttrValues = attrValues.get(i);
      var targetAttrValue = localAttrValues.get(maxGainRatio.first);
      var curClass = classes.get(i);
      var curClassFrequenciesOfTargetAttrValue = classFrequenciesByTargetAttrValue
        .getOrDefault(targetAttrValue, new HashMap<>());
      var curClassCount = curClassFrequenciesOfTargetAttrValue.getOrDefault(curClass, 0);
      curClassCount++;
      curClassFrequenciesOfTargetAttrValue.put(curClass, curClassCount);
      classFrequenciesByTargetAttrValue.put(targetAttrValue, curClassFrequenciesOfTargetAttrValue);
      targetAttrValues.add(targetAttrValue);
    }

    Pair<Integer, Double> finalMaxGainRatio = maxGainRatio;
    return targetAttrValues.stream()
      .map(value -> {
        AtomicInteger countOfClasses = new AtomicInteger();
        var prevailingClassValueWithCount = classFrequenciesByTargetAttrValue
          .get(value).entrySet().stream()
          .peek(entry -> countOfClasses.addAndGet(entry.getValue()))
          .max(Map.Entry.comparingByValue())
          .get();

        var notPrevailingClassValueWithCount = classFrequenciesByTargetAttrValue
          .get(value).entrySet().stream()
          .peek(entry -> countOfClasses.addAndGet(entry.getValue()))
          .min(Map.Entry.comparingByValue())
          .get();

        var prevailingClassProbability = ((double) prevailingClassValueWithCount.getValue()) / ((double) countOfClasses.get());
        var notPrevailingClassProbability = ((double) notPrevailingClassValueWithCount.getValue()) / ((double) countOfClasses.get());
        // BaseClass -- class with MIN string-value

        if (prevailingClassValueWithCount.getKey().compareTo(notPrevailingClassValueWithCount.getKey()) > 0) {
          var tmpProbability = prevailingClassProbability;
          prevailingClassProbability = notPrevailingClassProbability;
          notPrevailingClassProbability = tmpProbability;
          var tmpClassValueWithCount = prevailingClassValueWithCount;
          prevailingClassValueWithCount = notPrevailingClassValueWithCount;
          notPrevailingClassValueWithCount = tmpClassValueWithCount;
        }

        if (prevailingClassProbability >= probabilityToBaseClass) {
          return new Node(
            parentNode, finalMaxGainRatio.first, value,
            prevailingClassValueWithCount.getKey(),
            prevailingClassProbability
          );
        } else {
          return new Node(
            parentNode, finalMaxGainRatio.first, value,
            notPrevailingClassValueWithCount.getKey(),
            notPrevailingClassProbability
          );
        }
      })
      .collect(Collectors.toList());
  }

  void addChildren(List<Node> children) {
    this.children.addAll(children);
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
