package forwards_backwards_api.memory;

import forwards_backwards_api.InstructionComponent;

public class RegisterWithRefinementGoal extends Register implements MemoryLocationWithInstructionComponent {

    private InstructionComponent instructionComponent;

    public RegisterWithRefinementGoal(int id, InstructionComponent ic) {
        super(id);
        this.instructionComponent = ic;
    }

    public InstructionComponent getInstructionComponent() {
        return instructionComponent;
    }

    @Override
    public String toString() {
        return "(" + getId() + "," + instructionComponent + ")";
    }
}
