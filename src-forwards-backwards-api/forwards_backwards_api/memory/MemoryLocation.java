package forwards_backwards_api.memory;

public interface MemoryLocation {

    <T> T accept(MemoryLocationVisitor<T> visitor);
}
