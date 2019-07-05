package dk.brics.tajs.lattice;

import forwards_backwards_api.ProgramLocation;
import forwards_backwards_api.memory.MemoryLocation;

import java.util.Objects;
import java.util.Optional;

public class PropertyReadSpecialization {
    private final ProgramLocation programLocation;
    private final MemoryLocation memoryLocation;
    private final Optional<Value> propName;

    public PropertyReadSpecialization(ProgramLocation programLocation, MemoryLocation memoryLocation, Optional<Value> propName) {
        this.programLocation = programLocation;
        this.memoryLocation = memoryLocation;
        this.propName = propName;
    }

    public ProgramLocation getProgramLocation() {
        return programLocation;
    }

    public MemoryLocation getMemoryLocation() {
        return memoryLocation;
    }

    public Optional<Value> getPropName() {
        return propName;
    }

    @Override
    public String toString() {
        return "[" + programLocation.getNode().getSourceLocation() + ", " + memoryLocation + " --> " + propName + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyReadSpecialization that = (PropertyReadSpecialization) o;
        boolean equals = Objects.equals(propName, that.propName);
        return Objects.equals(programLocation, that.programLocation) &&
                Objects.equals(memoryLocation, that.memoryLocation) &&
                equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(programLocation, memoryLocation, propName);
    }
}
