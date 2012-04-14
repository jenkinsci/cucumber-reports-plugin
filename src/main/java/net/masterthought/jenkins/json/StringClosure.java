package net.masterthought.jenkins.json;

public interface StringClosure<R, T> {
  public R call(T t);
}

