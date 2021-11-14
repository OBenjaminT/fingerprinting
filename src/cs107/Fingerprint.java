package cs107;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Provides tools to compare fingerprint.
 */
public class Fingerprint {

    /**
     * The number of pixels to consider in each direction when doing the linear
     * regression to compute the orientation.
     */
    public static int ORIENTATION_DISTANCE = 16;

    /**
     * The maximum distance between two minutiae to be considered matching.
     */
    public static int DISTANCE_THRESHOLD = 5;

    /**
     * The number of matching minutiae needed for two fingerprints to be considered
     * identical.
     */
    public static int FOUND_THRESHOLD = 20;

    /**
     * The distance between two angle to be considered identical.
     */
    public static int ORIENTATION_THRESHOLD = 20;

    /**
     * The offset in each direction for the rotation to test when doing the
     * matching.
     */
    public static int MATCH_ANGLE_OFFSET = 2;

    /**
     * Returns an array containing the value of the 8 neighbours of the pixel at
     * coordinates <code>(row, col)</code>.
     * <p>
     * The pixels are returned such that their indices corresponds to the following
     * diagram:<br>
     * ------------- <br>
     * | 7 | 0 | 1 | <br>
     * ------------- <br>
     * | 6 | _ | 2 | <br>
     * ------------- <br>
     * | 5 | 4 | 3 | <br>
     * ------------- <br>
     * <p>
     * If a neighbours is out of bounds of the image, it is considered white.
     * <p>
     * If the <code>row</code> or the <code>col</code> is out of bounds of the
     * image, the returned value should be <code>null</code>.
     *
     * @param image nested array containing each pixel's boolean value.
     * @param row   the row of the pixel of interest, must be between
     *              <code>0</code>(included) and
     *              <code>image.length</code>(excluded).
     * @param col   the column of the pixel of interest, must be between
     *              <code>0</code>(included) and
     *              <code>image[row].length</code>(excluded).
     * @return An array containing each neighbours' value.
     */
    @Contract(value = "null, _, _ -> fail", pure = true)
    public static boolean @Nullable [] getNeighbours(boolean[][] image, int row, int col) {
        // special case that is not expected (the image is supposed to have been checked earlier)
        assert (image != null);

        // check if pixel is in the image bounds
        if (row < 0
                || row >= image.length
                || col < 0
                || col >= image[0].length
        ) return null;

        // check which of the sides of the 3x3 around the pixel are inbounds
        // @formatter:off
        boolean topRowInImage = (row > 0);
        boolean rightColumnInImage = (col < (image[0].length - 1)); // get length of an inner list (# of col)
        boolean bottomRowInImage = (row < (image.length - 1));
        boolean leftColumnInImage = (col > 0);

        // remember that positive y is down
        // for each pixel: if it's inbounds AND true, set the p-value to true
        return new boolean[]{ // p0 - p7
                topRowInImage && image[row - 1][col],
                topRowInImage && rightColumnInImage && image[row - 1][col + 1],
                rightColumnInImage && image[row][col + 1],
                rightColumnInImage && bottomRowInImage && image[row + 1][col + 1],
                bottomRowInImage && image[row + 1][col],
                bottomRowInImage && leftColumnInImage && image[row + 1][col - 1],
                leftColumnInImage && image[row][col - 1],
                leftColumnInImage && topRowInImage && image[row - 1][col - 1],
        };
        // @formatter:on
    }

    /**
     * Computes the number of black (<code>true</code>) pixels among the neighbours
     * of a pixel.
     *
     * @param neighbours array containing each pixel value. The array must respect
     *                   the convention described in
     *                   {@link #getNeighbours(boolean[][], int, int)}.
     * @return the number of black/<code>true</code> neighbours.
     */
    public static int blackNeighbours(boolean @NotNull [] neighbours) {
        return IntStream // accumulate how many neighbours are true
                .range(0, neighbours.length) // for each valid index of neighbours
                .map(index -> neighbours[index] ? 1 : 0) // if neighbours[index] is true return 1 and 0 if false
                .sum(); // sum how many 1s there are in the list
    }

