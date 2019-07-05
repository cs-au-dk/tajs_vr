package dk.brics.tajs.refinement.instantiations.forwards_backwards;

import dk.brics.tajs.options.ExperimentalOptions;

import java.io.Serializable;

public class RefinerOptions implements ExperimentalOptions.ExperimentalOption, Serializable {

    private static RefinerOptionValues refuterOptions;

    public static RefinerOptionValues get() {
        if(refuterOptions == null)
            refuterOptions = new RefinerOptionValues();
        return refuterOptions;
    }

    public static void set(RefinerOptionValues refOptions) {
        refuterOptions = refOptions;
    }
}
