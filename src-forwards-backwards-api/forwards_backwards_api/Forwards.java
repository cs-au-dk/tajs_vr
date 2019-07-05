package forwards_backwards_api;

import dk.brics.tajs.flowgraph.jsnodes.BinaryOperatorNode;
import dk.brics.tajs.flowgraph.jsnodes.UnaryOperatorNode;
import dk.brics.tajs.lattice.HostObject;
import dk.brics.tajs.lattice.ObjectLabel;
import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.util.Pair;
import forwards_backwards_api.memory.MemoryLocation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Query-interface a backwards analysis should implement.
 */
public interface Forwards {

    /**
     * Finds callee exits of a given call site
     * <p>
     * NB: due to implicit calls, a call site is not guaranteed to be a {@link dk.brics.tajs.flowgraph.jsnodes.CallNode}.
     *
     * @param caller the location the call occurs at
     * @return the ordinary exit program location in each callee and identifiers for invoked native functions
     */
    Pair<Set<ProgramLocation>,Set<HostObject>> getCalleeExits(ProgramLocation caller);

    Set<ProgramLocation> getCallSites(ProgramLocation callee);

    /**
     * Reads a value at a location.
     */
    Value get(ProgramLocation location, MemoryLocation memoryLocation);

    Value binop(ProgramLocation location, Value v1, BinaryOperatorNode.Op node, Value v2);

    Value unop(ProgramLocation location, UnaryOperatorNode.Op node, Value Value);

    Set<ProgramLocation> getPredecessors(ProgramLocation location, boolean loopUnrollingInsensitive);

    Properties getForInProperties(ProgramLocation location, ObjectLabel object);

    Set<ObjectLabel> getPrototypeObjects(ProgramLocation location, ObjectLabel object, Value propertyName);

    Optional<Set<Value>> inferPropertyNames(ProgramLocation location, Value base, Value targetValue, boolean usePrototypes);

    Optional<Set<Value>> inferPropertyValue(ProgramLocation location, Value base, Value propertyName, boolean usePrototypes);
}
