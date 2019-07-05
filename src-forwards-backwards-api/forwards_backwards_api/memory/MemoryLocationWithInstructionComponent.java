package forwards_backwards_api.memory;

import forwards_backwards_api.InstructionComponent;

public interface MemoryLocationWithInstructionComponent extends MemoryLocation {

    InstructionComponent getInstructionComponent();

}
