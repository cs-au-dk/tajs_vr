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

package dk.brics.tajs.flowgraph.syntaticinfo;

import com.google.javascript.jscomp.parsing.parser.trees.ParseTree;
import dk.brics.tajs.flowgraph.AbstractNode;
import dk.brics.tajs.flowgraph.Function;
import dk.brics.tajs.flowgraph.jsnodes.CallNode;
import dk.brics.tajs.flowgraph.jsnodes.IfNode;
import dk.brics.tajs.monitoring.inspector.util.OccurenceCountingMap;
import dk.brics.tajs.util.AnalysisException;
import dk.brics.tajs.util.Pair;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static dk.brics.tajs.util.Collections.addAllToMapSet;
import static dk.brics.tajs.util.Collections.addToMapSet;
import static dk.brics.tajs.util.Collections.newMap;
import static dk.brics.tajs.util.Collections.newSet;

/**
 * Syntactic information that eventually is used in {@link SyntacticQueries}.
 */
public class RawSyntacticInformation {

    private final Map<AbstractNode, Set<String>> variableDependencies;

    private final Set<Function> correlatedAccessFunctions;

    private final Set<AbstractNode> inForIn;

    private final Map<Function, Set<String>> loopVariables;

    private final Set<CallNode> tajsCallsWithLiteralFalseAsFirstOrFourthArgument;

    private final Map<AbstractNode, SyntacticReference> nodeWithBaseReferences;

    private final Map<ParseTree, SyntacticReference> simpleReads;

    private final Map<ParseTree, Integer> expressionRegisters;

    private final Map<IfNode, Set<String>> conditionRefined1ArgumentVariables;

    private final Map<IfNode, Set<String>> conditionRefinedArgumentVariables;

    private final Map<Function, Set<String>> stackVariables;

    private final Map<Function, Set<String>> functionClosureVariables;

    private final Map<Pair<Function, Integer>, String> variableNameFromFuncAndReg;

    private final OccurenceCountingMap<Pair<Function, String>> numberOfWritesToVariablesInFunction;

    private final OccurenceCountingMap<Pair<Function, String>> numberOfWritesToVariablesInnerFunctions;

    private final Set<Function> functionsWithThisReference;

    private final Map<Function, Set<String>> functionClosureVariablesTransitively;

    public RawSyntacticInformation() {
        this.variableDependencies = newMap();
        this.correlatedAccessFunctions = newSet();
        this.inForIn = newSet();
        this.loopVariables = newMap();
        this.tajsCallsWithLiteralFalseAsFirstOrFourthArgument = newSet();
        this.nodeWithBaseReferences = newMap();
        this.simpleReads = newMap();
        this.expressionRegisters = newMap();
        this.conditionRefined1ArgumentVariables = newMap();
        this.conditionRefinedArgumentVariables = newMap();
        this.stackVariables = newMap();
        this.functionClosureVariables = newMap();
        this.variableNameFromFuncAndReg = newMap();
        this.numberOfWritesToVariablesInFunction = new OccurenceCountingMap();
        this.numberOfWritesToVariablesInnerFunctions = new OccurenceCountingMap();
        this.functionsWithThisReference = newSet();
        this.functionClosureVariablesTransitively = newMap();
    }

    public Map<AbstractNode, Set<String>> getVariableDependencies() {
        return variableDependencies;
    }

    public Set<Function> getCorrelatedAccessFunctions() {
        return correlatedAccessFunctions;
    }

    public Set<AbstractNode> getInForIn() {
        return inForIn;
    }

    public Map<Function, Set<String>> getLoopVariables() {
        return loopVariables;
    }

    public Set<CallNode> getTajsCallsWithLiteralFalseAsFirstOrFourthArgument() {
        return tajsCallsWithLiteralFalseAsFirstOrFourthArgument;
    }

    public Map<AbstractNode, SyntacticReference> getNodeWithBaseReferences() {
        return nodeWithBaseReferences;
    }

    public Map<ParseTree, SyntacticReference> getSimpleReads() {
        return simpleReads;
    }

    public Map<ParseTree, Integer> getExpressionRegisters() {
        return expressionRegisters;
    }

    public Map<IfNode, Set<String>> getConditionRefined1ArgumentVariables() {
        return conditionRefined1ArgumentVariables;
    }

    public Map<IfNode, Set<String>> getConditionRefinedArgumentVariables() {
        return conditionRefinedArgumentVariables;
    }

    public Map<Function, Set<String>> getStackVariables() {
        return stackVariables;
    }

    public Map<Function, Set<String>> getFunctionClosureVariables() {
        return functionClosureVariables;
    }

