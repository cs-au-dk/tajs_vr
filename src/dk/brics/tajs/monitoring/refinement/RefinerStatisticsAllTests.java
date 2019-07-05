package dk.brics.tajs.monitoring.refinement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dk.brics.tajs.flowgraph.AbstractNode;
import dk.brics.tajs.flowgraph.SourceLocation;
import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.monitoring.inspector.util.OccurenceCountingMap;
import dk.brics.tajs.refinement.instantiations.forwards_backwards.RefinerOptions;
import dk.brics.tajs.solver.refinement.QueryResult;
import dk.brics.tajs.solver.refinement.RefineQuery;
import dk.brics.tajs.util.AnalysisException;
import forwards_backwards_api.ProgramLocation;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.brics.tajs.util.Collections.addToMapSet;
import static dk.brics.tajs.util.Collections.newMap;
import static dk.brics.tajs.util.Collections.newSet;

public class RefinerStatisticsAllTests {

    private static RefinerStatisticsAllTests instance;

    // ------------- MAPS USED FOR STATS ----------------
    private Map<SourceLocation, OccurenceCountingMap<String>> issuedQueries;
    private Map<SourceLocation, OccurenceCountingMap<String>> successfulQueries;
    private Map<SourceLocation, OccurenceCountingMap<String>> unsuccessfulQueries;
    private Map<SourceLocation, Map<String, OccurenceCountingMap<String>>> exceptionalQueries;

    private Map<SourceLocation, OccurenceCountingMap<Long>> timeSuccessfulQueries;
    private Map<SourceLocation, OccurenceCountingMap<Long>> timeUnsuccessfulQueries;
    private Map<SourceLocation, OccurenceCountingMap<Long>> numberOfVisitedNodesSuccessfulQuery;
    private Map<SourceLocation, OccurenceCountingMap<Long>> numberOfVisitedNodesUnsuccessfulQuery;

    private Map<SourceLocation, OccurenceCountingMap<Boolean>> requiresInterproceduralReasoningMap;
    private Map<SourceLocation, OccurenceCountingMap<Boolean>> solvedByPropertyNameInferenceMap;
    private Map<SourceLocation, Set<String>> testsThatUseRefinementAtLocation;

    private Map<String, Set<String>> exceptionsToTestFiles;

    // ------- INFO NEEDED TO BUILD UP STATS -----------
    private long startQueryTime;
    private String currentTestName;
    private String currentTestSuite;
    private String currentStartTime;
    private Set<AbstractNode> visitedNodesInCurrentQuery;
    private boolean requiresInterproceduralReasoning;
    private boolean solvedByPropertyNameInference;
    private RefineQuery currentQuery;

    private RefinerStatisticsAllTests() {
        issuedQueries = newMap();
        successfulQueries = newMap();
        unsuccessfulQueries = newMap();
        exceptionalQueries = newMap();

        timeSuccessfulQueries = newMap();
        timeUnsuccessfulQueries = newMap();
        numberOfVisitedNodesSuccessfulQuery = newMap();
        numberOfVisitedNodesUnsuccessfulQuery = newMap();

        requiresInterproceduralReasoningMap = newMap();
        solvedByPropertyNameInferenceMap = newMap();
        testsThatUseRefinementAtLocation = newMap();

        exceptionsToTestFiles = newMap();

    }

