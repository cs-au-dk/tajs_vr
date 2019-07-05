package forwards_backwards_api;

import dk.brics.tajs.analysis.Solver;
import dk.brics.tajs.flowgraph.jsnodes.BinaryOperatorNode;
import dk.brics.tajs.flowgraph.jsnodes.UnaryOperatorNode;
import dk.brics.tajs.lattice.HostObject;
import dk.brics.tajs.lattice.ObjectLabel;
import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.refinement.instantiations.forwards_backwards.TAJSForwardsAPI;
import dk.brics.tajs.util.Pair;
import forwards_backwards_api.memory.MemoryLocation;

import java.util.Set;

/**
 * Created by Barslev on 12/10/16.
 */
public class TAJSForwardsLogger extends TAJSForwardsAPI {
    @Override
    public void setSolverInterface(Solver.SolverInterface c) {
        tajsForwardsAPI.setSolverInterface(c);
    }

    @Override
    public Solver.SolverInterface getSolverInterface() {
        return tajsForwardsAPI.getSolverInterface();
    }

    @Override
    public int getNodeTransfers() {
        return tajsForwardsAPI.getNodeTransfers();
    }

    @Override
    public Set<ProgramLocation> getCallSites(ProgramLocation functionEntry) {
        return tajsForwardsAPI.getCallSites(functionEntry);
    }

    @Override
    public Pair<Set<ProgramLocation>, Set<HostObject>> getCalleeExits(ProgramLocation caller) {
        System.out.println("TAJSForwardsLogger.getCalleeExits");
        System.out.println("getCallecaller = [" + caller + "]");
        Pair<Set<ProgramLocation>, Set<HostObject>> calleeExits = tajsForwardsAPI.getCalleeExits(caller);
        System.out.println(calleeExits);
        return calleeExits;
    }

    @Override
    public Value get(ProgramLocation location, MemoryLocation memoryLocation) {
        System.out.println("TAJSForwardsLogger.get");
        System.out.println("location = [" + location + "], memoryLocation = [" + memoryLocation + "]");
        Value value = tajsForwardsAPI.get(location, memoryLocation);
        System.out.println(value);
        return value;
    }

    @Override
    public Value binop(ProgramLocation location, Value v1, BinaryOperatorNode.Op op, Value v2) {
        return tajsForwardsAPI.binop(location, v1, op, v2);
    }

    @Override
    public Value unop(ProgramLocation location, UnaryOperatorNode.Op op, Value v) {
        return tajsForwardsAPI.unop(location, op, v);
    }

    @Override
    public Set<ProgramLocation> getPredecessors(ProgramLocation location, boolean loopUnrollingInsensitive) {
        System.out.println("TAJSForwardsLogger.getPredecessors");
        System.out.println("location = [" + location + "]");
        Set<ProgramLocation> predecessors = tajsForwardsAPI.getPredecessors(location, loopUnrollingInsensitive);
        System.out.println(predecessors);
        return predecessors;
    }

    @Override
    public Set<ObjectLabel> getPrototypeObjects(ProgramLocation location, ObjectLabel object, Value propertyName) {
        System.out.println("TAJSForwardsLogger.getPrototypeObjects");
        System.out.println("location = [" + location + "], object = [" + object + "], propertyName = [" + propertyName + "]");
        Set<ObjectLabel> prototypeObjects = tajsForwardsAPI.getPrototypeObjects(location, object, propertyName);
        System.out.println("result: " + prototypeObjects);
        return prototypeObjects;
    }

    private TAJSForwardsAPI tajsForwardsAPI;

    public TAJSForwardsLogger(TAJSForwardsAPI tajsForwardsAPI) {
        this.tajsForwardsAPI = tajsForwardsAPI;
    }
}
