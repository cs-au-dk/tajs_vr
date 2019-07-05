package forwards_backwards_api;

import java.util.List;
import java.util.Objects;

public class Properties {
    private final List<String> definiteProperties;
    private final List<String> maybeProperties;

    public Properties(List<String> definiteProperties, List<String> maybeProperties) {
        this.definiteProperties = definiteProperties;
        this.maybeProperties = maybeProperties;
    }

    public List<String> getDefiniteProperties() {
        return definiteProperties;
    }

    public List<String> getMaybeProperties() {
        return maybeProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Properties that = (Properties) o;
        return Objects.equals(definiteProperties, that.definiteProperties) &&
                Objects.equals(maybeProperties, that.maybeProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definiteProperties, maybeProperties);
    }
}
