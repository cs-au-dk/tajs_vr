package forwards_backwards_api;

import dk.brics.tajs.solver.refinement.QueryResult;

public interface Query<T extends QueryResult> {

    T getSoundDefault();

}
