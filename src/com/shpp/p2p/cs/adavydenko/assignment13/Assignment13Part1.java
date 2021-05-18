package com.shpp.p2p.cs.adavydenko.assignment13;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class takes user image, reads it and defines the number of silhouettes
 * on the image. The background of the image and the color of the silhouettes
 * shall be in contrast. The program uses breadth-first-search algorithm to
 * detect silhouettes.
 * <p>
 * One can control program outputs by changing three parameters:
 * --- COLORS_SIMILARITY_THRESHOLD
 * The larger this value, the fewer pixels with a similar color to the background color
 * will be considered as background color.
 * --- MINIMUM_SILHOUETTE_SIZE
 * If this parameter is set to zero, the program will define all kind of small objects
 * as silhouettes (e.g. posterized pixels, artifacts and small insignificant fuzzy
 * objects etc). It is advised to keep this parameter at 0.05 % (== 0.0005) level to get
 * correct results.
 * <p>
 * I took part of ideas from external resources:
 * - The ArrayDeque concept
 * https://metanit.com/java/tutorial/5.7.php
 */
public class Assignment13Part1 {

    /**
     * The value indicating that two colors are totally equal.
     */
    static final double COLORS_ARE_TOTALLY_EQUIVALENT = 510.0;

    /**
     * The proportion of similarity between the two colors.
     * If a certain value is less than this value, the colors
     * are considered different. If larger, the colors
     * are considered equivalent.
     */
    static final double COLORS_SIMILARITY_THRESHOLD = 0.95;

    /**
     * The share of pixels from the total number of pixels in the
     * user image that an object must have to be considered a silhouette.
     * Is set by default to 0.05 % (== 0.0005).
     */
    static final double MINIMUM_SILHOUETTE_SIZE = 0.0005;

    /**
     * Indicates the height of the top / bottom edge or the width of the
     * right / left edge to be inspected to define the background color.
     * <p>
     * One can set this value to 1, but the value three provides more
     * accurate results.
     */
    static final int NUMBER_OF_LINES = 3;

    /**
     * The image the user provided to the program.
     */
    static BufferedImage image = null;

    /**
     * The background color that differs from silhouettes colors
     * and its four channels values.
     */
    static Color bgColor;
    static int bgColorRed;
    static int bgColorGreen;
    static int bgColorBlue;
    static int bgColorAlpha;

    /**
     * A two-dimensional array consisting of the pixels` colors
     * of the user provided image. Each array cell represents a
     * corresponding pixel in the user image.
     */
    static Color[][] imgArray;

    /**
     * An array indicating whether the program already inspected a particular
     * pixel of the image. The array has the same dimensions as the imgArray.
     */
    static boolean[][] visited;

    /**
     * An array indicating whether a particular pixel has already been
     * in the queue (even if the pixel has been already removed from it).
     * The array has the same dimensions as the imgArray.
     */
    static boolean[][] pixelsInQueue;

    /**
     * An array containing the pixels that shall be inspected
     * by the program. The program always takes the first element
     * from the left and adds new elements only to the array`s end.
     */
    static ArrayDeque<int[]> queue = new ArrayDeque<>();

    /**
     * An array with all silhouettes the program found on the image.
     */
    static ArrayList<Silhouette> silhouettes = new ArrayList<>();

    /**
     * Main method launches the algorithm for finding silhouettes
     * on the user image.
     *
     * @param args are command line arguments provided by user.
     */
    public static void main(String[] args) {
        findSilhouettes(args);
    }

