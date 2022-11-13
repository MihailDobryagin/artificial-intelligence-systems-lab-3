package com.company.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class LocalCollectors {
  public static <T> Collector<T, ?, ArrayList<T>> toArrayList(int capacity) {
    return new Collector<T, ArrayList<T>, ArrayList<T>>() {
      @Override
      public Supplier<ArrayList<T>> supplier() {
        return () -> new ArrayList<>(capacity);
      }

      @Override
      public BiConsumer<ArrayList<T>, T> accumulator() {
        return ArrayList::add;
      }

      @Override
      public Function<ArrayList<T>, ArrayList<T>> finisher() {
        return Function.identity();
      }

      @Override
      public BinaryOperator<ArrayList<T>> combiner() {
        return (left, right) -> {
          left.addAll(right);
          return left;
        };
      }

      @Override
      public Set<Characteristics> characteristics() {
        return Collections.emptySet();
      }
    };
  }
}
