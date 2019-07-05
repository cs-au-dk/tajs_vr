package forwards_backwards_api.memory;

public class Register implements MemoryLocation {

    private int id;

    public Register(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Register register = (Register) o;

        return id == register.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "REG(" + id + ")";
    }

    @Override
    public <T> T accept(MemoryLocationVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public int getId() {
        return id;
    }
}
