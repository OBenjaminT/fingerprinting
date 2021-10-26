# Fingerprinting

EPFL Java mini-project 1

[TOC]

## Structure

Receive 2 black and white pictures of fingerprints, and return if they represent
the same fingerprint.

Each image is a nested array of booleans, with `true = black` and `false =
white`.

### Task 1: Skeletonization

#### Preliminary methods

##### Method `getNeighbors`

Function signature: `boolean[] getNeighbors(boolean[][] image, int row, int col)`

Given a pixel `P` in a 3x3 grid of pixels, return an array of its neighbors
`[P0, P1, P2, ..., P7]` as defined below.

```
+----+----+----+
| P7 | P0 | P1 |
+----+----+----+
| P6 | P  | P2 |
+----+----+----+
| P5 | P4 | P3 |
+----+----+----+
```

Checks:
- Is the pixel being checked in the image?
    - Assume it is. i.e. don't check
- Are any of its neighbors outside of the image?
    - If they are, return `false`.

Initial idea (brute force):
```java
public class fingerprinting {
  // Assumes that the pixel given is in the image
  boolean[] getNeighbors(boolean[][] image, int row, int col) {
    boolean topRowInImage = (row > 0);
    boolean rightColumnInImage = (col < image[0].len); // get length of an inner list
    boolean bottomRowInImage = (row < image.len);
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
    
    return {p0, p1, p2, p3, p4, p5, p6, p7};
  }
}
```


##### Method `blackNeighbors`

Function signature: `int blackNeighbors (boolean [] neighbors)`

Given the output of `getNeighbors`, count how many are black/`true`.

```java
public class fingerprinting {
  int blackNeighbors(boolean[] neighbors) {
    int ans = 0;
    for (boolean i : boolList) {
      int isTrue = i ? 1 : 0;
      ans += isTrue;
    }
  }
}
```
        
##### Method `transitions`

Function signature: `int transitions(boolean[] neighbours)`

Given the output of `getNeighbors` count how many `false -> true` transitions there are.

```java
public class fingerprinting {
  int transitions(boolean[] neighbours) {
      
  }
}
```

##### Method `identical`

#### Main methods

#### Tests

### Task 2: Locate and calculate the orientation

#### Function `connectedPixels`

#### Function `computeSlope`

#### Function `computeAngle`

#### Function `computeOrientation`

#### Function `extract`

#### Tests

### Task 3: Comparison

#### Presentation of the algorithm

#### Method `applyTransformation`

#### Method `matchingMinutiaeCount`

#### Method `match`

#### Tests

