package forwards_backwards_api;

import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.solver.refinement.QueryResult;
import forwards_backwards_api.memory.MemoryLocation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Result from {@link Refiner#refine(ProgramLocation, MemoryLocation, Formula, Forwards)}.
 */
public class RefineResult implements QueryResult<Collection<Value>> {

    /**
     * Refined value.
     */
    private final Collection<Value> refined;

    /**
     * Assumptions made by the refiner.
     */
    private final Map<ProgramLocation, Set<Assumption>> assumptions;

    /**
     * Constructs a new RefineResult.
     */
    public RefineResult(Collection<Value> refined, Map<ProgramLocation, Set<Assumption>> assumptions) {
        this.refined = refined;
        this.assumptions = assumptions;
    }

    @Override
    public String toString() {
        return "RefineResult{" +
                "refined=" + refined +
                ", assumptions=" + assumptions +
                '}';
    }

    /**
     * Returns the refined value as a collection of new values.
     */
    public Collection<Value> getResult() {
        return refined;
    }

    /**
     * Returns the assumptions made by the refiner.
     */
    public Map<ProgramLocation, Set<Assumption>> getAssumptions() {
        return assumptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefineResult that = (RefineResult) o;

        if (refined != null ? !refined.equals(that.refined) : that.refined != null) return false;
        return assumptions != null ? assumptions.equals(that.assumptions) : that.assumptions == null;

    }

    @Override
    public int hashCode() {
        int result = refined != null ? refined.hashCode() : 0;
        result = 31 * result + (assumptions != null ? assumptions.hashCode() : 0);
        return result;
    }
}
