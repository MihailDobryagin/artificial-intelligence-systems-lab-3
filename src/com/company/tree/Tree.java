package com.company.tree;

import com.company.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class Tree {
  private final List<Node> rootNodes;
  private final List<String> columnNames;

  public Tree(List<String> columnNames, List<? extends List<String>> attrValues) {
    this.rootNodes = new LinkedList<>();
    this.columnNames = columnNames;
  }

  public Tree rebuildToNew(
    Set<Integer> allowedColumnIndexes,
    List<String> classes,
    List<? extends List<String>> attrValues,
    double probabilityToBaseClass
  ) {
    return makeTree(columnNames, allowedColumnIndexes, classes, attrValues, probabilityToBaseClass);
  }

  public static Tree makeTree(
    List<String> columnNames,
    Set<Integer> allowedColumnIndexes,
    List<String> classes,
    List<? extends List<String>> attrValues,
    double probabilityToBaseClass
  ) {
    var tree = new Tree(columnNames, attrValues);
    tree.rootNodes.addAll(createChildrenFromParent(null, allowedColumnIndexes, classes, attrValues, probabilityToBaseClass));
    return tree;
  }

  public String toJson() {
    Queue<Node> nodesQueue = new LinkedList<>(rootNodes);
    var builder = new StringBuilder();
    builder.append("[");

    while (!nodesQueue.isEmpty()) {
      var parent = nodesQueue.poll();
      builder.append(nodeToJson(parent)).append(",");
    }
    builder.append("]");
    return builder.toString();
  }

  public Pair<String, Double> predict(List<String> attrValue) {
    List<Node> nodes = rootNodes;
    String resultClass = null;
    Double probability = null;

    while (resultClass == null && !nodes.isEmpty()) {
      var matchedNode = nodes.stream()
        .filter(node -> attrValue.get(node.columnIdx).equals(node.columnValue))
        .findFirst().get();

      if (matchedNode.children.isEmpty()) {
        resultClass = matchedNode.prevailingClassValue;
        probability = matchedNode.classProbability;
      }
      nodes = matchedNode.children;
    }

    return Pair.of(resultClass, probability);
  }

  public String predictClass(List<String> attrValue) {
    return predict(attrValue).first;
  }

  private static List<Node> createChildrenFromParent(
    Node parentNode,
    Set<Integer> allowedColumnIndexes,
    List<String> classes,
    List<? extends List<String>> attrValues,
    double probabilityToBaseClass
  ) {
    List<Node> children;

    if (parentNode != null) {
      var allowedColumnIndexesWithoutParentNodeTargetIndex = new HashSet<>(allowedColumnIndexes);
      allowedColumnIndexesWithoutParentNodeTargetIndex.remove(parentNode.columnIdx);
      var cuttedByParentClasses = new LinkedList<String>();
      var cuttedByParentAttrValues = new LinkedList<List<String>>();
      for (int i = 0; i < classes.size(); i++) {
        if (attrValues.get(i).get(parentNode.columnIdx).equals(parentNode.columnValue)) {
          cuttedByParentClasses.add(classes.get(i));
          cuttedByParentAttrValues.add(attrValues.get(i));
        }
      }
      children = allowedColumnIndexesWithoutParentNodeTargetIndex.isEmpty()
        ? Collections.emptyList()
        : Node.makeNodes(
        parentNode, allowedColumnIndexesWithoutParentNodeTargetIndex, cuttedByParentClasses, cuttedByParentAttrValues, probabilityToBaseClass
      );

      parentNode.addChildren(children);
      children.forEach(child -> createChildrenFromParent(child, allowedColumnIndexesWithoutParentNodeTargetIndex, cuttedByParentClasses, cuttedByParentAttrValues, probabilityToBaseClass));
    } else {
      children = allowedColumnIndexes.isEmpty()
        ? Collections.emptyList()
        : Node.makeNodes(null, allowedColumnIndexes, classes, attrValues, probabilityToBaseClass);
      children.forEach(child -> createChildrenFromParent(child, allowedColumnIndexes, classes, attrValues, probabilityToBaseClass));
    }
    return children;
  }

  private StringBuilder nodeToJson(Node node) {
    var childrenJson = node.children.stream().map(this::nodeToJson).collect(Collectors.joining(","));
    return new StringBuilder()
      .append("{")
      .append("\"column-index\"").append(":").append("\"").append(node.columnIdx).append("\",")
      .append("\"column-name\"").append(":").append("\"").append(columnNames.get(node.columnIdx)).append("\",")
      .append("\"prevailing-class\"").append(":").append("\"").append(node.prevailingClassValue).append("\",")
      .append("\"children\"").append(":").append("[").append(childrenJson).append("]")
      .append("}");
  }
}
