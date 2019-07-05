package dk.brics.tajs.solver.refinement;

import forwards_backwards_api.Assumption;
import forwards_backwards_api.Formula;
import forwards_backwards_api.Forwards;
import forwards_backwards_api.ProgramLocation;
import forwards_backwards_api.Refiner;

public class AssumptionChecker {
    private final Refiner<? extends Formula> refiner;
    private final Forwards forwards;

    public AssumptionChecker(Refiner<? extends Formula> refiner, Forwards forwards) {
        this.refiner = refiner;
        this.forwards = forwards;
    }

    public boolean assumptionHolds(ProgramLocation location, Assumption value) {
        return refiner.assumptionHolds(location, value, forwards);
    }
}