    /**
     * Computes the number of white to black transitions among the neighbours of
     * pixel.
     *
     * @param neighbours array containing each pixel value. The array must respect
     *                   the convention described in
     *                   {@link #getNeighbours(boolean[][], int, int)}.
     * @return the number of white to black transitions.
     */
    public static int transitions(boolean @NotNull [] neighbours) {
        return IntStream
                .range(0, neighbours.length) // for each valid index of neighbours
                .map(index -> // turns every index into a 1 if neighbours[i] is false AND neighbours[index + 1] is true
                        !neighbours[index] && neighbours[(index + 1) % neighbours.length] ? 1 : 0
                ) // (% allows index wrap around)
                .sum(); // sum how many 1s there are in the list
    }

    /**
     * Returns <code>true</code> if the images are identical and false otherwise.
     *
     * @param image1 array containing each pixel's boolean value.
     * @param image2 array containing each pixel's boolean value.
     * @return <code>True</code> if they are identical, <code>false</code>
     * otherwise.
     */
    public static boolean identical(boolean[][] image1, boolean[][] image2) {
        // @formatter:off
        return (image1 == null && image2 == null) // if both are null they are the same
                || ((image1 != null && image2 != null) // if one is null and the other isn't, return false
                && image1.length == image2.length // they have to be the same length
                && image1[0].length == image2[0].length // in both dimensions
                && IntStream
                .range(0, image1.length) // for each valid index of image (rows)
                .parallel() // check each row simultaneously
                .noneMatch( // make sure that all rows have no differing pixels
                        row -> IntStream
                                .range(0, image1[0].length) // for each valid index of image[0] (cols)
                                .parallel() // check each pixel simultaneously
                                .anyMatch(col -> image1[row][col] != image2[row][col])
                ) // if any pixels are different -> true
        );
        // @formatter:on
    }

    /**
     * Compute the skeleton of a boolean image.
     *
     * @param image array containing each pixel's boolean value.
     * @return array containing the boolean value of each pixel of the image after
     * applying the thinning algorithm.
     */
    public static boolean @NotNull [][] thin(boolean @NotNull [][] image) {
        boolean[][] previous = new boolean[image.length][image[0].length]; // define the dimensions of the image
        do {
            for (int row = 0; row < image.length; row++) // store the image's current state
                System.arraycopy(image[row], 0, previous[row], 0, image[row].length);
            image = thinningStep(thinningStep(image, 0), 1); // run both thinning steps
        } while (!identical(previous, image)); // repeat if there was a change
        return image;
    }

    /**
     * Internal method used by {@link #thin(boolean[][])}.
     *
     * @param image array containing each pixel's boolean value.
     * @param step  the step to apply, Step 0 or Step 1.
     * @return A new array containing each pixel's value after the step.
     */
    public static boolean @NotNull [][] thinningStep(boolean @NotNull [][] image, int step) {
        boolean[][] newImage = new boolean[image.length][image[0].length];
        IntStream
                .range(0, image.length) // for each valid index of image (rows)
                .parallel() // process each row simultaneously
                .forEach(row -> IntStream
                        .range(0, image[0].length) // for each valid index of image[0] (cols)
                        .parallel() // process each column simultaneously
                        .forEach(col -> // set newImage[row][col] = to the new pixels after seeing if they should stay
                                newImage[row][col] = pixelSurvivalCriteria(image, step, row, col)
                        ));
        return newImage;
    }

    /**
     * Internal method used by {@link #thinningStep(boolean[][], int)}.
     * <p>
     * Given a particular pixel and a step, returns true if that pixel "survives" the step or not.
     *
     * @param image array containing each pixel's boolean value.
     * @param step  the step to apply, Step 0 or Step 1.
     * @param col   the column number of the pixel in question.
     * @param row   the row number of the pixel in question.
     * @return A new array containing each pixel's value after the step.
     */
    public static boolean pixelSurvivalCriteria(boolean[][] image, int step, int row, int col) {
        boolean[] neighbours = getNeighbours(image, row, col); // simplifies the logic below
        // @formatter:off
        return image[row][col] // if the pixel is black it stays black (image[i][j] && ...) == false
                && !(neighbours != null // if all the below is true the pixel should be false -> !(...)
                && blackNeighbours(neighbours) > 1
                && blackNeighbours(neighbours) < 7
                && transitions(neighbours) == 1
                && (step == 0 // either the step0 conditions are true
                && (!neighbours[0] || !neighbours[2] || !neighbours[4])
                && (!neighbours[2] || !neighbours[4] || !neighbours[6])
                || step == 1 // or the step1 conditions are true
                && (!neighbours[0] || !neighbours[2] || !neighbours[6])
                && (!neighbours[0] || !neighbours[4] || !neighbours[6])
        ));
        // @formatter:on
    }

