/*
 * Copyright 2009-2019 Aarhus University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.brics.tajs.analysis;

import dk.brics.tajs.flowgraph.AbstractNode;
import dk.brics.tajs.flowgraph.Function;
import dk.brics.tajs.flowgraph.jsnodes.BeginForInNode;
import dk.brics.tajs.flowgraph.jsnodes.BeginLoopNode;
import dk.brics.tajs.flowgraph.jsnodes.EndLoopNode;
import dk.brics.tajs.lattice.Context;
import dk.brics.tajs.lattice.ObjectLabel;
import dk.brics.tajs.lattice.State;
import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static dk.brics.tajs.util.Collections.addToMapSet;
import static dk.brics.tajs.util.Collections.newList;
import static dk.brics.tajs.util.Collections.newMap;

/**
 * Context sensitivity strategy determined by TAJS_makeContextSensitive instructions in the analyzed program.
 * Wraps another context sensitivity strategy while enabling custom context sensitivity for selected functions.
 * The wrapper can encode parameter-sensitivity, object-sensitivity, and closure-variable-sensitivity,
 * and uses the wrapped context sensitivity strategy as default.
 */
public class CustomContextSensitivityStrategy implements IContextSensitivityStrategy {

    /**
     * Map from (caller,callee) to set of sensitivity flags.
     * @see #makeSensitiveFromCaller(Function, int, Function)
     */
    private Map<Pair<Function, Function>, Set<Integer>> sensitiveFunctions;

    private IContextSensitivityStrategy defaultContextSensitivity;

    /**
     * Creates a new <code>CustomContextSensitivityWrapper</code> by wrapping the given context sensitivity strategy.
     */
    public CustomContextSensitivityStrategy(IContextSensitivityStrategy defaultContextSensitivity) {
        sensitiveFunctions = newMap();
        this.defaultContextSensitivity = defaultContextSensitivity;
    }

    @Override
    public Context makeFunctionHeapContext(Function fun, Solver.SolverInterface c) {
        Context context = makeClosureVariableContext(fun, c);
        if (context != null) {
            return context;
        }
        return defaultContextSensitivity.makeFunctionHeapContext(fun, c);
    }

    @Override
    public Context makeActivationAndArgumentsHeapContext(State state, ObjectLabel function, FunctionCalls.CallInfo callInfo, Solver.SolverInterface c) {
        Context functionContext = makeContextArguments(state, function, state.getExecutionContext().getThis(), callInfo);
        if (functionContext != null) {
            return functionContext;
        }
        return defaultContextSensitivity.makeActivationAndArgumentsHeapContext(state, function, callInfo, c);
    }

    @Override
    public Context makeConstructorHeapContext(State state, ObjectLabel function, FunctionCalls.CallInfo callInfo, Solver.SolverInterface c) {
        Context functionContext = makeContextArguments(state, function, Value.makeNone(), callInfo); // not using state.getExecutionContext().getThis() here
        if (functionContext != null) {
            return functionContext;
        }
        return defaultContextSensitivity.makeConstructorHeapContext(state, function, callInfo, c);
    }

    @Override
    public Context makeObjectLiteralHeapContext(AbstractNode node, State state, Solver.SolverInterface c) {
        return defaultContextSensitivity.makeObjectLiteralHeapContext(node, state, c);
    }

    @Override
    public Context makeInitialContext() {
        return defaultContextSensitivity.makeInitialContext();
    }

    @Override
    public Context makeFunctionEntryContext(State state, ObjectLabel function, FunctionCalls.CallInfo callInfo, Solver.SolverInterface c) {
        Context functionContext = makeContextArguments(state, function, state.getExecutionContext().getThis(), callInfo);
        if (functionContext != null) {
            return Context.make(functionContext.getThisVal(), functionContext.getSpecialRegisters(), null, functionContext.getExtraAllocationContexts(), functionContext.getLoopUnrolling(), functionContext.getUnknownArg(), functionContext.getParameterNames(), functionContext.getArguments(), functionContext.getFreeVariables(), functionContext.getFreeVariablePartitioning(), function.getPropertyReadSpecialization());
        }
        return defaultContextSensitivity.makeFunctionEntryContext(state, function, callInfo, c);
    }

    @Override
    public Context makeForInEntryContext(Context currentContext, BeginForInNode n, Value v) {
        return defaultContextSensitivity.makeForInEntryContext(currentContext, n, v);
    }

    @Override
    public Context makeNextLoopUnrollingContext(Context currentContext, BeginLoopNode node) {
        return defaultContextSensitivity.makeNextLoopUnrollingContext(currentContext, node);
    }

    @Override
    public Context makeLoopExitContext(Context currentContext, EndLoopNode node) {
        return defaultContextSensitivity.makeLoopExitContext(currentContext, node);
    }

