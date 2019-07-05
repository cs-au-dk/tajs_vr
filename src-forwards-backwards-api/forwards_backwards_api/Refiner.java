package forwards_backwards_api;

import dk.brics.tajs.lattice.Value;
import forwards_backwards_api.memory.Register;
import forwards_backwards_api.memory.MemoryLocation;

/**
 * Query interface a refuter should implement.
 */
public interface Refiner<T extends Formula> {

    /**
     * Refines the designated abstract value, given some constraints.
     */
    RefineResult refine(ProgramLocation location, MemoryLocation mem, T constraint, Forwards forwards);

    /**
     * Decides if a query is feasible at a location.
     */
    RefutationResult refute(ProgramLocation location, T formula, Forwards forwards);

    /**
     * Creates an equality constraint.
     */
    T mkEqualityConstraint(MemoryLocation mem, Value value);

    T mkInequalityConstraint(MemoryLocation mem, Value value); // TODO: remove if unused?

    T mkConjunctionConstraint(T f1, T f2);

    T mkTrueConstraint();

    /**
     * Decides if a previously made assumption still holds.
     */
    boolean assumptionHolds(ProgramLocation location, Assumption a, Forwards forwards);
}