    /**
     * Computes all pixels that are connected to the pixel at coordinate
     * <code>(row, col)</code> and within the given distance of the pixel.
     *
     * @param image    array containing each pixel's boolean value.
     * @param row      the first coordinate of the pixel of interest.
     * @param col      the second coordinate of the pixel of interest.
     * @param distance the maximum distance at which a pixel is considered.
     * @return An array where <code>true</code> means that the pixel is within
     * <code>distance</code> and connected to the pixel at
     * <code>(row, col)</code>.
     */
    public static boolean @NotNull [][] connectedPixels(boolean[][] image, int row, int col, int distance) {
        int squareSideLength = (2 * distance) + 1;
        // create a square clone of a subset of the image centered on the pixel and squareSideLength wide
        var clone = subClone(
                image,
                row - distance,// + 1, // new center of sub image
                col - distance ,// - 1, // WTF?
                squareSideLength
        );
        // set an empty array with the same center pixel as in clone
        var relevant = new boolean[squareSideLength][squareSideLength];
        relevant[distance][distance] = image[row][col];

        boolean[][] previousArray; // declared outside of loop scope so that it can be used in the check
        do {
            previousArray = ArrayCloneSquare(relevant, squareSideLength); // clone relevant to remember it
            IntStream
                    .range(0, squareSideLength) // for each row
                    .parallel()
                    .forEach(rowIndex -> IntStream
                            .range(0, squareSideLength) // for each column
                            .parallel()
                            .forEach(colIndex -> // make every pixel "infect" its true neighbours if it is true
                                    spreadPixel(clone, relevant, rowIndex, colIndex)
                            ));
        } while (!identical(previousArray, relevant)); // repeat if there was a change
        return relevant;
    }

    static boolean @NotNull [][] ArrayCloneSquare(boolean[][] image, int width) {
        return subClone(image, 0, 0, width); // preset to clone a square array
    }

    static boolean @NotNull [][] subClone(boolean[][] image, int topLeftRow, int topLeftCol, int width) {
        boolean[][] clone = new boolean[width][width];
        IntStream
                .range(0, width) // for each row
                .map(row -> row + topLeftRow) // shift to the location in the original
                .forEach(row -> IntStream
                        .range(0, width) // for each column
                        .map(col -> col + topLeftCol) // shift to the location in the original
                        // keep only those that are actually in the image
                        .filter(col -> row >= 0
                                && row < image.length
                                && col >= 0
                                && col < image[0].length
                        )
                        // assign the values to the clone's equivalent
                        .forEach(col -> clone[row - topLeftRow][col - topLeftCol] = image[row][col]));
        return clone;
    }


    static void spreadPixel(boolean[][] imageSubset, boolean[][] subsetClone, int row, int col) {
        // check if pixel is in the image bounds
        if (row < 0
                || row >= imageSubset.length
                || col < 0
                || col >= imageSubset[0].length
        ) return;

        if (!subsetClone[row][col]) return; // if the pixel is false it doesn't spread

        // check which of the sides of the 3x3 around the pixel are inbounds
        // @formatter:off
        boolean topRowInImage = (row > 0);
        boolean rightColumnInImage = (col < (imageSubset[0].length -1));
        boolean bottomRowInImage = (row < (imageSubset.length -1));
        boolean leftColumnInImage = (col > 0);

        // for each pixel: if it's inbounds and true in the original image, make it true
        // if statements instead of ternary operators (as in getNeighbours) because the subsetClone
        // indexes also have to be inbounds
        if (topRowInImage) subsetClone[row - 1][col] = imageSubset[row - 1][col];
        if (topRowInImage && rightColumnInImage) subsetClone[row - 1][col + 1] = imageSubset[row - 1][col + 1];
        if (rightColumnInImage) subsetClone[row][col + 1] = imageSubset[row][col + 1];
        if (rightColumnInImage && bottomRowInImage) subsetClone[row + 1][col + 1] = imageSubset[row + 1][col + 1];
        if (bottomRowInImage) subsetClone[row + 1][col] = imageSubset[row + 1][col];
        if (bottomRowInImage && leftColumnInImage) subsetClone[row + 1][col - 1] = imageSubset[row + 1][col - 1];
        if (leftColumnInImage) subsetClone[row][col - 1] = imageSubset[row][col - 1];
        if (leftColumnInImage && topRowInImage) subsetClone[row - 1][col - 1] = imageSubset[row - 1][col - 1];
        // @formatter:on
        // it changed the pixels in place so no return
    }

