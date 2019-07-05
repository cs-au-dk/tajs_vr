package forwards_backwards_api.memory;

import dk.brics.tajs.flowgraph.Function;

import java.util.Optional;

public class Variable implements MemoryLocation {

    private final String name;
    private Optional<Function> scope;

    public Variable(String name) {
        this.name = name;
        this.scope = Optional.empty();
    }

    public Variable(String name, Function scope) {
        this.name = name;
        this.scope = Optional.of(scope);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Variable variable = (Variable) o;

        return name != null ? name.equals(variable.name) : variable.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "VAR(" + name + ")";
    }

    @Override
    public <T> T accept(MemoryLocationVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public String getName() {
        return name;
    }

    public Optional<Function> getScope() {
        return scope;
    }
}
