package dk.brics.tajs.refinement.instantiations.forwards_backwards;

import dk.brics.tajs.analysis.Conversion;
import dk.brics.tajs.analysis.Solver;
import dk.brics.tajs.analysis.js.Operators;
import dk.brics.tajs.analysis.nativeobjects.TAJSFunction;
import dk.brics.tajs.flowgraph.AbstractNode;
import dk.brics.tajs.flowgraph.BasicBlock;
import dk.brics.tajs.flowgraph.Function;
import dk.brics.tajs.flowgraph.jsnodes.BeginForInNode;
import dk.brics.tajs.flowgraph.jsnodes.BeginLoopNode;
import dk.brics.tajs.flowgraph.jsnodes.BinaryOperatorNode;
import dk.brics.tajs.flowgraph.jsnodes.CallNode;
import dk.brics.tajs.flowgraph.jsnodes.DeclareFunctionNode;
import dk.brics.tajs.flowgraph.jsnodes.EndForInNode;
import dk.brics.tajs.flowgraph.jsnodes.EndLoopNode;
import dk.brics.tajs.flowgraph.jsnodes.NewObjectNode;
import dk.brics.tajs.flowgraph.jsnodes.ReadVariableNode;
import dk.brics.tajs.flowgraph.jsnodes.UnaryOperatorNode;
import dk.brics.tajs.js2flowgraph.FlowGraphBuilder;
import dk.brics.tajs.lattice.CallEdge;
import dk.brics.tajs.lattice.Context;
import dk.brics.tajs.lattice.HostObject;
import dk.brics.tajs.lattice.ObjProperties;
import dk.brics.tajs.lattice.ObjectLabel;
import dk.brics.tajs.lattice.PKey;
import dk.brics.tajs.lattice.State;
import dk.brics.tajs.lattice.UnknownValueResolver;
import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.monitoring.refinement.RefinerStatisticsAllTests;
import dk.brics.tajs.options.Options;
import dk.brics.tajs.solver.BlockAndContext;
import dk.brics.tajs.solver.CallGraph;
import dk.brics.tajs.solver.refinement.QueryManager;
import dk.brics.tajs.util.AnalysisException;
import dk.brics.tajs.util.Pair;
import forwards_backwards_api.Forwards;
import forwards_backwards_api.ProgramLocation;
import forwards_backwards_api.Properties;
import forwards_backwards_api.memory.MemoryLocation;
import forwards_backwards_api.memory.MemoryLocationVisitor;
import forwards_backwards_api.memory.Property;
import forwards_backwards_api.memory.Register;
import forwards_backwards_api.memory.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static dk.brics.tajs.analysis.nativeobjects.ECMAScriptObjects.FUNCTION_CALL;
import static dk.brics.tajs.util.Collections.addToMapMapSet;
import static dk.brics.tajs.util.Collections.addToMapSet;
import static dk.brics.tajs.util.Collections.newList;
import static dk.brics.tajs.util.Collections.newMap;
import static dk.brics.tajs.util.Collections.newSet;
import static dk.brics.tajs.util.Collections.singleton;

public class TAJSForwardsAPI implements Forwards {

    // FIXME handle (delayed) side effects from implicit calls

    private final Map<Function, Map<AbstractNode, Set<AbstractNode>>> nodePredecessors;

    private Solver.SolverInterface c;

    /**
     * Due to the technicality that TAJS has implicit intermediary states, node transfers may need to be re-applied.
     */
    private int nodeTransfers;

    public TAJSForwardsAPI() {
        this.nodeTransfers = 0;
        this.nodePredecessors = newMap();
    }

    public void setSolverInterface(Solver.SolverInterface c) {
        this.c = c;
    }

    public Solver.SolverInterface getSolverInterface() {
        return c;
    }
    /**
     * @return the number of node transfers performed by this instance.
     */
    public int getNodeTransfers() {
        return nodeTransfers;
    }

