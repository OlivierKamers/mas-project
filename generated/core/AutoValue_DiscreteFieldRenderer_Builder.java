
package core;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
 final class AutoValue_DiscreteFieldRenderer_Builder extends DiscreteFieldRenderer.Builder {

  private final DiscreteField df;

  AutoValue_DiscreteFieldRenderer_Builder(
      DiscreteField df) {
    if (df == null) {
      throw new NullPointerException("Null df");
    }
    this.df = df;
  }

  @Override
  DiscreteField df() {
    return df;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof DiscreteFieldRenderer.Builder) {
      DiscreteFieldRenderer.Builder that = (DiscreteFieldRenderer.Builder) o;
      return (this.df.equals(that.df()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= this.df.hashCode();
    return h;
  }

}