    /**
     * Computes the slope of a minutia using linear regression.
     *
     * @param connectedPixels the result of
     *                        {@link #connectedPixels(boolean[][], int, int, int)}.
     * @param row             the row of the minutia.
     * @param col             the col of the minutia.
     * @return the slope.
     */
    public static double computeSlope(boolean @NotNull [][] connectedPixels, int row, int col) {
        var xValues = new ArrayList<Integer>();
        var yValues = new ArrayList<Integer>();

        for (int i = 0; i < connectedPixels.length; i++) // for each pixel
            for (int j = 0; j < connectedPixels[0].length; j++)
                if (connectedPixels[i][j] && !(i == row && j == col)) { // if it's true and not the origin
                    int x = j - col; // make its coordinates relative to the new origin
                    int y = row - i;
                    xValues.add(x); // add it to the list
                    yValues.add(y);
                }

        /* TODO
        IntStream
            .range(0, connectedPixels.length) // for each row
            .forEach(rowIndex -> IntStream
                .range(0, connectedPixels[0].length) // for each column
                // only if the pixel is true and not the coordinates of the minutia in question
                .filter(colIndex -> connectedPixels[rowIndex][colIndex] && !(rowIndex == row && colIndex == col))
                .forEach(colIndex -> { // add an adjusted value to the list
                    xValues.add(colIndex - col);
                    yValues.add(row - rowIndex);
                }));
         */

        double xySum = IntStream
                .range(0, xValues.size()) // for each index of xValues
                .mapToDouble(i -> xValues.get(i) * yValues.get(i)) // multiply it with its equivalent in yValues
                .sum(); // sum them up
        double xSquared = xValues.stream() // for each value of xValues
                .mapToDouble(i -> i * i) // square it
                .sum(); // sum the squares
        double ySquared = yValues.stream() // for each value of yValues
                .mapToDouble(i -> i * i) // square it
                .sum(); // sum the squares

        // @formatter:off
        if (xSquared == 0) return Double.POSITIVE_INFINITY; // if vertical return infinity
        if (xSquared >= ySquared) return xySum / xSquared;
        else return ySquared / xySum;
        // @formatter:on
    }

    /**
     * Computes the orientation of a minutia in radians.
     *
     * @param connectedPixels the result of
     *                        {@link #connectedPixels(boolean[][], int, int, int)}.
     * @param row             the row of the minutia.
     * @param col             the col of the minutia.
     * @param slope           the slope as returned by
     *                        {@link #computeSlope(boolean[][], int, int)}.
     * @return the orientation of the minutia in radians.
     */
    public static double computeAngle(boolean @NotNull [][] connectedPixels, int row, int col, double slope) {
        // atomics because they're used in a lambda expression below
        var pixelsAbove = new AtomicInteger();
        var pixelsUnder = new AtomicInteger();
        IntStream
                .range(0, connectedPixels.length) // for each row
                .forEach(rowIndex -> IntStream
                        .range(0, connectedPixels[0].length) // for each column
                        // only if the pixel is true and not the origin
                        .filter(colIndex ->
                                connectedPixels[rowIndex][colIndex]
                                        && !(rowIndex == row && colIndex == col)
                        )
                        .forEach(colIndex -> {
                            int x = colIndex - col; // make its coordinates relative to the new origin
                            int y = row - rowIndex;
                            if (y >= (-1 / slope) * x)
                                pixelsAbove.getAndIncrement(); // if it's above the normal increment pixelsAbove
                            else
                                pixelsUnder.getAndIncrement(); // if not increment pixels below
                        }));

        if (slope == Double.POSITIVE_INFINITY) // if the line is vertical
            return (pixelsAbove.get() > pixelsUnder.get() ? Math.PI : -Math.PI) / 2; // return either up or down

        double angle = Math.atan(slope);
        if ((angle > 0 && pixelsUnder.get() > pixelsAbove.get()) // if it's going up and there are more under the line than not
                || (angle < 0 && pixelsUnder.get() < pixelsAbove.get()) // or going down and more over than under
        ) angle += Math.PI; // flip the angle
        return angle;
    }

