package dk.brics.tajs.solver.refinement;

import forwards_backwards_api.Assumption;
import forwards_backwards_api.ProgramLocation;

import java.util.Map;
import java.util.Set;

public interface QueryResult<T> {
    /**
     * Returns the query result.
     */
    T getResult();

    /**
     * Returns the assumptions made solving the query.
     */
    Map<ProgramLocation, Set<Assumption>> getAssumptions();

}