    @Override
    public Set<ProgramLocation> getCallSites(ProgramLocation functionEntry) {
        if (RefinerStatisticsAllTests.isInitialized())
            RefinerStatisticsAllTests.get().registerRequiresInterproceduralReasoning();
        BasicBlock entryBlock = functionEntry.getNode().getBlock();
        if (Options.get().isDebugOrTestEnabled() && !entryBlock.isEntry()) {
            throw new AnalysisException(String.format("%s: is not a function entry", functionEntry));
        }
        Set<Context> contexts = c.getAnalysisLatticeElement().getStates(entryBlock).keySet();
        CallGraph<State, Context, CallEdge> callGraph = c.getAnalysisLatticeElement().getCallGraph();

        Set<ProgramLocation> callsites = newSet();
        contexts.forEach(ctx -> {
            Set<CallGraph.ReverseEdge<Context>> sources = callGraph.getSources(BlockAndContext.makeEntry(entryBlock, ctx));
            sources.forEach(source -> callsites.add(new ProgramLocation(
                    source.getCallNode(),
                    source.getCallerContext())));
        });
        if (RefinerOptions.get().isContextInsensitiveRefinementsEnabled()) {
            Set<ProgramLocation> newProgramLocations = getProgramLocationsWithJoinedContextsForEachNode(callsites);
            return newProgramLocations;
        }
        return callsites;
    }

    @Override
    public Pair<Set<ProgramLocation>, Set<HostObject>> getCalleeExits(ProgramLocation caller) {
        if (RefinerStatisticsAllTests.isInitialized())
            RefinerStatisticsAllTests.get().registerRequiresInterproceduralReasoning();
        CallGraph<State, Context, CallEdge> callGraph = c.getAnalysisLatticeElement().getCallGraph();

        Set<ProgramLocation> calleeExits = newSet();
        caller.getContexts().forEach(ctx -> {
            Map<BlockAndContext<Context>, CallEdge> edges = callGraph.getCallEdges_unsafe(caller.getNode(), ctx);
            if (edges != null) {
                edges.forEach((blockAndContext, edge) -> calleeExits.add(
                        new ProgramLocation(
                                blockAndContext.getBlock().getFunction().getOrdinaryExit().getFirstNode(),
                                blockAndContext.getContext()))
                );
            }
        });

        Set<HostObject> natives = newSet();
        final Set<ObjectLabel> calleeLabels = newSet();
        if (caller.getNode() instanceof CallNode) {
            CallNode callNode = (CallNode) caller.getNode();
            if (callNode.getTajsFunctionName() != null) {
                natives.add(new TAJSFunction(callNode.getTajsFunctionName()));
            } else if (callNode.getFunctionRegister() != AbstractNode.NO_VALUE) {
                calleeLabels.addAll(get(caller, new Register(callNode.getFunctionRegister())).getObjectLabels());
            } else {
                final Value propertyName;
                if (callNode.getPropertyRegister() != AbstractNode.NO_VALUE) {
                    propertyName = get(caller, new Register(callNode.getPropertyRegister()));
                } else {
                    propertyName = Value.makeStr(callNode.getPropertyString());
                }
                Value baseValue = get(caller, new Register(callNode.getBaseRegister()));
                baseValue.getObjectLabels().forEach(base -> {

                    Set<ObjectLabel> objectLabels = get(caller, new Property(base, propertyName)).getObjectLabels();
                    if (objectLabels.stream().filter(obj -> obj.isHostObject()).anyMatch(obj -> obj.getHostObject() == FUNCTION_CALL) //Calls to native functions aren't visible in CallGraph
                            && base.isHostObject()) {
                        calleeLabels.add(base);
                    }
                    calleeLabels.addAll(objectLabels);
                });
            }
        }
        natives.addAll(
                calleeLabels.stream()
                        .filter(l -> l.isHostObject())
                        .map(l -> l.getHostObject())
                        .collect(Collectors.toSet()));

        if (RefinerOptions.get().isContextInsensitiveRefinementsEnabled()) {
            Set<ProgramLocation> newProgramLocations = getProgramLocationsWithJoinedContextsForEachNode(calleeExits);
            return Pair.make(newProgramLocations, natives);
        }
        Set<ProgramLocation> reachableCalleeExits = calleeExits.stream().filter(location -> { //Only use reachable calleeExits
            BasicBlock block = location.getNode().getBlock();
            Collection<State> states = location.getContexts().stream().map(ctx -> c.getAnalysisLatticeElement().getState(block, ctx)).collect(Collectors.toSet());
            return states.stream().anyMatch(state -> state != null);
        }).collect(Collectors.toSet());

        return Pair.make(reachableCalleeExits, natives);
    }