    /**
     * Computes the orientation of the minutia that the coordinate <code>(row,
     * col)</code>.
     *
     * @param image    array containing each pixel's boolean value.
     * @param row      the first coordinate of the pixel of interest.
     * @param col      the second coordinate of the pixel of interest.
     * @param distance the distance to be considered in each direction to compute
     *                 the orientation.
     * @return The orientation in degrees.
     */
    public static int computeOrientation(boolean[][] image, int row, int col, int distance) {
        var connectedPixels = connectedPixels(image, row, col, distance);
        var slope = computeSlope(connectedPixels, distance, distance);
        var angle = computeAngle(connectedPixels, distance, distance, slope);
        var angleDegrees = (int) Math.round(Math.toDegrees(angle));
        return angleDegrees < 0 ? angleDegrees + 360 : angleDegrees;
    }

    /**
     * Extracts the minutiae from a thinned image.
     *
     * @param image array containing each pixel's boolean value.
     * @return The list of all minutiae. A minutia is represented by an array where
     * the first element is the row, the second is column, and the third is
     * the angle in degrees.
     * @see #thin(boolean[][])
     */
    public static @NotNull List<int[]> extract(boolean[][] image) {
        var minutiaes = new ArrayList<int[]>();
        IntStream
                .range(1, image.length - 1) // for each pixel excluding the outer edge
                .forEach(row -> IntStream
                        .range(1, image[0].length - 1)
                        .filter(col -> image[row][col]) // if it's part of the fingerprint (i.e. true)
                        .filter(col -> { // if it's a minutia
                            var neighbors = getNeighbours(image, row, col);
                            assert neighbors != null;
                            var transitions = transitions(neighbors);
                            return transitions == 3 || transitions == 1;
                        })
                        .forEach(col -> // add it to the list
                                minutiaes.add(new int[]{
                                        row,
                                        col,
                                        computeOrientation(image, row, col, ORIENTATION_DISTANCE)
                                })
                        ));
        System.out.println("len" + minutiaes.size());
        return minutiaes;
    }

    /**
     * Applies the specified rotation to the minutia.
     *
     * @param minutia   the original minutia.
     * @param centerRow the row of the center of rotation.
     * @param centerCol the col of the center of rotation.
     * @param rotation  the rotation in degrees.
     * @return the minutia rotated around the given center.
     */
    @Contract("_, _, _, _ -> new")
    public static int @NotNull [] applyRotation(int @NotNull [] minutia, int centerRow, int centerCol, int rotation) {
        // center on new origin
        int x = minutia[1] - centerCol;
        int y = centerRow - minutia[0];
        // convert to radians, then compute sin/cos
        double rotationRad = Math.toRadians(rotation);
        double sinRot = Math.sin(rotationRad);
        double cosRot = Math.cos(rotationRad);
        // formula
        int newRow = (int) Math.round(centerRow - (x * sinRot + y * cosRot));
        int newCol = (int) Math.round(centerCol + (x * cosRot - y * sinRot));
        int newOrientation = (minutia[2] + rotation) % 360;
        return new int[]{newRow, newCol, newOrientation};
    }

    /**
     * Applies the specified translation to the minutia.
     *
     * @param minutia        the original minutia.
     * @param rowTranslation the translation along the rows.
     * @param colTranslation the translation along the columns.
     * @return the translated minutia.
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static int @NotNull [] applyTranslation(int @NotNull [] minutia, int rowTranslation, int colTranslation) {
        int newRow = minutia[0] - rowTranslation;
        int newCol = minutia[1] - colTranslation;
        return new int[]{newRow, newCol, minutia[2]};
    }

    /**
     * Computes the row, column, and angle after applying a transformation
     * (translation and rotation).
     *
     * @param minutia        the original minutia.
     * @param centerCol      the column around which the point is rotated.
     * @param centerRow      the row around which the point is rotated.
     * @param rowTranslation the vertical translation.
     * @param colTranslation the horizontal translation.
     * @param rotation       the rotation.
     * @return the transformed minutia.
     */
    public static int @NotNull [] applyTransformation(int[] minutia,
                                                      int centerRow,
                                                      int centerCol,
                                                      int rowTranslation,
                                                      int colTranslation,
                                                      int rotation) {
        return applyTranslation(
                applyRotation(minutia, centerRow, centerCol, rotation),
                rowTranslation,
                colTranslation
        );
    }

