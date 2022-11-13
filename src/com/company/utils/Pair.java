package com.company.utils;

public class Pair<T1, T2> {
  public T1 first;
  public T2 second;

  private Pair(T1 first, T2 second) {
    this.first = first;
    this.second = second;
  }

  public static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
    return new Pair<>(first, second);
  }

  public static  <T> Pair<T, T> ofSame(T value1, T value2) {
    return new Pair<>(value1, value2);
  }
}
