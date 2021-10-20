import java.util.ArrayList;
//import java.util.Scanner;

public class main_runner {
    // random code sample
    public static void main(String[] args) {
        System.out.println("Hello World");
        System.out.printf("pi = %.5f", Math.PI);
        //Scanner scanner = new Scanner(System.in);
        //String name = scanner.next();
        //System.out.println(name);
        int one, two, three = 3;
        one = two = 3;
        final int t = 3;
        StringBuilder builderConcat = new StringBuilder();
        builderConcat.append("daklfjlsa");
        System.out.println(builderConcat);
        ArrayList arr;
        if (one == two && two == three) {
            System.out.println(t);
        }
        for (int i = 0; i < 29; i++) {
            builderConcat.append(i + ", \n");
        }
        System.out.print(builderConcat + "\n");
    }
}