    public OccurenceCountingMap<Pair<Function, String>> getVariableWritesInFunction() {
        return numberOfWritesToVariablesInFunction;
    }

    public OccurenceCountingMap<Pair<Function, String>> getVariableWritesInnerFunction() {
        return numberOfWritesToVariablesInnerFunctions;
    }

    public void registerVariableWriteInFunction(Function function, String variableName) {
        numberOfWritesToVariablesInFunction.count(Pair.make(function, variableName));
    }

    public void registerVariableWriteInnerFunction(Function function, String variableName) {
        numberOfWritesToVariablesInnerFunctions.count(Pair.make(function, variableName));
    }

    public Map<Pair<Function, Integer>, String> getVariableReads() {
        return variableNameFromFuncAndReg;
    }

    public void registerVariableRead(Function function, int reg, String variableName) {
        variableNameFromFuncAndReg.put(Pair.make(function, reg), variableName);
    }

    public Set<Function> getFunctionsWithThisReference() {
        return functionsWithThisReference;
    }

    public Map<Function, Set<String>> getFunctionClosureVariablesTransitively() {
        return functionClosureVariablesTransitively;
    }

    /**
     * Registers that fun has the given closure variables.
     */
    public void registerFunctionClosureVariables(Function fun, Set<String> varnames) {
        if (varnames == null) {
            return;
        }
        functionClosureVariables.put(fun, varnames);
        for (String varname : varnames) {
            Function currentFunction = fun;

            Map<Function, Set<String>> resForVarname = newMap();
            while (!(currentFunction == null || currentFunction.getVariableNames().contains(varname) || currentFunction.getParameterNames().contains(varname))) {
                addToMapSet(resForVarname, currentFunction, varname);
                currentFunction = currentFunction.getOuterFunction();
            }
            if (currentFunction != null) {
                resForVarname.entrySet().forEach(e -> addAllToMapSet(functionClosureVariablesTransitively, e.getKey(), e.getValue()));
            }
        }
    }

    public SyntacticQueries getQueryView() {
        return new QueryView();
    }

    /**
     * A query view of the raw information.
     */
    private class QueryView implements SyntacticQueries {

        @Override
        public boolean isLoopVariable(Function function, String variable) {
            return loopVariables.containsKey(function) && loopVariables.get(function).contains(variable);
        }

        @Override
        public boolean doesLiteralReferenceParameter(AbstractNode literalAllocationNode, String parameterName) {
            return variableDependencies.containsKey(literalAllocationNode) && variableDependencies.get(literalAllocationNode).contains(parameterName);
        }

        @Override
        public boolean isInForIn(AbstractNode node) {
            return inForIn.contains(node);
        }

        @Override
        public boolean isCorrelatedAccessFunction(Function function) {
            return correlatedAccessFunctions.contains(function);
        }

        @Override
        public Set<CallNode> getTajsCallsWithLiteralFalseAsFirstOrFourthArgument() {
            return tajsCallsWithLiteralFalseAsFirstOrFourthArgument;
        }

        @Override
        public boolean isSimpleRead(ParseTree tree) {
            return simpleReads.containsKey(tree);
        }

        @Override
        public SyntacticReference getSimpleRead(ParseTree tree) {
            return simpleReads.get(tree);
        }

        @Override
        public int getExpressionRegister(ParseTree tree) {
            if (!expressionRegisters.containsKey(tree)) {
                throw new AnalysisException("No register associated with tree: " + tree + "?!?");
            }
            return expressionRegisters.get(tree);
        }

        @Override
        public boolean isStackVariable(Function function, String variableName) {
            return stackVariables.getOrDefault(function, newSet()).contains(variableName);
        }

        @Override
        public Set<String> getClosureVariableNames(Function function) {
            return functionClosureVariables.getOrDefault(function, newSet());
        }

        @Override
        public boolean isFunctionWithThisReference(Function function) {
            return functionsWithThisReference.contains(function);
        }

        @Override
        public int getNumberOfVariableWritesInFunction(Function function, String variableName) {
            return numberOfWritesToVariablesInFunction.getResult(Pair.make(function, variableName));
        }

        @Override
        public int getNumberOfVariableWritesInnerFunction(Function function, String variableName) {
            return numberOfWritesToVariablesInnerFunctions.getResult(Pair.make(function, variableName));
        }

        @Override
        public Optional<String> getVariableNameFromReg(Function function, int reg) {
            String res = variableNameFromFuncAndReg.get(Pair.make(function, reg));
            return res != null ? Optional.of(res) : Optional.empty();
        }

        @Override
        public Set<String> getClosureVariableNamesTransitively(Function function) {
            return functionClosureVariablesTransitively.getOrDefault(function, newSet());
        }
    }
}
