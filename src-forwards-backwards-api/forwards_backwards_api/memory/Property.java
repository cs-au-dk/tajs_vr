package forwards_backwards_api.memory;

import dk.brics.tajs.lattice.ObjectLabel;
import dk.brics.tajs.lattice.Value;

/**
 * A property memory location.
 * NB: this does make use of prototype chains.
 */
public class Property implements MemoryLocation {

    private final ObjectLabel object;

    private final Value name;

    public Property(ObjectLabel object, Value name) {
        this.object = object;
        this.name = name;
    }

    @Override
    public String toString() {
        return "PROP(" + object + ", " + name + ")";
    }

    public Value getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Property property = (Property) o;

        if (object != null ? !object.equals(property.object) : property.object != null) return false;
        return name != null ? name.equals(property.name) : property.name == null;
    }

    @Override
    public int hashCode() {
        int result = object != null ? object.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public ObjectLabel getObject() {

        return object;
    }

    @Override
    public <T> T accept(MemoryLocationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