    private Set<ProgramLocation> getProgramLocationsWithJoinedContextsForEachNode(Set<ProgramLocation> calleeExits) {
        Map<AbstractNode, Set<Context>> newProgramLocationsInfo = newMap();
        calleeExits.forEach(csLoc -> addToMapSet(newProgramLocationsInfo, csLoc.getNode(), csLoc.getContext()));
        return newProgramLocationsInfo.entrySet().stream().map(e -> new ProgramLocation(e.getKey(), e.getValue())).collect(Collectors.toSet());
    }

    @Override
    public Value get(ProgramLocation location, MemoryLocation memoryLocation) {
        if (location.getNode() instanceof CallNode && ((CallNode) location.getNode()).getTajsFunctionName() != null) {
            throw new UnsupportedOperationException("calling get on a TAJSfunction");
        }

        Collection<State> states = TAJSForwardsAPI.this.makeIntermediaryState(location, true); //TODO: makeIntermediaryState might be called twice during one get

        Set<Value> values = states.stream().map(state -> {
            java.util.function.Function<Value, Value> real = v -> UnknownValueResolver.getRealValue(v, state);
            return memoryLocation.accept(new MemoryLocationVisitor<Value>() {

                @Override
                public Value visit(Property l) {
                    Value propNameCoerced = withTempState(location.getNode(), state, () -> Conversion.toString(l.getName(), c));
                    return real.apply(Value.join(TAJSForwardsAPI.this.withTempState(location,
                            () -> real.apply(TAJSForwardsAPI.this.c.getAnalysis().getPropVarOperations().readPropertyValue(singleton(l.getObject()), propNameCoerced)), true)));
                }

                @Override
                public Value visit(Variable l) {
                    ProgramLocation adjustedLocation = getAdjustedLocationFromDeclareFunctionNodeOrReadVariableNode();
                    Collection<State> adjustedStates = TAJSForwardsAPI.this.makeIntermediaryState(adjustedLocation, true);
                    return Value.join(adjustedStates.stream().map(adjustedState ->
                            TAJSForwardsAPI.this.withTempState(adjustedLocation.getNode(), adjustedState,
                                    (() -> UnknownValueResolver.getRealValue(
                                            TAJSForwardsAPI.this.c.getAnalysis().getPropVarOperations().readVariable(l.getName(), null),
                                            adjustedState)))).collect(Collectors.toSet()));
                }

                @Override
                public Value visit(Register l) {
                    ProgramLocation adjustedLocation = getAdjustedLocationFromDeclareFunctionNodeOrReadVariableNode();
                    if (location.getNode() instanceof CallNode) {
                        CallNode n = (CallNode) location.getNode();
                        if (n.isConstructorCall() && l.getId() == n.getResultRegister()) {
                            adjustedLocation = new ProgramLocation(n.getBlock().getSingleSuccessor().getFirstNode(), location.getContexts());
                        }
                    }

                    Collection<State> adjustedStates = TAJSForwardsAPI.this.makeIntermediaryState(adjustedLocation, true);
                    try {
                        return Value.join(adjustedStates.stream().map(adjustedState -> UnknownValueResolver.getRealValue(adjustedState.readRegister(l.getId()), adjustedState)).collect(Collectors.toSet()));
                    } catch (AnalysisException e) {
                        return Value.makeNone();
                    }
                }

                private ProgramLocation getAdjustedLocationFromDeclareFunctionNodeOrReadVariableNode() {
                    ProgramLocation adjustedLocation = location;
                    if (location.getNode() instanceof DeclareFunctionNode || location.getNode() instanceof NewObjectNode || location.getNode() instanceof ReadVariableNode) {
                        boolean shouldAdjustLocationToNextNode = false;
                        for (AbstractNode node : location.getNode().getBlock().getNodes()) {
                            if (shouldAdjustLocationToNextNode) {
                                adjustedLocation = new ProgramLocation(node, location.getContexts());
                                shouldAdjustLocationToNextNode = false;
                                break;
                            }
                            if (node.equals(location.getNode())) {
                                shouldAdjustLocationToNextNode = true;
                            }
                        }
                        if (shouldAdjustLocationToNextNode) {
                            adjustedLocation = new ProgramLocation(location.getNode().getBlock().getSingleSuccessor().getFirstNode(), location.getContexts());
                        }
                    }
                    return adjustedLocation;
                }
            });
        }).collect(Collectors.toSet());

        Value join = Value.join(values);
        if (join.isMaybeStrIdentifier()) {
            join = join.join(Value.makeAnyStrNotUInt());
        }
        return join;
    }

