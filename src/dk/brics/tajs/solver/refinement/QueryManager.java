package dk.brics.tajs.solver.refinement;

import dk.brics.tajs.analysis.Solver;
import dk.brics.tajs.analysis.nativeobjects.concrete.SingleGamma;
import dk.brics.tajs.flowgraph.AbstractNode;
import dk.brics.tajs.lattice.Context;
import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.monitoring.IAnalysisMonitoring;
import dk.brics.tajs.monitoring.inspector.util.OccurenceCountingMap;
import dk.brics.tajs.monitoring.refinement.RefinerStatistics;
import dk.brics.tajs.monitoring.refinement.RefinerStatisticsAllTests;
import dk.brics.tajs.refinement.instantiations.forwards_backwards.ForwardsCache;
import dk.brics.tajs.refinement.instantiations.forwards_backwards.RefinerOptions;
import dk.brics.tajs.refinement.instantiations.forwards_backwards.TAJSForwardsAPI;
import dk.brics.tajs.solver.BlockAndContext;
import forwards_backwards_api.Assumption;
import forwards_backwards_api.Formula;
import forwards_backwards_api.Forwards;
import forwards_backwards_api.ProgramLocation;
import forwards_backwards_api.Query;
import forwards_backwards_api.RefineResult;
import forwards_backwards_api.Refiner;
import forwards_backwards_api.RefutationResult;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.brics.tajs.util.Collections.addAllToMapSet;
import static dk.brics.tajs.util.Collections.addToMapMap;
import static dk.brics.tajs.util.Collections.addToMapSet;
import static dk.brics.tajs.util.Collections.newMap;
import static dk.brics.tajs.util.Collections.newSet;

public class QueryManager<QueryResultType extends QueryResult<QueryResultValueType>, QueryResultValueType, QueryType extends Query<QueryResultType>> {

    private final AssumptionChecker checker;

    private Logger log = Logger.getLogger(QueryManager.class);

    private QuerySolver<QueryResultType, QueryResultValueType, QueryType> querySolver;

    private IAnalysisMonitoring monitoring;

    private Map<ProgramLocation, Map<QueryType, QueryResultType>> queryResults = newMap();

    private Map<ProgramLocation, Set<Assumption>> assumptionsAtLocation = newMap();

    private Map<Assumption, Set<ProgramLocation>> locationsThatUseAssumption = newMap();

    private Map<Assumption, Set<QueryType>> queriesThatUseAssumption = newMap();

    private boolean allowRefinements = true;

    private OccurenceCountingMap<ProgramLocation> failedQueries = new OccurenceCountingMap<>();

    private OccurenceCountingMap<ProgramLocation> successfulQueries = new OccurenceCountingMap<>();

    public QueryManager(AssumptionChecker checker, QuerySolver<QueryResultType, QueryResultValueType, QueryType> querySolver, IAnalysisMonitoring monitoring) {
        this.checker = checker;
        this.querySolver = querySolver;
        this.monitoring = monitoring;
    }

    public QueryResultValueType getQueryResult(ProgramLocation location, QueryType query) {
        if (monitoring!=null && !monitoring.allowNextIteration()) {
            return query.getSoundDefault().getResult();
        }

        if (RefinerOptions.get().isUseSoundDefaultForFailedQueriesLocations() && failedQueries.getResult(location) >= 2) {
            return query.getSoundDefault().getResult();
        }

        if (queryResults.getOrDefault(location, newMap()).containsKey(query)) {
            RefinerStatistics.get().registerRefinementQueryAnsweredByCache(location, query);
            return queryResults.get(location).get(query).getResult();
        }

        if(!allowRefinements) {
            if (log.isDebugEnabled())
                log.debug("Not queuing query at " + location + " to worklist " + query);
            return query.getSoundDefault().getResult();
        }

        QueryResultType result;
        try {
            result = querySolver.solve(query, location);
        } catch (Exception e) {
            if (RefinerOptions.get().isDisplayQueryDebuggingInformationEnabled()) {
                log.error("Exception from refiner: " + e + " at: " + location.getNode().getSourceLocation());
                log.error(" For query: " + query);
            }
            RefinerStatistics.get().registerExceptionFromRefiner();
            RefinerStatistics.get().endRefinementQueryToValueRefiner();
            if (RefinerStatisticsAllTests.isInitialized())
                RefinerStatisticsAllTests.get().queryTerminatedWithException(location, e);
            result = query.getSoundDefault();
        }
        if (RefinerStatisticsAllTests.isQuerySuccessful((Collection<Value>)result.getResult(), (Collection<Value>) query.getSoundDefault().getResult())) {
            successfulQueries.count(location);
        } else {
            failedQueries.count(location);
        }
        recordQueryResult(location, query, result);
        return result.getResult();
    }

