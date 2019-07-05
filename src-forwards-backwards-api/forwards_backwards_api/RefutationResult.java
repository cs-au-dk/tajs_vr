package forwards_backwards_api;

import dk.brics.tajs.solver.refinement.QueryResult;

import java.util.Map;
import java.util.Set;

/**
 * The result of an attempted refutation of a formula.
 */
public class RefutationResult implements QueryResult<Boolean> {

    /**
     * True iff the formula was refuted.
     * <p>
     * NB: It is sound to let this be false.
     */
    private final boolean refuted;

    /**
     * The assumptions made during the refutation.
     */
    private final Map<ProgramLocation, Set<Assumption>> assumptions;

    public RefutationResult(boolean refuted, Map<ProgramLocation, Set<Assumption>> assumptions) {
        this.refuted = refuted;
        this.assumptions = assumptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefutationResult that = (RefutationResult) o;

        if (refuted != that.refuted) return false;
        return assumptions != null ? assumptions.equals(that.assumptions) : that.assumptions == null;
    }

    @Override
    public int hashCode() {
        int result = (refuted ? 1 : 0);
        result = 31 * result + (assumptions != null ? assumptions.hashCode() : 0);
        return result;
    }

    public Boolean getResult() {
        return refuted;
    }

    public Map<ProgramLocation, Set<Assumption>> getAssumptions() {
        return assumptions;
    }
}