    @Override
    public Value binop(ProgramLocation location, Value v1, BinaryOperatorNode.Op op, Value v2) {
        if (v1.getObjectLabels().size() == 0 && v2.getObjectLabels().size() == 0) {
            return withExceptionsDisabled(() -> Operators.binop(v1, op, v2, c));
        }
        return Value.join(withTempState(location, () -> Operators.binop(v1, op, v2, c), false));
    }

    @Override
    public Value unop(ProgramLocation location, UnaryOperatorNode.Op op, Value v) {
        if (v.getObjectLabels().size() == 0) {
            return withExceptionsDisabled(() -> Operators.unop(op, v, c));
        }
        return Value.join(withTempState(location, () -> Operators.unop(op, v, c), false));
    }

    public <T> T withExceptionsDisabled(Supplier<T> f) {
        boolean exceptionsEnabled = !Options.get().isExceptionsDisabled();
        Options.get().enableNoExceptions();
        T result = f.get();
        if (exceptionsEnabled) {
            Options.get().disableNoExceptions();
        }
        return result;
    }

    @Override
    public Set<ProgramLocation> getPredecessors(ProgramLocation location, boolean loopUnrollingInsensitive) {
        AbstractNode node = location.getNode();
        Function function = node.getBlock().getFunction();
        if (!nodePredecessors.containsKey(function)) { // FIXME remove function from cache when unevalizer mutates...
            nodePredecessors.put(function, FlowGraphBuilder.makeNodePredecessorMap(function));
        }
        Set<AbstractNode> predecessors = nodePredecessors.get(function).get(node);

        Set<ProgramLocation> contextInsensitivePredecessors = predecessors.stream()
                //filter unreachable states, i.e., state is null for any context
                .filter(n -> {
                    Map<Context, State> contextToStates = c.getAnalysisLatticeElement().getStates(n.getBlock());
                    return contextToStates.values().stream().anyMatch(state -> state != null && !state.isBottom());
                })
                .flatMap(n -> {
                    Map<Context, State> contextToStates = c.getAnalysisLatticeElement().getStates(n.getBlock());
                    return contextToStates.keySet().stream().map(c -> new ProgramLocation(n, c));
                })
                .collect(Collectors.toSet());

        Set<Context> queriedContexts = location.getContexts();
        Set<ProgramLocation> contextSensitivePredecessors = contextInsensitivePredecessors.stream()
                .filter(ciLoc -> queriedContexts.stream().anyMatch(ctx -> contextEquality(ctx, ciLoc.getContext(), loopUnrollingInsensitive)) ||
                        ciLoc.getNode() instanceof EndForInNode ||
                        ciLoc.getNode() instanceof EndLoopNode ||
                        ciLoc.getNode() instanceof BeginForInNode ||
                        ciLoc.getNode() instanceof BeginLoopNode
                ).collect(Collectors.toSet());

        // filter out the predecessors that are BeginForInNodes and has a specialized context that does not match the
        // one we should find the predecessors for.
        contextSensitivePredecessors = contextSensitivePredecessors.stream().filter( loc ->
                !(loc.getNode() instanceof BeginForInNode) ||
                        (loc.getContext().getSpecialRegisters() == null ||
                                queriedContexts.stream().anyMatch( ctx ->
                                        loc.getContext().getSpecialRegisters().entrySet().stream().allMatch( e -> e.getValue().equals(ctx.getSpecialRegisters().get(e.getKey()))))))

                .filter( loc -> queriedContexts.stream()
                        .anyMatch(ctx -> Objects.equals(ctx.getThisVal(), loc.getContext().getThisVal())))
                .filter( loc -> queriedContexts.stream()
                        .anyMatch( ctx ->
                                Objects.equals(ctx.getArguments(), loc.getContext().getArguments()) &&
                                Objects.equals(ctx.getUnknownArg(), loc.getContext().getUnknownArg()) &&
                                Objects.equals(ctx.getParameterNames(), loc.getContext().getParameterNames()) &&
                                Objects.equals(ctx.getFreeVariables(), loc.getContext().getFreeVariables())))
                .filter( loc -> !(loc.getNode() instanceof BeginForInNode || loc.getNode() instanceof EndForInNode)
                        || queriedContexts.stream()
                            .anyMatch( ctx -> Objects.equals(ctx.getLoopUnrolling(), loc.getContext().getLoopUnrolling())))
                .filter( loc -> !(loc.getNode() instanceof BeginLoopNode || loc.getNode() instanceof EndLoopNode)
                        || queriedContexts.stream()
                        .anyMatch( ctx -> Objects.equals(ctx.getSpecialRegisters(), loc.getContext().getSpecialRegisters())))
                .filter( loc -> !(loc.getNode() instanceof BeginLoopNode) || loc.getContexts().stream().anyMatch(ctx -> {
                    if(!(loc.getNode() instanceof BeginLoopNode) || loopUnrollingInsensitive) {
                        return true;
                    }
                    if(location.getNode() instanceof BeginLoopNode) {
                        if (ctx.getLoopUnrolling() != null && ctx.getLoopUnrolling().containsKey(location.getNode())) {
                            int unrollingCount = ctx.getLoopUnrolling().get(location.getNode());
                            if (unrollingCount > 0) {
                                return loc.getContext().getLoopUnrolling() != null
                                    && loc.getContext().getLoopUnrolling().containsKey(location.getNode())
                                        && (loc.getContext().getLoopUnrolling().get(location.getNode()) == unrollingCount - 1);
                            }
                        }
                    }
                    return true;
                }))
                .filter( loc -> !(loc.getNode() instanceof EndLoopNode)
                        || queriedContexts.stream()
                            .anyMatch( ctx -> ctx.getLoopUnrolling() == null
                                    || Objects.equals(ctx.getLoopUnrolling(), loc.getContext().getLoopUnrolling())))
                .collect(Collectors.toSet());

        if (RefinerStatisticsAllTests.isInitialized())
            contextSensitivePredecessors.forEach(loc -> RefinerStatisticsAllTests.get().registerNodeVisitedDuringQuery(loc.getNode()));
        if (RefinerOptions.get().isContextInsensitiveRefinementsEnabled()) {
            return getProgramLocationsWithJoinedContextsForEachNode(contextSensitivePredecessors);
        } else if (loopUnrollingInsensitive) {
            Set<ProgramLocation> loopInsensitivePredecessors = newSet();
            //map that collects all context that is equal up to loop unrollings in a set.
            Map<AbstractNode, Map<Context, Set<Context>>> map = newMap();
            contextSensitivePredecessors.forEach( pl -> addToMapMapSet(map, pl.getNode(), getContextWithoutUnrollings(pl.getContext()), pl.getContext()));
            map.entrySet().forEach(e -> e.getValue().values().forEach( c -> loopInsensitivePredecessors.add(new ProgramLocation(e.getKey(), c))));
            return loopInsensitivePredecessors;
        }

        return contextSensitivePredecessors;
    }

