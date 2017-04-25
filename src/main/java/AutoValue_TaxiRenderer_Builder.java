//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

final class AutoValue_TaxiRenderer_Builder extends TaxiRenderer.Builder {
    private final TaxiRenderer.Language language;

    AutoValue_TaxiRenderer_Builder(TaxiRenderer.Language language) {
        if(language == null) {
            throw new NullPointerException("Null language");
        } else {
            this.language = language;
        }
    }

    TaxiRenderer.Language language() {
        return this.language;
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(o instanceof TaxiRenderer.Builder) {
            TaxiRenderer.Builder that = (TaxiRenderer.Builder)o;
            return this.language.equals(that.language());
        } else {
            return false;
        }
    }

    public int hashCode() {
        int h = 1 * 1000003;
        h ^= this.language.hashCode();
        return h;
    }
}
