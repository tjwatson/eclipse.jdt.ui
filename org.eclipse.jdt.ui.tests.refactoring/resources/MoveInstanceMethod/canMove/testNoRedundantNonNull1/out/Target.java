package p;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault()
public class Target {

    @NonNullByDefault({})
    public class Nested {
        
    }

	public String nonstatic1(String s) {
	    return s + hashCode();
	}
}

