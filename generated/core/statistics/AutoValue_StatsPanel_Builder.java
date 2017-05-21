
package core.statistics;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
 final class AutoValue_StatsPanel_Builder extends StatsPanel.Builder {

  private final StatsTracker st;

  AutoValue_StatsPanel_Builder(
      StatsTracker st) {
    if (st == null) {
      throw new NullPointerException("Null st");
    }
    this.st = st;
  }

  @Override
  StatsTracker st() {
    return st;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof StatsPanel.Builder) {
      StatsPanel.Builder that = (StatsPanel.Builder) o;
      return (this.st.equals(that.st()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= this.st.hashCode();
    return h;
  }

}