    /**
     * Uploads user image, converts it to the colors array (imgArray),
     * defines background color and its components, creates an array
     * consisting of each pixels` color, iterates through all pixels
     * and looks for silhouettes using breadth-first-algorithm (bfs).
     *
     * @param args are command line arguments provided by user.
     */
    private static void findSilhouettes(String[] args) {
        try {
            File file = new File(getFilePath(args)); // Gets image location
            image = ImageIO.read(file);              // Reads user image from the provided location
            bgColor = getBackgroundColor();          // Defines background color
            setBgColorComponents();                  // Saves background color`s RGBA-components
            writeImageToArray();                     // Creates an array consisting of image pixel colors
            visited = fillBooleanArray();            // Fills visited-array to indicate visited pixels
            pixelsInQueue = fillBooleanArray();      // Fills pixelsInQueue-array to indicate pixels in queue
            inspectImagePixels();                    // Inspects all image pixels to find silhouettes
            countAndDisplayNumOfSilhouettes();       // Counts and displays number of silhouettes
        } catch (Exception evt) {
            System.out.println(evt.getMessage());    // Displays the error occurred if any
        }
    }

    /**
     * Defines the user image location.
     *
     * @param args are the command line arguments provided by user.
     * @return user image location as a string.
     */
    private static String getFilePath(String[] args) {
        String filePath;

        if (args.length == 0) {
            filePath = "test.jpg";
        } else {
            filePath = args[0];
        }
        return filePath;
    }

