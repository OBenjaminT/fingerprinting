import java.util.Arrays;
import java.util.stream.IntStream;

public class fingerprinting {
    public static void main(String[] args) {
        boolean[][] image = {
                {false, true, false},
                {true, false, true},
                {true, false, false}
        };
        boolean[] neigh = getNeighbors(image, 3, 1);
        System.out.println(neigh);
        System.out.println(Arrays.toString(neigh));
        int black = blackNeighbors(neigh);
        System.out.println(black);
        int trans = transitions(neigh);
        System.out.println(trans);

        boolean[] boolList = {true, false, false, true, true, false, true}; // 2
        System.out.println(boolList[1]);
    }

    public static int transitions(boolean[] neighbors) {
        int ans = 0;
        for (int i = 0; i < neighbors.length; i++) {
            ans += (!neighbors[i] && neighbors[(i + 1)%6]) ? 1 : 0;
        }
        return ans;
    }

    public static boolean [] getNeighbors(boolean[][] image, int row, int col) {
        // turn column and row numbers into indexes
        row = row-1;
        col = col-1;

        boolean topRowInImage = (row > 0);
        boolean rightColumnInImage = (col < (image[0].length-1)); // get length of an inner list
        boolean bottomRowInImage = (row < (image.length-1));
        boolean leftColumnInImage = (col > 0);

        // index operations are the difficult part
        // remember that positive y is down
        boolean p0 = topRowInImage && image[row - 1][col];
        boolean p1 = topRowInImage && rightColumnInImage && image[row - 1][col + 1];
        boolean p2 = rightColumnInImage && image[row][col + 1];
        boolean p3 = rightColumnInImage && bottomRowInImage && image[row + 1][col + 1];
        boolean p4 = bottomRowInImage && image[row + 1][col];
        boolean p5 = bottomRowInImage && leftColumnInImage && image[row + 1][col - 1];
        boolean p6 = leftColumnInImage && image[row][col - 1];
        boolean p7 = leftColumnInImage && topRowInImage && image[row - 1][col - 1];

        return new boolean[]{p0, p1, p2, p3, p4, p5, p6, p7};
    }

    public static int blackNeighbors(boolean[] neighbors) {
        int ans = 0;
        for (boolean i : neighbors) {
            int isTrue = i ? 1 : 0;
            ans += isTrue;
        }
        return ans;
    }
}