    /**
     * Computes the row, column, and angle after applying a transformation
     * (translation and rotation) for each minutia in the given list.
     *
     * @param minutiae       the list of minutiae.
     * @param centerCol      the column around which the point is rotated.
     * @param centerRow      the row around which the point is rotated.
     * @param rowTranslation the vertical translation.
     * @param colTranslation the horizontal translation.
     * @param rotation       the rotation.
     * @return the list of transformed minutiae.
     */
    public static List<int[]> applyTransformation(@NotNull List<int[]> minutiae,
                                                  int centerRow,
                                                  int centerCol,
                                                  int rowTranslation,
                                                  int colTranslation,
                                                  int rotation) {
        return minutiae.stream().parallel() // for each minutia transform it
                .map(i -> applyTransformation(i, centerRow, centerCol, rowTranslation, colTranslation, rotation))
                .collect(Collectors.toList()); // convert to a list again
    }

    /**
     * Counts the number of overlapping minutiae.
     *
     * @param minutiae1      the first set of minutiae.
     * @param minutiae2      the second set of minutiae.
     * @param maxDistance    the maximum distance between two minutiae to consider
     *                       them as overlapping.
     * @param maxOrientation the maximum difference of orientation between two
     *                       minutiae to consider them as overlapping.
     * @return the number of overlapping minutiae.
     */
    public static int matchingMinutiaeCount(@NotNull List<int[]> minutiae1,
                                            List<int[]> minutiae2,
                                            int maxDistance,
                                            int maxOrientation) {
       /* return (int) minutiae1.stream()//.parallel() // for each minutia in minutiae1
                .filter(minutia1 -> minutiae2.stream()//.parallel()
                        .anyMatch(minutia2 -> // keep it if there is any in minutiae2 that is true below
                        { // keep it if there is any in minutiae2 that is true below
                            var a = minutia1[0] - minutia2[0]; // Pythagoras
                            var b = minutia1[1] - minutia2[1];
                            return Math.sqrt(a * a + b * b) <= maxDistance
                                    && Math.abs(minutia1[2] - minutia2[2]) <= maxOrientation;
                        })
                ).count();*/
        ArrayList<Integer[]> minutiaes = new ArrayList<Integer[]>(); //list of minutiaes that matched
        ArrayList<Integer[]> minutiaes2 = new ArrayList<Integer[]>(); //list of minutiaes that matched
        int count = 0;
        for (int i = 0; i < minutiae1.size(); ++i) {
            int j = 0;
            boolean bol = true;
            while (bol && j < minutiae2.size()) {
                Integer[] minutiae = new Integer[3];
                Integer[] minutiae28 = new Integer[3];//contains the row and column of the minutiae 1 that mathched
                if (Math.sqrt((minutiae1.get(i)[0] - minutiae2.get(j)[0]) * (minutiae1.get(i)[0] - minutiae2.get(j)[0]) + (minutiae1.get(i)[1] - minutiae2.get(j)[1]) * (minutiae1.get(i)[1] - minutiae2.get(j)[1])) <= maxDistance
                        && Math.abs(minutiae1.get(i)[2] - minutiae2.get(j)[2]) <= maxOrientation) {
                    ++count;
                    bol = false;
                    minutiae[0] = minutiae1.get(i)[0];
                    minutiae[1] = minutiae1.get(i)[1];
                    minutiae[2] = minutiae1.get(i)[2];
                    minutiaes.add(minutiae);
                    minutiae28[0] = minutiae2.get(j)[0];
                    minutiae28[1] = minutiae2.get(j)[1];
                    minutiae28[2] = minutiae2.get(j)[2];
                    minutiaes2.add(minutiae28);
                }
                ++j;
            }
        }
        //uncomment this to check what minutiae matched and how many of them
/*        if (count >= 15) {
            System.out.println(count);
            for (int i = 0; i < minutiaes.size(); ++i) {
                System.out.println(minutiaes.get(i)[0]
                        + "\t-\t" + minutiaes.get(i)[1]
                        + "\t-\t" + minutiaes.get(i)[2] + "\t//\t"
                        + minutiaes2.get(i)[0] + "\t-\t"
                        + minutiaes2.get(i)[1] + "\t-\t"
                        + minutiaes2.get(i)[2]
                );
            }

        }*/
        return count;
    }

