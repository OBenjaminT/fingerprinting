import java.util.Arrays;

public class tests {
    public static void test(){
        testGetNeighbors.testAll();
        testBlackNeighbors.testAll();
        testTransitions.testAll();
    }
}

class testGetNeighbors {
    static void testAll(){
        basicFunctionality();
    }

    static void basicFunctionality(){
        boolean[][] image = {
                {false, true, false, false},
                {true, false, true, true},
                {true, false, false, true},
                {true, false, false, false}
        };

        boolean[] neighbors = fingerprinting.getNeighbors(image, 3, 1);
        assert Arrays.equals(neighbors, new boolean[]{true, false, false, false, true, false, false, false});

        // line System.out.println(neighbors); // prints a pointer/reference
        // line System.out.println(Arrays.toString(neighbors)); // actually prints content
    }
}

class testBlackNeighbors {
    static void testAll(){
        basicFunctionality();
    }

    static void basicFunctionality(){
        int black = fingerprinting.blackNeighbors(new boolean[]{true, false, false, false, true, false, false, false});
        System.out.println(black);
        assert black == 2;
    }
}

class testTransitions {
    static void testAll(){
        basicFunctionality();
    }

    static void basicFunctionality(){
        int trans = fingerprinting.transitions(new boolean[]{true, false, false, false, true, false, false, false});
        System.out.println(trans);
        assert trans == 2;
    }
}