    private void recordQueryResult(ProgramLocation location, QueryType query, QueryResultType result) {
        result.getAssumptions().entrySet().forEach(entry -> {
            addAllToMapSet(assumptionsAtLocation, entry.getKey(), entry.getValue());
            entry.getValue().forEach(assumption -> addToMapSet(locationsThatUseAssumption, assumption, location));
            entry.getValue().forEach(assumption -> addToMapSet(queriesThatUseAssumption, assumption, query));
        });
        addToMapMap(queryResults, location, query, result);
    }

    /**
     * Checks if previously made assumptions for a program point still holds.
     * <p>
     * If an assumption is found to no longer hold (i.e., it is unsound to rely on), its dependents will be re-added to the worklist.
     */
    public Set<BlockAndContext> checkAssumptions(AbstractNode n, Context context) {
        ProgramLocation location = new ProgramLocation(n, context);

        Set<Assumption> assumptionsToCheck = assumptionsAtLocation.getOrDefault(location, newSet());
        Set<Assumption> invalidatedAssumptions = assumptionsToCheck.stream()
                .filter(assumption -> !checker.assumptionHolds(location, assumption))
                .collect(Collectors.toSet());

        Set<ProgramLocation> locationsToProcess = newSet();
        Set<QueryType> invalidatedQueries = newSet();

        invalidatedAssumptions.forEach(assumption -> {
            locationsToProcess.addAll(locationsThatUseAssumption.getOrDefault(assumption, newSet()));
            invalidatedQueries.addAll(queriesThatUseAssumption.getOrDefault(assumption, newSet()));
            locationsThatUseAssumption.get(assumption).remove(location);
            queriesThatUseAssumption.get(assumption).remove(location);
        });

        assumptionsAtLocation.getOrDefault(location, newSet()).removeAll(invalidatedAssumptions);
        return locationsToProcess.stream().map(loc -> {
            invalidatedQueries.forEach( query -> queryResults.get(loc).remove(query));
            return new BlockAndContext(loc.getNode().getBlock(), loc.getContext());
        }).collect(Collectors.toSet());
    }

    /**
     * Sets whether refinements are allowed.
     */
    public void setAllowRefinements(boolean allowRefinements) {
        this.allowRefinements = allowRefinements;
    }

    public static class MultiQueryManager<FormulaType extends Formula> {

        private static MultiQueryManager<? extends Formula> instance;

        private Refiner<FormulaType> refiner;

        private QueryManager<RefutationResult, Boolean, RefuteQuery<FormulaType>> refuterInstance;

        private QueryManager<RefineResult, Collection<Value>, RefineQuery<FormulaType>> refinerInstance;

        public MultiQueryManager(Refiner<FormulaType> refiner, QueryManager<RefutationResult, Boolean, RefuteQuery<FormulaType>> refuterInstance, QueryManager<RefineResult, Collection<Value>, RefineQuery<FormulaType>> refinerInstance) {
            this.refiner = refiner;
            this.refuterInstance = refuterInstance;
            this.refinerInstance = refinerInstance;
        }

        private static RefineResult meetBetweenRefineResults(RefineResult soundDefault, RefineResult refineResult, Forwards forwards) {
            Collection<Value> soundValues = soundDefault.getResult();
            Collection<Value> refinedValues = refineResult.getResult();

            Set<Value> intersectedValues = newSet();
            for(Value refinedValue : refinedValues) {
                Value acc = Value.makeNone();
                for(Value soundValue : soundValues) {
                    acc = acc.join(refinedValue.restrictToStrictEquals(soundValue));
                }
                intersectedValues.add(acc);
            }

            return new RefineResult(intersectedValues, refineResult.getAssumptions());
        }