    /**
     * Creates a two-dimensional array consisting of color objects.
     * Each of them represents the color of a corresponding pixel of
     * the image provided by the user.
     * E.g. the imgArray[1][1] color represents the color of the
     * pixel in the second column and the second row in the user image.
     */
    private static void writeImageToArray() {
        /* Creates additional one-pixel-thick rows / columns on the very top, bottom,
         left and right edges of the array to fills them later with background color */
        imgArray = new Color[image.getHeight() + 2][image.getWidth() + 2];
        fillPixelsWithBGColor(); // Fills array`s edges with background color

        // Copies the color values of the image pixels to the corresponding cells of the imgArray
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                Color currentColor = new Color(image.getRGB(j, i), true);
                imgArray[i + 1][j + 1] = currentColor;
            }
        }
    }

    /**
     * Fills the very top, bottom, left and right edges of the array
     * with background color.
     * This is the way to prevent the program from throwing an error
     * when a silhouette touches image edge.
     */
    private static void fillPixelsWithBGColor() {
        // Fills the left and the right edges
        for (int y = 0; y < imgArray.length; y++) {
            imgArray[y][0] = bgColor;
            imgArray[y][imgArray[0].length - 1] = bgColor;
        }
        // Fills the top and the bottom edges
        for (int x = 0; x < imgArray[0].length; x++) {
            imgArray[0][x] = bgColor;
            imgArray[imgArray.length - 1][x] = bgColor;
        }
    }

    /**
     * Iterates through each image pixel and looks for pixels of non-background color.
     * If found any, the program uses breadth-first search algorithm to detect all such
     * neighbour pixels and deems them as silhouette pixels. If the program finds pixels
     * of the background color, it marks them visited and goes on looking for silhouettes.
     */
    private static void inspectImagePixels() {
        for (int x = 0; x < imgArray.length; x++) {
            for (int y = 0; y < imgArray[0].length; y++) { // Iterates through each pixel of the image
                if (!visited[x][y]) { // Inspects a particular pixel only if it has not been inspected yet
                    Color currentColor = imgArray[x][y];
                    // If it is not a background pixel, deems it as a silhouette pixel and finds other silhouette pixels
                    if (isNotSimilarToBackground(currentColor)) {
                        // Creates new silhouette object if found first non-background pixel
                        Silhouette silhouette = new Silhouette();
                        silhouettes.add(silhouette); // Adds this silhouette to the array with all silhouettes
                        int[] firstPixelCoordinates = {x, y}; // Coordinates of the first silhouette`s pixel
                        queue.addLast(firstPixelCoordinates); // Adds first pixel coordinates to the queue
                        bfs(silhouette); // Runs breadth-first search (bfs) algorithm
                    } else {
                        // Marks the pixel visited if its is a background pixel and it has not been visited yet
                        visited[x][y] = true;
                    }
                }
            }
        }
    }

    /**
     * Non-recursive breadth-first search (BFS) algorithm that detects
     * all pixels belonging to a silhouette.
     *
     * @param silhouette is the object containing the number of pixels
     *                   this particular silhouette consists of and the
     *                   silhouette`s pixels coordinates.
     */
    private static void bfs(Silhouette silhouette) {

        while (queue.size() > 0) { // Keeps going while there is at least one pixel in the queue array
            int currentPixelX = queue.getFirst()[0]; // X-coordinate of the first pixel in the queue
            int currentPixelY = queue.getFirst()[1]; // Y-coordinate of the first pixel in the queue
            if (!noNeighboursLeft(currentPixelX, currentPixelY)) { // Adds valid neighbour pixels to the queue if any
                addNeighboursToQueue(currentPixelX, currentPixelY);
            }
            visited[currentPixelX][currentPixelY] = true; // Marks current pixel as visited
            silhouette.numOfPixels += 1; // Increases the number of silhouette`s pixels by one
            silhouette.silhouettePixels[currentPixelX][currentPixelY] = true; // Links this pixel to the silhouette object
            queue.removeFirst(); // Deletes current pixel`s coordinates from the queue
        }
    }

    /**
     * Chooses neighbours pixels that shall be added to the queue and inspected later.
     * These pixels:
     * --- shall be not visited yet;
     * --- shall exist;
     * --- shall have non-background color;
     * --- shall be "peers" and not "children" to the currently inspected pixel.
     *
     * @param x is the x-coordinate of the current pixel.
     * @param y is the y-coordinate of the current pixel.
     */
    private static void addNeighboursToQueue(int x, int y) {
        addBottomNeighbourToQueue(x, 1, y, 0);
        addTopNeighbourToQueue(x, -1, y, 0);
        addRightNeighbourToQueue(x, 0, y, 1);
        addLeftNeighbourToQueue(x, 0, y, -1);
    }

    /**
     * Adds bottom neighbour pixel of the current pixel to the queue if it
     * exists, is not visited yet, has not got background color and was
     * not in the queue before.
     *
     * @param x  is the x-coordinate of the current pixel.
     * @param dx is the value by which the x-coordinate of the current
     *           pixel should change to find its bottom neighbor.
     * @param y  is the y-coordinate of the current pixel.
     * @param dy is the value by which the y-coordinate of the current
     *           pixel should change to find its bottom neighbor.
     */
    private static void addBottomNeighbourToQueue(int x, int dx, int y, int dy) {
        if ((x + dx < imgArray.length) && !visited[x + dx][y + dy]
                && isNotSimilarToBackground(x + dx, y + dy) && !pixelsInQueue[x + dx][y + dy]) {
            addNeighbourPixelCoordinates(x, dx, y, dy);
            pixelsInQueue[x + dx][y + dy] = true; // Mark that the pixel already has been in the queue
        }
    }

    /**
     * Adds top neighbour pixel of the current pixel to the queue if it
     * exists, is not visited yet, has not got background color and was
     * not in the queue before.
     *
     * @param x  is the x-coordinate of the current pixel.
     * @param dx is the value by which the x-coordinate of the current
     *           pixel should change to find its top neighbor.
     * @param y  is the y-coordinate of the current pixel.
     * @param dy is the value by which the y-coordinate of the current
     *           pixel should change to find its top neighbor.
     */
    private static void addTopNeighbourToQueue(int x, int dx, int y, int dy) {
        if ((x + dx >= 0) && !visited[x + dx][y + dy]
                && isNotSimilarToBackground(x + dx, y + dy) && !pixelsInQueue[x + dx][y + dy]) {
            addNeighbourPixelCoordinates(x, dx, y, dy);
            pixelsInQueue[x + dx][y + dy] = true; // Mark that the pixel already has been in the queue
        }
    }

    /**
     * Adds right neighbour pixel of the current pixel to the queue if it
     * exists, is not visited yet, has not got background color and was
     * not in the queue before.
     *
     * @param x  is the x-coordinate of the current pixel.
     * @param dx is the value by which the x-coordinate of the current
     *           pixel should change to find its right neighbor.
     * @param y  is the y-coordinate of the current pixel.
     * @param dy is the value by which the y-coordinate of the current
     *           pixel should change to find its right neighbor.
     */
    private static void addRightNeighbourToQueue(int x, int dx, int y, int dy) {
        if ((y + dy < imgArray[0].length) && !visited[x + dx][y + dy]
                && isNotSimilarToBackground(x + dx, y + dy) && !pixelsInQueue[x + dx][y + dy]) {
            addNeighbourPixelCoordinates(x, dx, y, dy);
            pixelsInQueue[x + dx][y + dy] = true; // Mark that the pixel already has been in the queue
        }
    }

    /**
     * Adds top neighbour pixel of the current pixel to the queue if it
     * exists, is not visited yet, has not got background color and was
     * not in the queue before.
     *
     * @param x  is the x-coordinate of the current pixel.
     * @param dx is the value by which the x-coordinate of the current
     *           pixel should change to find its left neighbor.
     * @param y  is the y-coordinate of the current pixel.
     * @param dy is the value by which the y-coordinate of the current
     *           pixel should change to find its left neighbor.
     */
    private static void addLeftNeighbourToQueue(int x, int dx, int y, int dy) {
        if ((y + dy >= 0) && !visited[x + dx][y + dy]
                && isNotSimilarToBackground(x + dx, y + dy) && !pixelsInQueue[x + dx][y + dy]) {
            addNeighbourPixelCoordinates(x, dx, y, dy);
            pixelsInQueue[x + dx][y + dy] = true; // Mark that the pixel already has been in the queue
        }
    }

    /**
     * Creates an array with neighbour pixel x- and y-coordinates
     * and adds them to the queue.
     *
     * @param x  is the x-coordinate of the current pixel.
     * @param dx is the value by which the x-coordinate of the current
     *           pixel should change to find its neighbor
     * @param y  is the y-coordinate of the current pixel.
     * @param dy is the value by which the y-coordinate of the current
     *           pixel should change to find its neighbor
     */
    private static void addNeighbourPixelCoordinates(int x, int dx, int y, int dy) {
        int[] coordinatesArray = new int[2]; // Saves coordinates of the next pixel to be inspected
        coordinatesArray[0] = x + dx; // X-coordinate of the neighbour pixel
        coordinatesArray[1] = y + dy; // Y-coordinate of the neighbour pixel
        queue.addLast(coordinatesArray); // Adds neighbour pixel to the queue
    }

    /**
     * Says whether there are any pixels the program can visit starting from the current pixel.
     * The program can not visit a pixel if it does not exist, if it is already visited or
     * if it is a background pixel.
     *
     * @param x is the x-coordinate of the current pixel.
     * @param y is the y-coordinate of the current pixel.
     * @return true if the neighbor pixels do not exist
     * or are already visited or have background color.
     */
    private static boolean noNeighboursLeft(int x, int y) {

        return ((x + 1 >= imgArray.length) || visited[x + 1][y] || !isNotSimilarToBackground(x + 1, y))
                && ((x - 1 < 0) || visited[x - 1][y] || !isNotSimilarToBackground(x - 1, y))
                && ((y + 1 >= imgArray[0].length) || visited[x][y + 1] || !isNotSimilarToBackground(x, y + 1))
                && ((y - 1 < 0) || visited[x][y - 1]) || !isNotSimilarToBackground(x, y - 1);
    }

    /**
     * Defines which color shall be deemed as background color.
     *
     * @return background color as Color object.
     */
    private static Color getBackgroundColor() {
        /* HashMap with all colors which can be fined on the picture
        edges and the number of pixels with these colors */
        HashMap<Color, Integer> numOfColors = new HashMap<>();

        inspectTopImageEdge(numOfColors);
        inspectBottomImageEdge(numOfColors);
        inspectLeftImageEdge(numOfColors);
        inspectRightImageEdge(numOfColors);
        return findMaxPixelNumber(numOfColors);
    }

    /**
     * Defines background color`s all four channels values
     * (red, green, blue and alpha).
     */
    private static void setBgColorComponents() {
        bgColorRed = bgColor.getRed();
        bgColorGreen = bgColor.getGreen();
        bgColorBlue = bgColor.getBlue();
        bgColorAlpha = bgColor.getAlpha();
    }

    /**
     * Inspects {NUMBER_OF_LINES} (3 by default) top pixel rows
     * in the picture provided by user, saves all detected colors
     * to a hashmap and counts the number of pixels with these
     * colors in these three top rows.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     */
    private static void inspectTopImageEdge(HashMap<Color, Integer> numOfColors) {
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < NUMBER_OF_LINES; j++) {
                Color color = new Color(image.getRGB(i, j), true);
                saveColorData(numOfColors, color);
            }
        }
    }

    /**
     * Inspects three bottom pixel rows in the picture provided by user,
     * saves all detected colors to a hashmap and counts the number
     * of pixels with these colors in these three bottom rows.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     */
    private static void inspectBottomImageEdge(HashMap<Color, Integer> numOfColors) {
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = image.getHeight() - NUMBER_OF_LINES; j < image.getHeight(); j++) {
                Color color = new Color(image.getRGB(i, j), true);
                saveColorData(numOfColors, color);
            }
        }
    }

    /**
     * Inspects three left pixel rows in the picture provided by user,
     * saves all detected colors to a hashmap and counts the number
     * of pixels with these colors in these three left rows.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     */
    private static void inspectLeftImageEdge(HashMap<Color, Integer> numOfColors) {
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < NUMBER_OF_LINES; j++) {
                Color color = new Color(image.getRGB(j, i), true);
                saveColorData(numOfColors, color);
            }
        }
    }

    /**
     * Inspects three right pixel rows in the picture provided by user,
     * saves all detected colors to a hashmap and counts the number
     * of pixels with these colors in these three right rows.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    with these colors.
     */
    private static void inspectRightImageEdge(HashMap<Color, Integer> numOfColors) {
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = image.getWidth() - NUMBER_OF_LINES; j < image.getWidth(); j++) {
                Color color = new Color(image.getRGB(j, i), true);
                saveColorData(numOfColors, color);
            }
        }
    }

    /**
     * Calculates the color equivalence coefficient between the background color
     * and the color that is compared with the background color.
     * If the compared color is more than {COLORS_SIMILARITY_THRESHOLD} % similar to the background color,
     * it is deemed to be a background color and not a silhouettes color. If less - it is deemed to be
     * a silhouettes color.
     *
     * @param color is the color that is compared with the background color.
     * @return true if both colors are not similar.
     */
    private static boolean isNotSimilarToBackground(Color color) {
        // Values of red, green, blue and alpha channels of the pixel compared to the background color
        int pixelColorRed = color.getRed();
        int pixelColorGreen = color.getGreen();
        int pixelColorBlue = color.getBlue();
        int pixelColorAlpha = color.getAlpha();

        // Calculates the difference between two colors by calculating the difference between all four channels
        double colorDifference = Math.pow(Math.pow(bgColorRed - pixelColorRed, 2)
                + Math.pow(bgColorGreen - pixelColorGreen, 2)
                + Math.pow(bgColorBlue - pixelColorBlue, 2)
                + Math.pow(bgColorAlpha - pixelColorAlpha, 2), (0.5));

        // Calculates color equivalence coefficient between background and the current colors
        double colorEquivalenceCoefficient = (COLORS_ARE_TOTALLY_EQUIVALENT - colorDifference)
                / COLORS_ARE_TOTALLY_EQUIVALENT;

        // Returns true if color equivalence coefficient is more than certain threshold
        return !(colorEquivalenceCoefficient > COLORS_SIMILARITY_THRESHOLD);
    }

    /**
     * Calculates the color equivalence coefficient between the background
     * color and the color that is compared with the background color.
     * If the compared color is more than {COLORS_SIMILARITY_THRESHOLD} %
     * similar to the background color, it is deemed to be a background
     * color and not a silhouettes color. If less - it is deemed to be
     * a silhouettes color.
     *
     * @param x is the x-coordinate of the color to be compared with the background color.
     * @param y is the y-coordinate of the color to be compared with the background color.
     * @return true if both colors are not similar.
     */
    private static boolean isNotSimilarToBackground(int x, int y) {
        Color color = imgArray[x][y];
        return isNotSimilarToBackground(color);
    }

    /**
     * Saves the color of a particular pixel to the hashmap as a key and the number of
     * pixels of this color as a value.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    of these colors.
     * @param color       is a color of a particular pixel.
     */
    private static void saveColorData(HashMap<Color, Integer> numOfColors, Color color) {
        if (!numOfColors.containsKey(color)) {
            numOfColors.put(color, 1);
        } else {
            int pixelNum = numOfColors.get(color);
            numOfColors.put(color, pixelNum + 1);
        }
    }

    /**
     * Iterates through the numOfColors hashmap and finds the color with the biggest
     * number of pixels.
     *
     * @param numOfColors HashMap with all colors which can be fined
     *                    on the picture edges and the number of pixels
     *                    of these colors.
     * @return the color that is deemed to be a background color.
     */
    private static Color findMaxPixelNumber(HashMap<Color, Integer> numOfColors) {
        // Saves the biggest number of pixels of a particular color in the numOfColors hashmap
        int maxValue = 0;
        // Saves the color from the numOfColors hashmap with the biggest number of pixels
        Color bgColor = null;

        for (Color color : numOfColors.keySet()) {
            if (numOfColors.get(color) > maxValue) {
                maxValue = numOfColors.get(color);
                bgColor = color;
            }
        }
        return bgColor;
    }

    /**
     * Fills boolean array of the same size as the image array
     * with "false" values.
     *
     * @return an array of the same size that the image array
     * filled with "false" values.
     */
    protected static boolean[][] fillBooleanArray() {
        return new boolean[imgArray.length][imgArray[0].length];
    }

    /**
     * Prints to console the red, green, blue and alpha channels
     * values of the color the program defined as the background color.
     * One uses this method for debugging purposes, namely to compare
     * the actual background color with the color the program identified
     * as the particular image background color.
     */
    private static void displayBackgroundColor() {
        System.out.println("The background color is: "
                + "\n" + "-- red: " + bgColor.getRed()
                + "\n" + "-- green: " + bgColor.getGreen()
                + "\n" + "-- blue: " + bgColor.getBlue()
                + "\n" + "-- alpha: " + bgColor.getAlpha());
    }

    /**
     * Takes an array with all detected silhouettes and counts only those
     * which have more than {imgPixelTotalAmount * minimumSilhouetteSize} %
     * pixels from the overall number of pixels in the image.
     * This value is set to 0.05 % by default and can be changed.
     */
    private static void countAndDisplayNumOfSilhouettes() {
        // Number of silhouettes detected on the image
        int numOfSilhouettes = 0;
        // Total number of all pixels in the image
        int imgPixelTotalNum = imgArray.length * imgArray[0].length;

        /* Increases numOfSilhouettes by one if the silhouette has more
         than {imgPixelTotalAmount * minimumSilhouetteSize} pixels (0.05 %) */
        for (Silhouette silhouette : silhouettes) {
            if (silhouette.numOfPixels > imgPixelTotalNum * MINIMUM_SILHOUETTE_SIZE) {
                numOfSilhouettes++;
                System.out.println("Silhouette №" + numOfSilhouettes + " - " + silhouette.numOfPixels + " pixels");
            }
        }
        System.out.println("Total number of silhouettes: " + numOfSilhouettes);
    }
}
