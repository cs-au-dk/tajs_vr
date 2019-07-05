package dk.brics.tajs.solver.refinement;

import forwards_backwards_api.ProgramLocation;
import forwards_backwards_api.Query;

interface QuerySolver<QueryResultType extends QueryResult<QueryResultValueType>, QueryResultValueType, QueryType extends Query<QueryResultType>> {
    QueryResultType solve(QueryType query, ProgramLocation location);
}