    @Override
    public void requestContextSensitiveParameter(Function function, String parameter) {
        defaultContextSensitivity.requestContextSensitiveParameter(function, parameter);
    }

    /**
     * Makes a function context sensitive.
     *
     * @see #makeSensitiveFromCaller(Function, int, Function)
     */
    public void makeSensitive(Function function, int sensitivity) {
        makeSensitiveFromCaller(function, sensitivity, null);
    }

    /**
     * Makes a function context sensitive, but only when it is invoked from a specific caller.
     * <p>
     * If the specific caller is null, then the function is always context sensitive.
     * <p>
     * The sensitivity flag can be:
     * <ul>
     * <li>-2: sensitive in closure variables that are context sensitive parameters of the enclosing function</li>
     * <li>-1: sensitive in the receiver (== object sensitivity)</li>
     * <li>non-negative: the position of an argument to be sensitive in</li>
     * </ul>
     *
     * @param callee      the function to make sensitive.
     * @param sensitivity indicates what kind of context sensitivity that should be used.
     * @param caller      the caller that is required for context sensitivity to be enabled.
     */
    public void makeSensitiveFromCaller(Function callee, int sensitivity, Function caller) {
        addToMapSet(sensitiveFunctions, Pair.make(caller, callee), sensitivity);
    }

    public Context makeClosureVariableContext(Function fun, Solver.SolverInterface c) {
        // "inherit" parameter sensitivity of outer function parameters that are used as closure variables
        State state = c.getState();
        Function outerFun = state.getBasicBlock().getFunction();
        Context outerFunArgs = state.getContext();
        if (outerFunArgs == null) {
            return null;
        }
        if (sensitiveFunctions.keySet().stream().noneMatch(e -> outerFun.equals(e.getSecond()))) {
            // disallow inheriting from non-customized function
            return null;
        }
        Map<String, Value> closureVariableValues = newMap();
        c.getFlowGraph().getSyntacticInformation().getClosureVariableNames(fun).forEach(n -> {
            Value parameterValue = outerFunArgs.getParameterValue(n);
            if (parameterValue != null) {
                closureVariableValues.put(n, parameterValue);
            }
        });
        if (closureVariableValues.isEmpty()) {
            return null;
        }
        return Context.makeFreeVars(closureVariableValues);
    }

    /**
     * Attempts to make a context for a call, but only for callees that have been made context sensitive.
     */
    private Context makeContextArguments(State edgeState, ObjectLabel calleeObj, Value thisval, FunctionCalls.CallInfo callInfo) {
        Function callee = calleeObj.getFunction();
        Context calleeContext = calleeObj.getHeapContext();
        Map<String, Value> closureVariables = calleeContext != null ? calleeContext.getFreeVariables() : null;
        Function caller = edgeState.getBasicBlock().getFunction();
        Pair<Function, Function> key = Pair.make(caller, callee);
        if (!sensitiveFunctions.containsKey(key)) {
            key = Pair.make(null, callee); // try the default caller
        }
        if (sensitiveFunctions.containsKey(key)) {
            return reallyMakeContextArguments(sensitiveFunctions.get(key), edgeState, callee, closureVariables, callInfo, thisval);
        }
        return null;
    }

    /**
     * Creates a context for a call.
     * The context can encode parameter-sensitivity, object-sensitivity, and closure-variable-sensitivity.
     */
    private Context reallyMakeContextArguments(Set<Integer> arguments, State edgeState, Function callee, Map<String, Value> closureVariables, FunctionCalls.CallInfo callInfo, Value thisval) {
        List<String> parameterNames = callee.getParameterNames();
        List<Value> args = newList();
        //noinspection OptionalGetWithoutIsPresent
        int max = Math.max(callee.getParameterNames().size(), arguments.stream().max(Integer::compare).get());
        for (int i = 0; i <= max; i++) {
            Value qualifier;
            if (arguments.contains(i)) { // enables parameter-sensitivity
                if (!callInfo.isUnknownNumberOfArgs() && callInfo.getNumberOfArgs() > i) {
                    qualifier = FunctionCalls.readParameter(callInfo, edgeState, i);
                } else {
                    qualifier = Value.makeUndef();
                }
            } else {
                qualifier = null;
            }
            args.add(qualifier);
        }
        Map<String, Value> nonArguments = newMap();
        if (arguments.contains(-1)) {  // enables object-sensitivity
            nonArguments.put("this", thisval);
        }
        if (arguments.contains(-2)) { // enables closure parameter sensitivity
            if (closureVariables != null) {
                closureVariables.forEach((n, v) -> {
                    if (!parameterNames.contains(n)) {
                        nonArguments.put(n, v);
                    }
                });
            }
        }
        return Context.make(null, parameterNames, args, nonArguments);
    }
}
