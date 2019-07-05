package forwards_backwards_api.memory;

public interface MemoryLocationVisitor<T> {

    T visit(Property l);

    T visit(Variable l);

    T visit(Register l);
}
