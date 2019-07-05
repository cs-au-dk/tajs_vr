package dk.brics.tajs.solver.refinement;

import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.util.Collections;
import forwards_backwards_api.Formula;
import forwards_backwards_api.Query;
import forwards_backwards_api.RefineResult;
import forwards_backwards_api.memory.MemoryLocation;

import static dk.brics.tajs.util.Collections.newMap;

public class RefineQuery<T extends Formula> implements Query<RefineResult> {

    private MemoryLocation memoryLocation;
    private T formula;
    private Value soundDefault;

    public RefineQuery(MemoryLocation memoryLocation, T formula, Value soundDefault) {
        this.memoryLocation = memoryLocation;
        this.formula = formula;

        this.soundDefault = soundDefault;
    }

    public MemoryLocation getMemoryLocation() {
        return memoryLocation;
    }

    public T getFormula() {
        return formula;
    }

    @Override
    public RefineResult getSoundDefault() {
        return new RefineResult(Collections.singleton(soundDefault), newMap());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefineQuery<?> that = (RefineQuery<?>) o;

        if (memoryLocation != null ? !memoryLocation.equals(that.memoryLocation) : that.memoryLocation != null) return false;
        if (formula != null ? !formula.equals(that.formula) : that.formula != null) return false;
        return soundDefault != null ? soundDefault.equals(that.soundDefault) : that.soundDefault == null;

    }

    @Override
    public int hashCode() {
        int result = memoryLocation != null ? memoryLocation.hashCode() : 0;
        result = 31 * result + (formula != null ? formula.hashCode() : 0);
        result = 31 * result + (soundDefault != null ? soundDefault.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RefineQuery{" +
                "memoryLocation=" + memoryLocation +
                ", formula=" + formula +
                ", soundDefault=" + soundDefault +
                '}';
    }
}