    private boolean contextEquality(Context ctx1, Context ctx2, boolean loopUnrollingInsensitive) {
        if(!loopUnrollingInsensitive) {
            return ctx1.equals(ctx2);
        }

        Context ctx1WithoutUnrolling = getContextWithoutUnrollings(ctx1);
        Context ctx2WithoutUnrolling = getContextWithoutUnrollings(ctx2);
        return ctx1WithoutUnrolling.equals(ctx2WithoutUnrolling);
    }

    private Context getContextWithoutUnrollings(Context ctx) {
        if (ctx.getLoopUnrolling() == null) {
            return ctx;
        }
        return ctx.copyWithLoopUnrolling(null);
    }

    @Override
    public Properties getForInProperties(ProgramLocation location, ObjectLabel object) {
        ObjProperties.PropertyQuery propertyQuery = ObjProperties.PropertyQuery.makeQuery();
        propertyQuery.setOnlyEnumerable(true);
        propertyQuery.usePrototypes();

        Collection<ObjProperties> statesProperties = withTempState(
                location,
                () -> c.getState().getProperties(singleton(object), propertyQuery),
                true);

        List<String> definiteProperties = newList();
        List<String> maybeProperties = newList();

        statesProperties.forEach(properties -> {
            definiteProperties.addAll(getPropertyNamesFromPKeys(properties.getDefinitely()));
            maybeProperties.addAll(getPropertyNamesFromPKeys(properties.getMaybe()));
        });

        return new Properties(definiteProperties, maybeProperties);
    }

