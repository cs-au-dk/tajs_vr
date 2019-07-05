package forwards_backwards_api;

import dk.brics.tajs.flowgraph.AbstractNode;
import dk.brics.tajs.lattice.Context;

import java.util.Set;

import static dk.brics.tajs.util.Collections.singleton;

public class ProgramLocation {

    private final AbstractNode node;

    private final Set<Context> contexts;

    public ProgramLocation(AbstractNode node, Context context) {
        if(context == null){
            throw new IllegalArgumentException("Should not use null contexts!");
        }
        this.node = node;
        this.contexts = singleton(context);
    }

    public ProgramLocation(AbstractNode node, Set<Context> contexts) {
        if (contexts.isEmpty()) {
            throw new IllegalArgumentException("Should have at least one context");
        }

        this.node = node;
        this.contexts = contexts;
    }

    public AbstractNode getNode() {
        return node;
    }

    public Context getContext() {
        if(contexts.size() != 1) {
            throw new IllegalArgumentException("getContext can only be used on programLocations with a single context");
        }
        return contexts.iterator().next();
    }

    public Set<Context> getContexts() {
        return contexts;
    }

    @Override
    public String toString() {
        return "LOC(" + node + "line: " + node.getSourceLocation().getLineNumber() + ", " + contexts + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProgramLocation that = (ProgramLocation) o;

        if (node != null ? !node.equals(that.node) : that.node != null) return false;
        return contexts != null ? contexts.equals(that.contexts) : that.contexts == null;

    }

    @Override
    public int hashCode() {
        int result = node != null ? node.hashCode() : 0;
        result = 31 * result + (contexts != null ? contexts.hashCode() : 0);
        return result;
    }
}
