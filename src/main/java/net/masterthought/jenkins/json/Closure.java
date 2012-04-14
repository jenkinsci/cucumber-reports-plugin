package net.masterthought.jenkins.json;

public interface Closure<R, T> {
  public Util.Status call(T t);
}
