package forwards_backwards_api.memory;

import dk.brics.tajs.lattice.ObjectLabel;
import dk.brics.tajs.lattice.Value;
import forwards_backwards_api.InstructionComponent;

public class PropertyWithRefinementGoal extends Property implements MemoryLocationWithInstructionComponent {

    private InstructionComponent instructionComponent;

    public PropertyWithRefinementGoal(ObjectLabel base, Value name, InstructionComponent ic) {
        super(base, name);
        this.instructionComponent = ic;
    }

    public InstructionComponent getInstructionComponent() {
        return instructionComponent;
    }

    @Override
    public String toString() {
        return "RegisterWithRefinementGoal{" +
                "base: " + super.getObject().toString() + ", " +
                "propName: " + super.getName().toString() + ", " +
                "instructionComponent=" + instructionComponent +
                '}';
    }
}
