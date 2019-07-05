package forwards_backwards_api;

// TODO: javadoc, explain each kind (all of them apply *after* ToObject/ToString coercion!)
public enum InstructionComponent {
    BASE,
    PROPERTY,
    TARGET,

    // TODO parametize this...
    ARG0, 
    ARG1, ARG2, ARG3, ARG4, ARG5, ARG6, ARG7, ARG8, ARG9,
    ARG10, ARG11, ARG12, ARG13, ARG14, ARG15, ARG16, ARG17, ARG18, ARG19, 
    ARG20, ARG21, ARG22, ARG23, ARG24, ARG25, ARG26, ARG27, ARG28, ARG29, 
    ARG30, ARG31, ARG32, ARG33, ARG34, ARG35, ARG36, ARG37, ARG38, ARG39, 
    ARG40, ARG41, ARG42, ARG43, ARG44, ARG45, ARG46, ARG47, ARG48, ARG49,
    ARG50, ARG51, ARG52, ARG53, ARG54, ARG55, ARG56, ARG57, ARG58, ARG59,
    ARG60, ARG61, ARG62, ARG63, ARG64, ARG65, ARG66, ARG67, ARG68, ARG69,
    ARG70, ARG71, ARG72, ARG73, ARG74, ARG75, ARG76, ARG77, ARG78, ARG79,
    ARG80, ARG81, ARG82, ARG83, ARG84, ARG85, ARG86, ARG87, ARG88, ARG89,
    ARG90, ARG91, ARG92, ARG93, ARG94, ARG95, ARG96, ARG97, ARG98, ARG99;

    public static InstructionComponent getArg(int i) {
        return InstructionComponent.valueOf("ARG" + i);
    }

    public static int getArgNumber(InstructionComponent ic) {
        if(ic.name().startsWith("ARG"))
            return Integer.valueOf(ic.name().substring(3));
        throw new UnsupportedOperationException("getArgNumber should be called with an argument InstructionComponent");
    }

    public static boolean isArgument(InstructionComponent ic) {
        return ic.name().startsWith("ARG");
    }
}
