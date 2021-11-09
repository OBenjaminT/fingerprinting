package cs107;

/*
- java.util.ArrayList;
- java.util.Arrays;
*/

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Provides tools to compare fingerprint.
 */
@SuppressWarnings("unused")
public class Fingerprint {

    /**
     * The number of pixels to consider in each direction when doing the linear
     * regression to compute the orientation.
     */
    public static final int ORIENTATION_DISTANCE = 16;

    /**
     * The maximum distance between two minutiae to be considered matching.
     */
    public static final int DISTANCE_THRESHOLD = 5;

    /**
     * The number of matching minutiae needed for two fingerprints to be considered
     * identical.
     */
    public static final int FOUND_THRESHOLD = 20;

    /**
     * The distance between two angle to be considered identical.
     */
    public static final int ORIENTATION_THRESHOLD = 20;

    /**
     * The offset in each direction for the rotation to test when doing the
     * matching.
     */
    public static final int MATCH_ANGLE_OFFSET = 2;

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
    public static boolean[] getNeighbours(boolean[][] image, int row, int col) {
        // special case that is not expected (the image is supposed to have been checked earlier)
        assert (image != null);

        // check if pixel is in the image bounds
        if (row < 0 || row >= image.length || col < 0 || col >= image[0].length) return null;

        // check which of the sides of the 3x3 around the pixel are inbounds
        boolean topRowInImage = (row > 0);
        boolean rightColumnInImage = (col < (image[0].length - 1)); // get length of an inner list
        boolean bottomRowInImage = (row < (image.length - 1));
        boolean leftColumnInImage = (col > 0);

        // remember that positive y is down
        // for each pixel: if it's inbounds and true, set the p-value to true
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
     * Computes the number of black (<code>true</code>) pixels among the neighbours
     * of a pixel.
     *
     * @param neighbours array containing each pixel value. The array must respect
     *                   the convention described in
     *                   {@link #getNeighbours(boolean[][], int, int)}.
     * @return the number of black/<code>true</code> neighbours.
     */
    public static int blackNeighbours(boolean[] neighbours) {
        assert neighbours != null; // assert there is something to get neighbours for
        // accumulate how many neighbours are true
        return IntStream.range(0, neighbours.length) // list of numbers from 0 to neighbours.length
                .map(i -> neighbours[i] ? 1 : 0) // turn every i into a 1 if neighbours[i] is true and 0 if false
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
    public static int transitions(boolean[] neighbours) {
        assert neighbours != null; // make sure there are neighbours to search
        return IntStream.range(0, neighbours.length) // list of numbers from 0 to neighbours.length
                // turn every i into a 1 if it is false and the next one in the list is true
                .map(i -> !neighbours[i] && neighbours[(i + 1) % neighbours.length] ? 1 : 0)
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
                    && IntStream.range(0, image1.length).parallel() // list of numbers from 0 to the length of image1
                        .noneMatch( // make sure that all rows have no differing pixels
                                i -> IntStream.range(0, image1[0].length)//.parallel() // from 0 to the length of image1[0]
                                .anyMatch(j -> image1[i][j] != image2[i][j]))); // if any pixels are different -> true
        // @formatter:on
    }

    /**
     * Compute the skeleton of a boolean image.
     *
     * @param image array containing each pixel's boolean value.
     * @return array containing the boolean value of each pixel of the image after
     * applying the thinning algorithm.
     */
    public static boolean[][] thin(boolean[][] image) {
        boolean[][] previous = new boolean[image.length][image[0].length]; // define the dimensions of the image
        do {
            for (int i = 0; i < image.length; i++) // copy every row of image into previous
                System.arraycopy(image[i], 0, previous[i], 0, image[i].length);
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
    public static boolean[][] thinningStep(boolean[][] image, int step) {
        boolean[][] newImage = new boolean[image.length][image[0].length];
        for (int i = 0; i < image.length; i++) // for every pixel
            for (int j = 0; j < image[i].length; j++) {
                boolean[] neighbours = getNeighbours(image, i, j); // simplifies the logic below
                // @formatter:off
                newImage[i][j] = image[i][j] // if the pixel is black it stays black (image[i][j] && ...) == false
                        && !(neighbours != null // if all the below is true the pixel should be false -> !(...)
                            && blackNeighbours(neighbours) > 1
                            && blackNeighbours(neighbours) < 7
                            && transitions(neighbours) == 1
                            && (step == 0 // either the step0 conditions is true
                                    && (!neighbours[0] || !neighbours[2] || !neighbours[4])
                                    && (!neighbours[2] || !neighbours[4] || !neighbours[6])
                                || step == 1 // or the step1 conditions are true
                                    && (!neighbours[0] || !neighbours[2] || !neighbours[6])
                                    && (!neighbours[0] || !neighbours[4] || !neighbours[6])));
                // @formatter:on
            }
        return newImage;
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
    public static boolean[][] connectedPixels(boolean[][] image, int row, int col, int distance) {
        int squareSideLength = 2 * distance + 1;
        int topLeftCornerXCoordinate = col - distance;
        int topLeftCornerYCoordinate = row - distance;
        // create a square clone of a subset of the image centered on the pixel and squareSideLength wide
        boolean[][] clone = subClone(image, topLeftCornerYCoordinate, topLeftCornerXCoordinate, squareSideLength);

        boolean[][] relevant = new boolean[squareSideLength][squareSideLength];
        // set an empty array with the same center pixel as in clone
        relevant[distance][distance] = image[row][col];
        boolean[][] previousArray; // variable to remember what relevant looked like
        do {
            previousArray = ArrayCloneSquare(relevant, squareSideLength); // clone relevant to remember it
            for (int i = 0; i < squareSideLength; i++) // for each pixel
                for (int j = 0; j < squareSideLength; j++)
                    spreadPixel(clone, relevant, i, j); // make every pixel "infect" its true neighbours if it is true
        } while (!identical(previousArray, relevant)); // repeat if there was a change
        return relevant;
    }

    static boolean[][] ArrayCloneSquare(boolean[][] image, int width) {
        return subClone(image, 0, 0, width); // preset to clone a square array
    }

    static boolean[][] subClone(boolean[][] image, int topLeftRow, int topLeftCol, int width) {
        boolean[][] clone = new boolean[width][width];
        for (int i = 0; i < width; i++) // for every pixel
            for (int j = 0; j < width; j++) {
                int row = topLeftRow + i; // shift over by the top left coordinate of the sub square
                int col = topLeftCol + j;
                if (row >= 0 && row < image.length && col >= 0 && col < image[0].length)
                    clone[i][j] = image[row][col]; // copy it
            }
        return clone;
    }

    static void spreadPixel(boolean[][] imageSubset, boolean[][] subsetClone, int row, int col) {
        // check if pixel is in the image bounds
        if (row < 0 || row >= imageSubset.length || col < 0 || col >= imageSubset[0].length) return;
        if (!subsetClone[row][col]) return; // if the pixel is false it doesn't spread

        // check which of the sides of the 3x3 around the pixel are inbounds
        boolean topRowInImage = (row > 0);
        boolean rightColumnInImage = (col < (imageSubset[0].length - 1));
        boolean bottomRowInImage = (row < (imageSubset.length - 1));
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
    public static double computeSlope(boolean[][] connectedPixels, int row, int col) {
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

        double xySum = IntStream
                .range(0, xValues.size())//.parallel() // for each index of xValues
                .mapToDouble(i -> xValues.get(i) * yValues.get(i)) // multiply it with its equivalent in yValues
                .sum(); // sum them up
        double xSquared = xValues.stream()//.parallel() // for each value of xValues
                .mapToDouble(i -> i * i) // square it
                .sum(); // sum the squares
        double ySquared = yValues.stream()//.parallel() // for each value of yValues
                .mapToDouble(i -> i * i) // square it
                .sum(); // sum the squares

        if (xSquared == 0) return Double.POSITIVE_INFINITY; // if vertical return infinity
        if (xSquared >= ySquared) return xySum / xSquared;
        else return ySquared / xySum;
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
    public static double computeAngle(boolean[][] connectedPixels, int row, int col, double slope) {
        int pixelsAbove = 0;
        int pixelsUnder = 0;

        for (int i = 0; i < connectedPixels.length; i++) // for each pixel
            for (int j = 0; j < connectedPixels[i].length; j++)
                if (connectedPixels[i][j] && !(i == row && j == col)) { // if it's not the origin and it's true
                    int x = j - col; // make its coordinates relative to the new origin
                    int y = row - i;
                    if (y >= -1 / slope * x) pixelsAbove++; // if it's above the normal increment pixelsAbove
                    else pixelsUnder++; // if not increment pixels below
                }

        if (slope == Double.POSITIVE_INFINITY) // if the line is vertical
            return (pixelsAbove > pixelsUnder ? Math.PI : -Math.PI) / 2; // return either up or down
        double angle = Math.atan(slope);
        if ((angle > 0 && pixelsUnder > pixelsAbove) // if it's going up and there are more under the line than not
                || (angle < 0 && pixelsUnder < pixelsAbove)) // or going down and more over than under
            angle += Math.PI; // flip the angle
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
    public static List<int[]> extract(boolean[][] image) {
        var minutiaes = new ArrayList<int[]>();
        var minutia = new int[3];
        var thinImage = thin(image); // thin the image
        for (int i = 1; i < thinImage.length - 1; i++) // for each pixel excluding the outer edge
            for (int j = 1; j < thinImage[i].length - 1; j++)
                if (thinImage[i][j]) { // if it's part of the fingerprint
                    var neighbors = getNeighbours(thinImage, i, j);
                    assert neighbors != null;
                    var transitions = transitions(neighbors);
                    if (transitions == 3 || transitions == 1) { // if it's a minutia
                        minutia = new int[]{i, j, computeOrientation(thinImage, i, j, ORIENTATION_DISTANCE)};
                        minutiaes.add(minutia); // add it to the list
                    }
                }
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
    public static int[] applyRotation(int[] minutia, int centerRow, int centerCol, int rotation) {
        // center on new origin
        int x = minutia[1] - centerCol;
        int y = centerRow - minutia[0];
        // convert to radians, then compute sin/cos
        double rotationRad = Math.toRadians(rotation);
        double sinRot = Math.sin(rotationRad);
        double cosRot = Math.cos(rotationRad);
        // formula
        int newRow = (int) Math.round(centerRow - (x * sinRot + y * cosRot));
        int newCol = (int) Math.round(centerCol + x * cosRot - y * sinRot);
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
    public static int[] applyTranslation(int[] minutia, int rowTranslation, int colTranslation) {
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
    public static int[] applyTransformation(int[] minutia,
                                            int centerRow,
                                            int centerCol,
                                            int rowTranslation,
                                            int colTranslation,
                                            int rotation) {
        var translatedMinutiae = applyRotation(minutia, centerRow, centerCol, rotation);
        return applyTranslation(translatedMinutiae, rowTranslation, colTranslation);
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
    public static List<int[]> applyTransformation(List<int[]> minutiae,
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
    public static int matchingMinutiaeCount(List<int[]> minutiae1,
                                            List<int[]> minutiae2,
                                            int maxDistance,
                                            int maxOrientation) {
/*        return (int) minutiae1.stream().parallel() // for each minutia in minutiae1
                .filter(i -> minutiae2.stream().parallel()
                        .anyMatch(j -> // keep it if there is any in minutiae2 that is true below
                                Math.sqrt((i[0] - j[0]) * (i[0] - j[0]) + (i[1] - j[1]) * (i[1] - j[1])) <= maxDistance
                                        && Math.abs(i[2] - j[2]) <= maxOrientation))
                .count(); // count how many were kept*/
        int count=0;
        for(int i =0; i<minutiae1.size(); ++i){
            for(int j=0; j<minutiae2.size(); ++j){
                if(Math.sqrt((minutiae1.get(i)[0] - minutiae2.get(j)[0]) * (minutiae1.get(i)[0] - minutiae2.get(j)[0]) + (minutiae1.get(i)[1] - minutiae2.get(j)[1]) * (minutiae1.get(i)[1] - minutiae2.get(j)[1])) <= maxDistance
                        && Math.abs(minutiae1.get(i)[2] - minutiae2.get(j)[2]) <= maxOrientation){
                    ++count;
                }
            }
        }
        if(count>=19){
            System.out.println(count);
        }
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
}
