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

package dk.brics.tajs.monitoring.inspector.dataprocessing;

import dk.brics.tajs.analysis.Solver;
import dk.brics.tajs.flowgraph.AbstractNode;
import dk.brics.tajs.flowgraph.BasicBlock;
import dk.brics.tajs.lattice.Context;
import dk.brics.tajs.lattice.State;
import dk.brics.tajs.options.Options;

import java.util.List;
import java.util.function.Supplier;

/**
 * Computes the intermediary state between nodes in a block.
 */
public class IntermediaryStateComputer {

    private final Solver.SolverInterface c;

    public IntermediaryStateComputer(Solver.SolverInterface c) {
        this.c = c;
    }

    private <T> T withExceptionsDisabled(Supplier<T> f) {
        boolean exceptionsEnabled = !Options.get().getUnsoundness().isNoExceptions();
        Options.get().getUnsoundness().setNoExceptions(true);
        T result = f.get();
        if (exceptionsEnabled) {
            Options.get().getUnsoundness().setNoExceptions(false);
        }
        return result;
    }

    public void withTempState(AbstractNode node, State state, Runnable f) {
        withTempState(node, state, () -> {
            f.run();
            return null;
        });
    }

    public <T> T withTempState(AbstractNode node, State state, Supplier<T> f) {
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

    public State makeIntermediaryPostState(AbstractNode node, Context context) {
        BasicBlock block = node.getBlock();
        State state = c.getAnalysisLatticeElement().getState(block, context);

        if (state == null) {
            return new State(c, block);
        }
        State entryState = state.clone();
        return withTempState(block.getFirstNode(), entryState, () -> {
            List<AbstractNode> nodes = node.getBlock().getNodes();
            for (AbstractNode n : nodes) {
                c.setNode(n);
                c.getAnalysis().getNodeTransferFunctions().transfer(n);
                if (node.equals(n)) {
                    break;
                }
            }
            return c.getState();
        });
    }
}