        public static <FormulaType extends Formula> void init(Refiner<FormulaType> refiner, Forwards forwards, IAnalysisMonitoring monitoring) {
            ForwardsCache forwardsWithCache = new ForwardsCache(forwards);
            Solver.SolverInterface c = ((TAJSForwardsAPI) forwards).getSolverInterface(); //TODO: Requires TAJSForwardsAPI
            QuerySolver<RefineResult, Collection<Value>, RefineQuery<FormulaType>> refinerSolver = (query, location) -> {
                if(c.getFlowGraph().isHostEnvironmentSource(location.getNode().getSourceLocation()) || location.getNode().getSourceLocation().getLocation() == null) {
                    return query.getSoundDefault();
                }

                if (!c.getMonitoring().allowNextIteration()) {
                    return query.getSoundDefault();
                }

                if (RefinerOptions.get().isDoNotRefinePreciseValues() && query.getSoundDefault().getResult().size() == 1) {
                    Value soundDefault = query.getSoundDefault().getResult().iterator().next();
                    boolean isConcrete = SingleGamma.isConcreteValue(query.getSoundDefault().getResult().iterator().next(), c);

                    if (isConcrete || (soundDefault.typeSize() == 1 && soundDefault.getObjectLabels().size() == 1)) {
                        return query.getSoundDefault();
                    }
                }

                boolean queryDebugging = RefinerOptions.get().isDisplayQueryDebuggingInformationEnabled();
                if (queryDebugging) {
                    System.out.println("Query at: " + location.getNode().getSourceLocation());
                    System.out.println("Query: " + query);
                }

                forwardsWithCache.resetCaches();
                RefinerStatistics.get().beginRefinementQueryToValueRefiner(location, (RefineQuery<Formula>) query);
                if (RefinerStatisticsAllTests.isInitialized())
                    RefinerStatisticsAllTests.get().initQuery(location, query);

                RefineResult refine = refiner.refine(location, query.getMemoryLocation(), query.getFormula(), forwardsWithCache);
                RefinerStatistics.get().endRefinementQueryToValueRefiner();
                RefineResult intersect = meetBetweenRefineResults(query.getSoundDefault(), refine, forwards);
                if (RefinerStatisticsAllTests.isInitialized())
                    RefinerStatisticsAllTests.get().queryFinished(location, intersect);
                if (queryDebugging) {
                    System.out.println("Result from refiner: " + refine.getResult());
                    System.out.println("Result intersected with forwards: " + intersect.getResult());
                }

                if (RefinerOptions.get().isDisableRefineToBottom() && Value.join(intersect.getResult()).isNone()) {
                    return query.getSoundDefault();
                }
                return intersect;
            };
            QueryManager<RefineResult, Collection<Value>, RefineQuery<FormulaType>> refinerInstance = new QueryManager(new AssumptionChecker(refiner, forwards), refinerSolver, monitoring);
            QuerySolver<RefutationResult, Boolean, RefuteQuery<FormulaType>> refuterSolver = (query, location) -> {
                if (c.getFlowGraph().isHostEnvironmentSource(location.getNode().getSourceLocation()) || location.getNode().getSourceLocation().getLocation() == null) {
                    return query.getSoundDefault();
                }
                forwardsWithCache.resetCaches();
                return refiner.refute(location, query.getFormula(), forwardsWithCache);
            };
            QueryManager<RefutationResult, Boolean, RefuteQuery<FormulaType>> refuterInstance = new QueryManager(new AssumptionChecker(refiner, forwards), refuterSolver, monitoring);
            instance = new MultiQueryManager<>(refiner, refuterInstance, refinerInstance);
        }

        public static <T extends Formula> MultiQueryManager<T> getInstance() { //XXX: programmer convenience, generic unsoundness
            return (MultiQueryManager<T>) instance;
        }

        public static void reset() {
            instance = null;
        }

        public Refiner<FormulaType> getRefiner() {
            return refiner;
        }

        public QueryManager<RefutationResult, Boolean, RefuteQuery<FormulaType>> getRefuterManager() {
            return refuterInstance;
        }

        public QueryManager<RefineResult, Collection<Value>, RefineQuery<FormulaType>> getRefinerManager() {
            return refinerInstance;
        }

        public Set<BlockAndContext> checkAssumptions(AbstractNode n, Context context) {
            RefinerStatistics.get().beginCheckAssumptions();
            Set<BlockAndContext> allInvalidations = newSet();
            allInvalidations.addAll(refinerInstance.checkAssumptions(n, context));
            allInvalidations.addAll(refuterInstance.checkAssumptions(n, context));
            RefinerStatistics.get().endCheckAssumptions();
            return allInvalidations;
        }

        public void setAllowRefinements(boolean allowRefinements) {
            refuterInstance.setAllowRefinements(allowRefinements);
            refinerInstance.setAllowRefinements(allowRefinements);
        }
    }
}
