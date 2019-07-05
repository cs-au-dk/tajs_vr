package dk.brics.tajs.monitoring.refinement;

import dk.brics.tajs.flowgraph.AbstractNode;
import dk.brics.tajs.flowgraph.SourceLocation;
import dk.brics.tajs.monitoring.inspector.util.OccurenceCountingMap;
import dk.brics.tajs.solver.refinement.RefineQuery;
import dk.brics.tajs.util.AnalysisException;
import dk.brics.tajs.util.Pair;
import forwards_backwards_api.Formula;
import forwards_backwards_api.ProgramLocation;
import forwards_backwards_api.Query;

public class RefinerStatistics {

    private static RefinerStatistics instance;
    private long timeSpentCheckingAssumptions;
    private long timeValueRefinerSpentsOnRefining;
    private long timeSpentOnDataFlowPhase;
    private int numberOfRefinementQueriesToValueRefiner;
    private int numberOfRefinementQueriesAnsweredByCache;
    private int numberOfExceptionsFromRefiner;

    private long startTimeCheckingAssumptions;
    private long startTimeRefinement;
    private long startTimeDataflowPhase;
    private OccurenceCountingMap<Pair<ProgramLocation, Query>> refinementQueriesToValueRefiner;
    private OccurenceCountingMap<Pair<AbstractNode, SourceLocation>> refinementLocations;

    private RefinerStatistics() {
        refinementLocations = new OccurenceCountingMap();
        refinementQueriesToValueRefiner = new OccurenceCountingMap();
    }

    public static void init() {
        if (instance != null) {
            throw new AnalysisException("RefinerStatistics is already initialized!?!");
        }
        instance = new RefinerStatistics();
    }

    public static RefinerStatistics get() {
        if (instance == null) {
            init(); //TODO
//            throw new AnalysisException("RefinerStatistics is not initialized!?!");
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    public void registerExceptionFromRefiner() {
        if (timeSpentOnDataFlowPhase == 0)
            numberOfExceptionsFromRefiner++;
    }

    public int getNumberOfExceptionsFromRefiner() {
        return numberOfExceptionsFromRefiner;
    }

    public void beginDataflowPhase() {
        startTimeDataflowPhase = System.currentTimeMillis();
    }

    public void endDataflowPhase() {
        timeSpentOnDataFlowPhase = System.currentTimeMillis() - startTimeDataflowPhase;
    }

    public long getDataflowPhaseTime() {
        return timeSpentOnDataFlowPhase;
    }

    public void beginCheckAssumptions() {
        startTimeCheckingAssumptions = System.currentTimeMillis();
    }

    public void endCheckAssumptions() {
        if (timeSpentOnDataFlowPhase == 0)
            timeSpentCheckingAssumptions += System.currentTimeMillis() - startTimeCheckingAssumptions;
    }

    public long getAssumptionCheckingTime() {
        return timeSpentCheckingAssumptions;
    }

    public void beginRefinementQueryToValueRefiner(ProgramLocation location, RefineQuery<Formula> query) {
        refinementLocations.count(Pair.make(location.getNode(), location.getNode().getSourceLocation()));
        refinementQueriesToValueRefiner.count(Pair.make(location, query));
        numberOfRefinementQueriesToValueRefiner++;
        startTimeRefinement = System.currentTimeMillis();
    }

    public int getNumberOfRefinementQueriesToValueRefiner() {
        return numberOfRefinementQueriesToValueRefiner;
    }

    public void endRefinementQueryToValueRefiner() {
        if (timeSpentOnDataFlowPhase == 0)
            timeValueRefinerSpentsOnRefining += System.currentTimeMillis() - startTimeRefinement;
    }

    public long getRefinementTime() {
        return timeValueRefinerSpentsOnRefining;
    }

    public void registerRefinementQueryAnsweredByCache(ProgramLocation location, Query query) {
        if (timeSpentOnDataFlowPhase == 0)
           numberOfRefinementQueriesAnsweredByCache++;
    }

    public int getNumberOfRefinementQueriesAnsweredByCache() {
        return numberOfRefinementQueriesAnsweredByCache;
    }

    public int getNumberOfDifferentQueries() {
        return refinementQueriesToValueRefiner.getResults().size();
    }

    public float getAverageRefinementTime() {
        return numberOfRefinementQueriesToValueRefiner == 0 ? 0 : timeValueRefinerSpentsOnRefining / numberOfRefinementQueriesToValueRefiner;
    }

    public String getNodesWithRefinement() {
        return refinementLocations.toString();
    }
}