    private List<String> getPropertyNamesFromPKeys(Set<PKey> pkeys) {
        if (pkeys.stream().anyMatch(pkey -> !(pkey instanceof PKey.StringPKey))) {
            throw new UnsupportedOperationException("Only supports StringPKeys");
        }
        return pkeys.stream().map(pkey -> (PKey.StringPKey) pkey).map(pkey -> pkey.getStr()).collect(Collectors.toList());
    }

    @Override
    public Set<ObjectLabel> getPrototypeObjects(ProgramLocation location, ObjectLabel object, Value propertyName) {
        Collection<State> states = makeIntermediaryState(location, true);
        Set<ObjectLabel> prototypeObjects = states.stream().flatMap(state ->
                withTempState(location.getNode(), state, () -> {
                    Value propertyNameCoerced = Conversion.toString(propertyName, c);
                    return c.getState().getPrototypeWithProperty(object, propertyNameCoerced);
                }).stream()
        ).collect(Collectors.toSet());
        return prototypeObjects;
    }

    @Override
    public Optional<Set<Value>> inferPropertyNames(ProgramLocation location, Value base, Value targetValue, boolean usePrototypes) {
        if (targetValue.isMaybeUndef() && !targetValue.isMaybeOtherThanUndef()) {
            if (RefinerStatisticsAllTests.isInitialized())
                RefinerStatisticsAllTests.get().registerResultFoundByPropNameInference();
            return Optional.of(singleton(Value.makeAnyStr()));
        }

        ObjProperties.PropertyQuery propertyQuery = ObjProperties.PropertyQuery.makeQuery();
        if (usePrototypes)
            propertyQuery.usePrototypes();

        Collection<Optional<Set<Value>>> res = withTempState(location, () -> {
            ObjProperties statesProperties = c.getState().getProperties(base.getObjectLabels(), propertyQuery);
            if ((statesProperties.isDefaultNumericMaybePresent() && base.getObjectLabels().stream().anyMatch(obj -> !UnknownValueResolver.getDefaultNumericProperty(obj, c.getState()).restrictToStrictEquals(targetValue).isNone()))
                    || (statesProperties.isDefaultOtherMaybePresent() && base.getObjectLabels().stream().anyMatch(obj -> !UnknownValueResolver.getDefaultOtherProperty(obj, c.getState()).restrictToStrictEquals(targetValue).isNone()))) {
                if (RefinerOptions.get().isDisplayQueryDebuggingInformationEnabled()) {
                    System.out.println("Returning Optional.empty from inferPropertyNames due to defaultArray or defaultNonArray");
                }
                return Optional.empty();
            }
            Collection<String> propertyNames = getPropertyNamesFromPKeys(statesProperties.getMaybe());
            propertyNames.add("__proto__");
            Set<String> inferredPropertyNames = newSet();

            // Read each property and see whether meet between the read value and targetValue != bottom
            for (String propName : propertyNames) {
                Value propValue = UnknownValueResolver.getRealValue(c.getAnalysis().getPropVarOperations().readPropertyValue(base.getObjectLabels(), propName), c.getState());
                Value propValueMeetTarget = targetValue.restrictToStrictEquals(propValue);
                if (!propValueMeetTarget.isNone()) {
                    inferredPropertyNames.add(propName);
                }
            }

            return Optional.of(inferredPropertyNames.stream().map(Value::makeStr).collect(Collectors.toSet()));
        }, true);
        Set<Value> finalRes = newSet();
        boolean hasRes = false;
        for (Optional<Set<Value>> valuesOpt : res) {
            if (valuesOpt.isPresent()) {
                hasRes = true;
                finalRes.addAll(valuesOpt.get());
            }
        }
        if (hasRes && RefinerStatisticsAllTests.isInitialized())
            RefinerStatisticsAllTests.get().registerResultFoundByPropNameInference();
        return hasRes ? Optional.of(finalRes) : Optional.empty();
    }