    public static void init() {
        if (instance != null) {
            throw new AnalysisException("RefinerStatisticsAllTests has already been initialized!?!");
        }

        instance = new RefinerStatisticsAllTests();
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static RefinerStatisticsAllTests get() {
        if (instance == null) {
            throw new AnalysisException("RefinerStatisticsAllTests is not initialized!?!");
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    public void initAnalysis(String startTime, String testSuite, String testName) {
        currentStartTime = startTime;
        currentTestSuite = testSuite;
        currentTestName = testName;
    }

    public void endAnalysis() {
        String resFileName = "out/stats/refinerStatisticsAllTests" + currentTestSuite + ".json";// + "_" + currentStartTime + ".json";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject existingResults = null;
        try {
            existingResults = gson.fromJson(new FileReader(resFileName), JsonObject.class);
        } catch (FileNotFoundException e) {
            existingResults = new JsonObject();
        }

        JsonObject testResults = new JsonObject();

        for (SourceLocation sl : issuedQueries.keySet()) {
            JsonObject slObject = new JsonObject();
            slObject.add("issuedQueries", getJsonObjectFromOccurrenceCountingMap(issuedQueries.get(sl)));
            slObject.add("successfulQueries", getJsonObjectFromOccurrenceCountingMap(successfulQueries.get(sl)));
            slObject.add("unsuccessfulQueries", getJsonObjectFromOccurrenceCountingMap(unsuccessfulQueries.get(sl)));
            slObject.add("exceptionalQueries", getJsonObjectFromMapOfOccMap(exceptionalQueries.get(sl)));

            slObject.add("timeSuccessfulQueries", getJsonObjectFromOccurrenceCountingMap(timeSuccessfulQueries.get(sl)));
            slObject.add("timeUnsuccessfulQueries", getJsonObjectFromOccurrenceCountingMap(timeUnsuccessfulQueries.get(sl)));
            slObject.add("numberOfVisitedNodesSuccessfulQuery", getJsonObjectFromOccurrenceCountingMap(numberOfVisitedNodesSuccessfulQuery.get(sl)));
            slObject.add("numberOfVisitedNodesUnsuccessfulQuery", getJsonObjectFromOccurrenceCountingMap(numberOfVisitedNodesUnsuccessfulQuery.get(sl)));

            slObject.add("requiresInterproceduralReasoningMap", getJsonObjectFromOccurrenceCountingMap(requiresInterproceduralReasoningMap.get(sl)));
            slObject.add("solvedByPropertyNameInferenceMap", getJsonObjectFromOccurrenceCountingMap(solvedByPropertyNameInferenceMap.get(sl)));

            testResults.add(sl.toString(), slObject);

        }
        existingResults.add(currentTestName, testResults);

        String updatedResults = gson.toJson(existingResults);
        try {
            Files.write(Paths.get(resFileName), updatedResults.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonElement getJsonObjectFromMapOfOccMap(Map<String, OccurenceCountingMap<String>> map) {
        if (map == null) {
            return new JsonObject();
        }
        JsonObject res = new JsonObject();
        for (String key : map.keySet()) {
            res.add(key, getJsonObjectFromOccurrenceCountingMap(map.get(key)));
        }
        return res;
    }

    private <T> JsonElement getJsonObjectFromOccurrenceCountingMap(OccurenceCountingMap<T> stringOccurenceCountingMap) {
        if (stringOccurenceCountingMap == null) {
            return new JsonObject();
        }
        JsonObject res = new JsonObject();
        Map<T, Integer> mapView = stringOccurenceCountingMap.getMapView();
        for (T key : mapView.keySet()) {
            res.addProperty(key.toString(), mapView.get(key));
        }
        return res;
    }

    public void initQuery(ProgramLocation loc, RefineQuery query) {
        startQueryTime = System.currentTimeMillis();
        currentQuery = query;
        countMap(issuedQueries, loc.getNode().getSourceLocation(), query.toString());
        visitedNodesInCurrentQuery = newSet();
        requiresInterproceduralReasoning = false;
        solvedByPropertyNameInference = false;
        addToMapSet(testsThatUseRefinementAtLocation, loc.getNode().getSourceLocation(), currentTestName);
    }

    public void registerNodeVisitedDuringQuery(AbstractNode n) {
        if (visitedNodesInCurrentQuery == null) {
            return;
        }
        visitedNodesInCurrentQuery.add(n);
    }

    public void registerRequiresInterproceduralReasoning() {
        requiresInterproceduralReasoning = true;
    }

    public void registerResultFoundByPropNameInference() {
        solvedByPropertyNameInference = true;
    }

    public void queryFinished(ProgramLocation loc, QueryResult queryResult) {
        boolean querySuccessful = isQuerySuccessful((Collection<Value>) queryResult.getResult(), currentQuery.getSoundDefault().getResult());
        countMap(querySuccessful ? successfulQueries : unsuccessfulQueries, loc.getNode().getSourceLocation(), currentQuery.toString());

        if (RefinerOptions.get().isDisplayQueryDebuggingInformationEnabled() && !querySuccessful) {
            System.out.println("Query unsuccesful: ");
            System.out.println("currentTestName = " + currentTestName);
            System.out.println("currentQuery = " + currentQuery);
            System.out.println("queryResult = " + queryResult);
            System.out.println("loc.getNode().getSourceLocation() = " + loc.getNode().getSourceLocation());
        }

        long queryTime = System.currentTimeMillis() - startQueryTime;
        countMap(querySuccessful ? timeSuccessfulQueries : timeUnsuccessfulQueries, loc.getNode().getSourceLocation(), queryTime);
        countMap(querySuccessful ? numberOfVisitedNodesSuccessfulQuery : numberOfVisitedNodesUnsuccessfulQuery, loc.getNode().getSourceLocation(), new Long(visitedNodesInCurrentQuery.size()));

        countMap(requiresInterproceduralReasoningMap, loc.getNode().getSourceLocation(), requiresInterproceduralReasoning);
        countMap(solvedByPropertyNameInferenceMap, loc.getNode().getSourceLocation(), solvedByPropertyNameInference);
    }

    public void queryTerminatedWithException(ProgramLocation loc, Throwable t) {
        String errorString = t.getClass() + ": " + t.getMessage();
        addToMapSet(exceptionsToTestFiles, errorString, currentTestName);
        countMap(exceptionalQueries, loc.getNode().getSourceLocation(), errorString, currentQuery.toString());
    }

    public static boolean isQuerySuccessful(Collection<Value> result, Collection<Value> soundDefault) {
        result = result.stream().filter(v -> !v.isNone()).collect(Collectors.toSet());
        if (Value.join(result).isNone()) {
            return true;
        }
        if (result.stream().allMatch(v -> soundDefault.stream().anyMatch(soundV -> v.equals(soundV))))
            return false;

        return true;
    }

    private <T> void countMap(Map<SourceLocation, OccurenceCountingMap<T>> map, SourceLocation key, T value) {
        if (!map.containsKey(key)) {
            map.put(key, new OccurenceCountingMap());
        }
        map.get(key).count(value);
    }

    private <R, T> void countMap(Map<SourceLocation, Map<R, OccurenceCountingMap<T>>> map, SourceLocation key, R key2, T value) {
        if (!map.containsKey(key)) {
            map.put(key, newMap());
        }
        if (!map.get(key).containsKey(key2)) {
            map.get(key).put(key2, new OccurenceCountingMap());
        }
        map.get(key).get(key2).count(value);
    }

    public void formatMinMaxAverageStats(String description, OccurenceCountingMap<Long> map) {
        long smallest = Long.MAX_VALUE;
        long largest = -1;
        int numberOfElements = 0;
        long totalSum = 0;

        for (OccurenceCountingMap.CountingResult<Long> time : map.getResults()) {
            if (time.getElement() < smallest)
                smallest = time.getElement();
            if (time.getElement() > largest)
                largest = time.getElement();
            numberOfElements += time.getOccurences();
            totalSum += time.getElement() * time.getOccurences();
        }
        if (numberOfElements == 0) {
            System.out.println(description + ": No data");
        } else {
            System.out.println(description + ": " + smallest + " to " + largest + " with average " + (totalSum / numberOfElements));
        }
    }
}
