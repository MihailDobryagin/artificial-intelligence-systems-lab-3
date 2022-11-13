package com.company.tree;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Tree {
  final List<Node> nodes;

  private Tree() {
    this.nodes = new LinkedList<>();
  }

  public static Tree makeTree(List<String> columnNames, List<String> classes, List<? extends List<String>> attrValues) {
    return makeTree(
      columnNames,
      IntStream.range(0, columnNames.size()).boxed().collect(Collectors.toSet()),
      classes,
      attrValues
    );
  }

  public static Tree makeTree(
    List<String> columnNames,
    Set<Integer> allowedColumnIndexes,
    List<String> classes,
    List<? extends List<String>> attrValues
  ) {
    var tree = new Tree();
    var currentNodes = makeNodes(null, allowedColumnIndexes, classes, attrValues);
    tree.addNodes(currentNodes);
    return tree;
  }

  static List<Node> makeNodes(
    Node parentNode,
    Set<Integer> allowedColumnIndexes,
    List<String> classes,
    List<? extends List<String>> attrValues
  ) {
    return makeNodes(parentNode, allowedColumnIndexes, classes, attrValues);
  }

  private void addNodes(Collection<Node> nodes) {
    this.nodes.addAll(nodes);
  }
}