    /**
     * Compares the minutiae from two fingerprints.
     *
     * @param minutiae1 the list of minutiae of the first fingerprint.
     * @param minutiae2 the list of minutiae of the second fingerprint.
     * @return Returns <code>true</code> if they match and <code>false</code>
     * otherwise.
     */
    public static boolean match(@NotNull List<int[]> minutiae1, List<int[]> minutiae2) {
        return minutiae1.stream().parallel() // for all minutia1
                .anyMatch(min1 -> minutiae2.stream().parallel() // is there any in minutia2 where
                        .anyMatch(min2 -> IntStream // in the angle_offset range
                                .rangeClosed(
                                        (min2[2] - min1[2]) - MATCH_ANGLE_OFFSET,
                                        (min2[2] - min1[2]) + MATCH_ANGLE_OFFSET
                                ).parallel()
                                .anyMatch(rotation -> // is any transformation similar by >= threshold amount
                                        matchingMinutiaeCount(
                                                minutiae1,
                                                applyTransformation(
                                                        minutiae2,
                                                        min1[0],
                                                        min1[1],
                                                        min2[0] - min1[0],
                                                        min2[1] - min1[1],
                                                        rotation
                                                ),
                                                DISTANCE_THRESHOLD,
                                                ORIENTATION_THRESHOLD
                                        ) >= FOUND_THRESHOLD)));
    }
/*    public static boolean match(List<int[]> minutiae1, List<int[]> minutiae2) {
        //TODO implement
        boolean resultat = false;
        List<int[]> Minutiae2trans = new ArrayList<int[]>();

        for(int[] a : minutiae1) {
            for(int[] b : minutiae2) {
                int ligM1 = a[0];
                int colM1 = a[1];
                int ligM2 = b[0];
                int colM2 = b[1];
                int rot = b[2]-a[2];

                for (int p = rot-MATCH_ANGLE_OFFSET ; p<=rot+MATCH_ANGLE_OFFSET ; p+=1) {
                    Minutiae2trans.clear();
                    Minutiae2trans = applyTransformation(minutiae2 , ligM1 , colM1 , ligM2-ligM1 , colM2-colM1 , p);
                    //System.out.println(matchingMinutiaeCount(minutiae1, Minutiae2trans, DISTANCE_THRESHOLD, ORIENTATION_THRESHOLD));
                    if (matchingMinutiaeCount(minutiae1, Minutiae2trans, DISTANCE_THRESHOLD, ORIENTATION_THRESHOLD) >= FOUND_THRESHOLD) {
                        int count = matchingMinutiaeCount(minutiae1, Minutiae2trans, DISTANCE_THRESHOLD, ORIENTATION_THRESHOLD);
                        resultat = true;
                    }
                }
            }
        }


        return resultat;
    }*/
/*public static boolean match(List<int[]> minutiae1, List<int[]> minutiae2) {
    //TODO implement
    boolean resultat = false;
    List<int[]> Minutiae2trans = new ArrayList<int[]>();
    int temp = 0;
    for(int[] a : minutiae1) {
        for(int[] b : minutiae2) {
            int ligM1 = a[0];
            int colM1 = a[1];
            int ligM2 = b[0];
            int colM2 = b[1];
            int rot = b[2]-a[2];

            for (int p = rot-MATCH_ANGLE_OFFSET ; p<=rot+MATCH_ANGLE_OFFSET ; p+=1) {
                Minutiae2trans.clear();
                Minutiae2trans = applyTransformation(minutiae2 , ligM1 , colM1 , ligM2-ligM1 , colM2-colM1 , p);
                //System.out.println(matchingMinutiaeCount(minutiae1, Minutiae2trans, DISTANCE_THRESHOLD, ORIENTATION_THRESHOLD));
                if (matchingMinutiaeCount(minutiae1, Minutiae2trans, DISTANCE_THRESHOLD, ORIENTATION_THRESHOLD) >= FOUND_THRESHOLD) {
                    int count = matchingMinutiaeCount(minutiae1, Minutiae2trans, DISTANCE_THRESHOLD, ORIENTATION_THRESHOLD);
                    if (temp <= count) {
                        temp = count;
                    }
                    resultat = true;
                }
            }
        }
    }

    System.out.println(temp);
    return resultat;
}*/
}