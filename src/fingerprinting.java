/**
 * - Task 1: Skeletonization
 * -- Preliminary methods
 * --- Method `getNeighbors`
 * --- Method `blackNeighbors`
 * --- Method `transitions`
 * --- Method `identical`
 * -- Main methods
 * - Task 2: Locate and calculate the orientation
 * -- Function `connectedPixels`
 * -- Function `computeSlope`
 * -- Function `computeAngle`
 * -- Function `computeOrientation`
 * -- Function `extract`
 * - Task 3: Comparison
 * -- Presentation of the algorithm
 * -- Method `applyTransformation`
 * -- Method `matchingMinutiaeCount`
 * -- Method `match`
 */
public class fingerprinting {
    public static void main(String[] args) {
        tests.test();
    }

    /**
     * Given a pixel `P` in a 3x3 grid of pixels, return an array of its neighbors
     * `[P0, P1, P2, ..., P7]` as defined below.
     * <p>
     * Checks:
     * - Is the pixel being checked in the image?
     * - Assume it is. i.e. don't check
     * - Are any of its neighbors outside of the image?
     * - If they are, return `false`.
     * <p>
     * +----+----+----+
     * | P7 | P0 | P1 |
     * +----+----+----+
     * | P6 | P  | P2 |
     * +----+----+----+
     * | P5 | P4 | P3 |
     * +----+----+----+
     *
     * @param image Back and white bitmap of a fingerprint.
     * @param row   The row number (not index!) of the pixel you want the neighbors of.
     * @param col   The column number (not index!) of the pixel you want the neighbors of.
     * @return The list of the values of the pixels neighboring the pixel in question, starting above and going clockwise.
     */
    public static boolean[] getNeighbors(boolean[][] image, int row, int col) {
        // turn column and row numbers into indexes
        row = row - 1;
        col = col - 1;

        boolean topRowInImage = (row > 0);
        boolean rightColumnInImage = (col < (image[0].length - 1)); // get length of an inner list
        boolean bottomRowInImage = (row < (image.length - 1));
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

    /**
     * @param neighbors Output of `getNeighbors`.
     * @return Then number of neighbors that are black/`true`.
     */
    public static int blackNeighbors(boolean[] neighbors) {
        int ans = 0;
        for (boolean i : neighbors) {
            int isTrue = i ? 1 : 0;
            ans += isTrue;
        }
        return ans;
    }

    /**
     * @param neighbors Output of `getNeighbors`
     * @return How many `false -> true` transitions there are.
     */
    public static int transitions(boolean[] neighbors) {
        int ans = 0;
        for (int i = 0; i < neighbors.length; i++) {
            ans += (!neighbors[i] && neighbors[(i + 1) % 6]) ? 1 : 0;
        }
        return ans;
    }
}