    @Override
    public Optional<Set<Value>> inferPropertyValue(ProgramLocation location, Value base, Value propertyName, boolean usePrototypes) {
        Collection<Optional<Set<Value>>> res = withTempState(location,
                () -> Optional.of(singleton(c.getAnalysis().getPropVarOperations().readPropertyValue(base.getObjectLabels(), propertyName)))
                , true);

        Set<Value> finalRes = newSet();
        for (Optional<Set<Value>> valuesOpt : res) {
            if (valuesOpt.isPresent()) {
                finalRes.addAll(valuesOpt.get());
            }
        }
        return Optional.of(finalRes);
    }

    private <T> Collection<T> withTempState(ProgramLocation location, Supplier<T> f, boolean forGet) {
        Collection<State> states = makeIntermediaryState(location, forGet);
        return states.stream().map(state -> withTempState(location.getNode(), state, f)).collect(Collectors.toSet());
    }

    private <T> T withTempState(AbstractNode node, State state, Supplier<T> f) {
        return withExceptionsDisabled(() -> {
            AbstractNode currentNode = c.getNode();
            State currentState = c.getState();
            c.setState(state);
            c.setNode(node);
            T result = f.get();
            c.setState(currentState);
            c.setNode(currentNode);
            return result;
        });

    }

    private Collection<State> makeIntermediaryState(ProgramLocation location, boolean forGet) {
        BasicBlock block = location.getNode().getBlock();
        Collection<State> states = location.getContexts().stream().map(ctx -> c.getAnalysisLatticeElement().getState(block, ctx)).collect(Collectors.toSet());

        return states.stream().map(state -> {
            if (state == null) {
                if (forGet) {
                    throw new AnalysisException("State is not reachable! It should not be used!");
                }
                return new State(c, block);
            }
            State blockEntryState = state.clone();
            return withTempState(block.getFirstNode(), blockEntryState, () -> {
                List<AbstractNode> nodes = location.getNode().getBlock().getNodes();
                if (QueryManager.MultiQueryManager.getInstance() != null) {
                    QueryManager.MultiQueryManager.getInstance().setAllowRefinements(false);
                }
                for (AbstractNode n : nodes) {
                    c.setNode(n);
                    if (location.getNode().equals(n)) {
                        break;
                    }
                    nodeTransfers++;
                    c.getAnalysis().getNodeTransferFunctions().transfer(n);
                }
                if (QueryManager.MultiQueryManager.getInstance() != null) {
                    QueryManager.MultiQueryManager.getInstance().setAllowRefinements(true);
                }
                return c.getState();
            });
        }).collect(Collectors.toSet());
    }
}
