package tsp.app;

import tsp.graph.Edge;
import tsp.graph.Graph;
import tsp.graph.Node;
import tsp.gui.TSPWindow;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.*;

public class TSPSolver {

    //
    // Define constants used for pheromone modifications and randomness factor in the tour
    //
    public static int NUM_AGENTS = 10;
    public static double Q0_PARAM = 0.25; // chance to follow the highest weighted pheromone trail
    public static double DISTANCE_WEIGHT = 2.00; // inverse distance exponent (lower values give higher weight to distance versus pheromone value)
    public static double DECAY_VALUE = 0.10; // pheromone decay paramater
    public static double INIT_WEIGHT = 1.00; // initial edge pheromone value
    public static double INCREASE_WEIGHT = 1.00; // visited edge pheromone increase value

    /**
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {

        Graph graph = new Graph();
        TSPWindow window = new TSPWindow(1024, 768, "Traveling Salesman", graph);
        window.setLocation(75, 75);

        // MainEventLoop will handle updating the display and 
        // handling any agents that are touring the graph
        Timer timer = new Timer(0, new MainEventLoop(window, graph));
        timer.start();
    }
}

class MainEventLoop extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1470315229081539641L;

    private enum Algorithm {
        NONE,
        ANTS,
        HILL_CLIMBING,
        TWO_OPT,
        THREE_OPT;
    }

    private TSPWindow _window;
    private Graph _graph;
    private List<Agent> _agents = new ArrayList<Agent>();
    private Algorithm _runningAlgorithm = Algorithm.NONE;
    private long _calculatePathStartTime = 0;

    private List<Node> _currentWorkingTour = null;

    MainEventLoop(TSPWindow window, Graph graph) {
        _window = window;
        _graph = graph;
    }

    private boolean _displayed = false;

    @Override
    public void actionPerformed(ActionEvent e) {

        if (_calculatePathStartTime != 0) {
            long elapsed = ((System.currentTimeMillis() - _calculatePathStartTime) / 1000);
            if (elapsed != 0 && elapsed % 15 == 0) {
                if (!_displayed) {
                    System.out.println("TIME: " + elapsed + "s");
                    _displayed = true;
                }
            }
            else {
                _displayed = false;
            }
        }

        if (_window.generateButtonClicked()) {
            _runningAlgorithm = Algorithm.NONE;
            _calculatePathStartTime = 0;
            _agents.clear();
        }

        if (_window.releaseAntsButtonClicked()) {
            _runningAlgorithm = Algorithm.ANTS;
            initializeAgents(TSPSolver.NUM_AGENTS);
            _calculatePathStartTime = System.currentTimeMillis();
        }

        if (_window.hillClimbingButtonClicked()) {
            _runningAlgorithm = Algorithm.HILL_CLIMBING;
            _currentWorkingTour = null;
            _calculatePathStartTime = System.currentTimeMillis();
        }

        if (_window.twoOptButtonClicked()) {
            _runningAlgorithm = Algorithm.TWO_OPT;
            _currentWorkingTour = null;
            _calculatePathStartTime = System.currentTimeMillis();
        }

        if (_window.threeOptButtonClicked()) {
            _runningAlgorithm = Algorithm.THREE_OPT;
            _currentWorkingTour = null;
            _calculatePathStartTime = System.currentTimeMillis();
        }

        long bestPathLength = _graph.getBestPathLength();

        if (_runningAlgorithm.equals(Algorithm.ANTS)) {
            updateAgents();
        }
        else if (_runningAlgorithm.equals(Algorithm.HILL_CLIMBING)) {
            updateHillClimbing();
        }
        else if (_runningAlgorithm.equals(Algorithm.TWO_OPT)) {
            long startTime = System.nanoTime();
            updateTwoOpt();
        }
        else if (_runningAlgorithm.equals(Algorithm.THREE_OPT)) {
            long startTime = System.nanoTime();
            updateThreeOpt();
        }

        // Did we find a better path this update cycle? If so, update the elapsed time to solution found!
        if (_graph.getBestPathLength() < bestPathLength) {
            long elapsedTime = System.currentTimeMillis() - _calculatePathStartTime;
            _graph.setElapsedTime(elapsedTime);
        }

        redrawGraph();
        _window.repaint();
    }

    /**
     * Creates a new random tour for hill-climbing by using nearest-neighbor algorithm
     * but every so often picking an edge completely at random 
     */
    private List<Node> createRandomTour(double randomEdgeChance) {
        //
        List<Node> remainingNodes = new ArrayList<Node>(_graph.getNodes());
        List<Node> randomTour = new ArrayList<Node>();

        if (remainingNodes.isEmpty()) {
            return randomTour;
        }

        List<Node> bestPath = _graph.getBestPath();

        Random rand = new Random();
        while (!remainingNodes.isEmpty()) {
            Node nextNode = null;
            if (randomTour.isEmpty() || rand.nextDouble() < randomEdgeChance) {
                // Pick a remaining city entirely at random
                int randIndex = rand.nextInt(remainingNodes.size());
                nextNode = remainingNodes.get(randIndex);
            }
            else {
                // Using the best tour found so far, we will travel to the next city from the current city in that tour
                // If the next city IS NOT in the remaining nodes, then simply revert to nearest (remaining) neighbor
                Node currentNode = randomTour.get(randomTour.size() - 1);
                int indexOfCurrentNodeInBestPath = bestPath.indexOf(currentNode);
                if (indexOfCurrentNodeInBestPath < bestPath.size() - 2 &&
                        remainingNodes.contains(bestPath.get(indexOfCurrentNodeInBestPath + 1))) {
                    // Travel along best path already found rather than nearest neighbor
                    nextNode = bestPath.get(indexOfCurrentNodeInBestPath + 1);
                }
                else {
                    // Pick nearest node as the next node in the best path from current node has already been visited
                    long closestDistance = 0;
                    for (Node node : remainingNodes) {
                        long distance = currentNode.distance(node);
                        if (nextNode == null || distance < closestDistance) {
                            nextNode = node;
                            closestDistance = distance;
                        }
                    }
                }
            }

            randomTour.add(nextNode);
            remainingNodes.remove(nextNode);
        }

        // Go back to starting node then return tour
        randomTour.add(randomTour.get(0));
        return randomTour;
    }

    private void updateHillClimbing() {
        // Start from best tour found OR greedy tour (if no best tour exists) and perform hill climbing from there
        if (_currentWorkingTour == null) {
            _currentWorkingTour = (!_graph.getBestPath().isEmpty()) ? _graph.getBestPath() : _graph.getGreedyPath();
        }

        List<Node> bestTourLocated = new ArrayList<Node>(_currentWorkingTour);
        long bestTourLength = _graph.calculatePathLength(bestTourLocated);

        // Step function will loop through the best tour found so far and find
        // the two nodes that, when swapped, grant the best new tour
        // Take care not to mess with the first or last nodes in the list (to keep the tour intact)
        for (int i = 1; i < _currentWorkingTour.size() - 2; i++) {
            for (int k = i + 1; k < _currentWorkingTour.size() - 1; k++) {
                // Swap nodes at position i and k, then calculate length and compare
                List<Node> testPath = new ArrayList<Node>(_currentWorkingTour);
                Collections.swap(testPath, i, k);

                long testPathLength = _graph.calculatePathLength(testPath);
                if (testPathLength < bestTourLength) {
                    bestTourLocated = testPath;
                    bestTourLength = testPathLength;
                }
            }
        }

        if (!_currentWorkingTour.equals(bestTourLocated)) {
            // new best tour was located -- local maximum not yet reached
            _currentWorkingTour = bestTourLocated;
            _graph.setBestPath(bestTourLocated);
        }
        else {
            // Local maximum found, so restart from random tour
            //System.out.println("Local maximum reached... Resetting to random tour.");
            _currentWorkingTour = createRandomTour(0.05d);
        }
    }

    private void updateTwoOpt() {
        // Start from best tour found OR greedy tour (if no best tour exists) and perform two-opt from there
        if (_currentWorkingTour == null) {
            _currentWorkingTour = (!_graph.getBestPath().isEmpty()) ? _graph.getBestPath() : _graph.getGreedyPath();
        }

        // Step function will perform a 2-opt move
        // A 2-opt move means picking every pair of non-adjacent edges in the graph, 
        // eliminating these edges, and reconnecting the graph -- by eliminating two non adjacent edges,
        // only a SINGLE method (that does not recreate the original tour) exists to reconnect these nodes
        // - This is a very simple algorithm to translate into a single list of nodes
        // --- 1. Iterate through every pair of nodes EXCEPT the first and last node in the list (same node in cyclic graph)
        // --- 2. Reverse the sublist from the first node to last node selected
        // ** i.e. for a list with 10 nodes (0..9) we choose ALL PAIRS of indices from 1..8 and the first reverse
        // ** attempted should be indices [1, 2] and the last reverse attempted should be indices [7, 8]

        // Initialize best path to the current two opt tour generated 
        List<Node> bestTourLocated = new ArrayList<Node>(_currentWorkingTour);
        long bestTourLength = _graph.calculatePathLength(bestTourLocated);

        List<Node> currTwoOptMove = new ArrayList<>(_currentWorkingTour);

        // Set up a nested loop to attempt every possible (valid) pair of indices in the list
        for (int firstNode = 1; firstNode < _currentWorkingTour.size() - 2; firstNode++) {
            for (int secondNode = firstNode + 2; secondNode < _currentWorkingTour.size(); secondNode++) {

                // Reverse the nodes between first and second nodes (secondNode is exclusive)
                Collections.reverse(currTwoOptMove.subList(firstNode, secondNode));

                long currTourLength = _graph.calculatePathLength(currTwoOptMove);
                if (currTourLength < bestTourLength) {
                    bestTourLocated = new ArrayList<>(currTwoOptMove);
                    bestTourLength = currTourLength;
                }

                // Reset changes made above before next iteration
                Collections.reverse(currTwoOptMove.subList(firstNode, secondNode));
            }
        }

        if (!_currentWorkingTour.equals(bestTourLocated)) {
            // new best tour was located -- local maximum not yet reached
            _currentWorkingTour = bestTourLocated;
            _graph.setBestPath(bestTourLocated);
        }
        else {
            // Local maximum found, so restart from random tour
            //System.out.println("Local maximum reached... Resetting to random tour.");
            _currentWorkingTour = createRandomTour(0.20);
        }
    }

    private void updateThreeOpt() {
        // Start from best tour found OR greedy tour (if no best tour exists) and perform two-opt from there
        if (_currentWorkingTour == null) {
            _currentWorkingTour = (!_graph.getBestPath().isEmpty()) ? _graph.getBestPath() : _graph.getGreedyPath();
        }

        // Initialize best path to the current two opt tour generated
        List<Node> bestTourLocated = new ArrayList<Node>(_currentWorkingTour);
        long bestTourLength = _graph.calculatePathLength(bestTourLocated);

        // Set up a nested loop to attempt every possible (valid) pair of indices in the list
        for (int firstNode = 1; firstNode < _currentWorkingTour.size() - 3; firstNode++) {
            for (int secondNode = firstNode + 2; secondNode < _currentWorkingTour.size() - 2; secondNode++) {
                for (int thirdNode = secondNode + 2; thirdNode < _currentWorkingTour.size(); thirdNode++) {

                    List<Node>[] currThreeOptMoves = new List[7];
                    currThreeOptMoves[0] = new ArrayList<>(_currentWorkingTour);
                    currThreeOptMoves[1] = new ArrayList<>(_currentWorkingTour);
                    currThreeOptMoves[2] = new ArrayList<>(_currentWorkingTour);
                    currThreeOptMoves[3] = new ArrayList<>(_currentWorkingTour);
                    currThreeOptMoves[4] = new ArrayList<>(_currentWorkingTour);
                    currThreeOptMoves[5] = new ArrayList<>(_currentWorkingTour);
                    currThreeOptMoves[6] = new ArrayList<>(_currentWorkingTour);

                    // Case1
                    Collections.reverse(currThreeOptMoves[0].subList(firstNode, secondNode));

                    // Case2
                    Collections.reverse(currThreeOptMoves[1].subList(secondNode, thirdNode));

                    // Case3
                    Collections.reverse(currThreeOptMoves[2].subList(firstNode, secondNode));
                    Collections.reverse(currThreeOptMoves[2].subList(secondNode, thirdNode));

                    // Case4
                    Collections.reverse(currThreeOptMoves[3].subList(firstNode, thirdNode));

                    // Case5
                    Collections.reverse(currThreeOptMoves[4].subList(firstNode, thirdNode));
                    Collections.reverse(currThreeOptMoves[4].subList(firstNode, secondNode));

                    // Case6
                    Collections.reverse(currThreeOptMoves[5].subList(firstNode, thirdNode));
                    Collections.reverse(currThreeOptMoves[5].subList(secondNode, thirdNode));

                    // Case7
                    Collections.reverse(currThreeOptMoves[6].subList(firstNode, thirdNode));
                    Collections.reverse(currThreeOptMoves[6].subList(firstNode, secondNode));
                    Collections.reverse(currThreeOptMoves[6].subList(secondNode, thirdNode));

                    for (int i = 0; i < currThreeOptMoves.length; i++) {
                        List<Node> currThreeOptMove = currThreeOptMoves[i];
                        long currTourLength = _graph.calculatePathLength(currThreeOptMove);
                        if (currTourLength < bestTourLength) {
                            bestTourLocated = currThreeOptMove;
                            bestTourLength = currTourLength;
                        }
                    }

                }
            }
        }

        if (!_currentWorkingTour.equals(bestTourLocated)) {
            // new best tour was located -- local maximum not yet reached
            _currentWorkingTour = bestTourLocated;
            _graph.setBestPath(bestTourLocated);
        }
        else {
            // Local maximum found, so restart from random tour
            //System.out.println("Local maximum reached... Resetting to random tour.");
            _currentWorkingTour = createRandomTour(0.20);
        }
    }

    private void initializeAgents(int numAgents) {
        _agents.clear();
        for (int i = 0; i < numAgents; i++) {
            _agents.add(new Agent(_graph));
        }
    }

    private void updateAgents() {
        // only update each agent so many times to avoid starving event queue
        int maxUpdates = 5;
        boolean allComplete = false;
        while (maxUpdates > 0) {
            //
            allComplete = true;
            for (Agent agent : _agents) {
                if (!agent.complete()) {
                    agent.update();
                    allComplete = false;
                }
            }
            if (allComplete) break;
            maxUpdates--;
            if (maxUpdates <= 0) break;
        }

        if (allComplete) {
            for (Agent agent : _agents) {
                // Get each agent's tour and see if it's the best path (setting best path fails/returns false if not better than than best path so far)
                List<Node> path = agent.getTour();
                _graph.setBestPath(path);

                // Perform local pheromone updating
                agent.updateEdgeLocal();
                agent.reset();
            }
        }

        // Perform global pheromone updating
        updateEdgeGlobal();
    }

    private void updateEdgeGlobal() {

        List<Node> path = _graph.getBestPath();
        double pathLength = _graph.getBestPathLength();

        // Find all edges used by the best path
        Set<Edge> bestEdges = new HashSet<Edge>();
        for (int i = 0; i < path.size() - 1; i++) {
            Node node1 = path.get(i);
            Node node2 = path.get(i + 1);
            bestEdges.add(node1.getEdge(node2));
        }

        for (Node node1 : _graph.getNodes()) {
            for (Node node2 : _graph.getNodes()) {
                if (node1.equals(node2)) continue;

                Edge edge = node1.getEdge(node2);
                double pheromoneWeight = edge.getPheromoneWeight();
                if (pheromoneWeight > 0) {
                    pheromoneWeight = (1 - TSPSolver.DECAY_VALUE) * pheromoneWeight;
                    if (bestEdges.contains(edge)) {
                        // This edge is in the best path, so increase its pheromone level
                        pheromoneWeight = pheromoneWeight + TSPSolver.DECAY_VALUE * (1 / pathLength);
                    }
                    edge.setPheromoneWeight(pheromoneWeight);
                }
            }
        }
    }

    /**
     * Clears the display screen buffer and redraws all existing nodes and path
     * data to the buffer.
     */
    private void redrawGraph() {

        _window.getSurface().clearBuffer();

        _window.setStatusText("Running: " + _runningAlgorithm.toString());

        // Display greedy path if selected
        if (_window.isDisplayGreedyChecked()) {
            drawPath(_graph.getGreedyPath(), Color.CYAN);
        }

        if (_window.isDisplayDataChecked()) {
            // Display pheromone trails if running ANTS (SLOW!)
            if (_runningAlgorithm.equals(Algorithm.ANTS)) {
                for (Node node1 : _graph.getNodes()) {
                    for (Node node2 : _graph.getNodes()) {
                        if (node1.equals(node2)) continue;

                        Edge edge = node1.getEdge(node2);
                        float colorWeight = (float) edge.getPheromoneWeight();
                        if (colorWeight > 1.0) colorWeight = 1.0f;
                        if (colorWeight > 0.25) {
                            drawEdge(node1, node2, new Color(0f, 0f, colorWeight));
                        }
                    }
                }
            }
            // Display most recent hill-climbing tour generated if running hill climbing
            else if (_runningAlgorithm.equals(Algorithm.HILL_CLIMBING)) {
                drawPath(_currentWorkingTour, Color.RED);
            }
            // Display most recent two-opt tour generated if running two opt algorithm
            else if (_runningAlgorithm.equals(Algorithm.TWO_OPT)) {
                drawPath(_currentWorkingTour, Color.RED);
            }
        }

        // Display shortest path found if one exists
        if (_window.isDisplayBestChecked()) {
            drawPath(_graph.getBestPath(), Color.GREEN);
        }

        // Draw nodes
        for (Node node : _graph.getNodes()) {
            drawNode(node, Color.YELLOW);
        }

        // Replace the first node in greedy path with 'red' circle if greedy path is enabled 
        if (_window.isDisplayGreedyChecked()) {
            List<Node> path = _graph.getGreedyPath();
            if (path.size() > 0) {
                Node node = path.get(0);
                drawNode(node, Color.RED);
            }
        }
    }

    private void drawPath(List<Node> path, Color color) {
        if (path != null) {
            for (int i = 0; i < path.size() - 1; i++) {
                Node node1 = path.get(i);
                Node node2 = path.get(i + 1);
                drawEdge(node1, node2, color);
            }
        }
    }

    /**
     * Draws a line between the two nodes using the specified color.
     */
    private void drawEdge(Node node1, Node node2, Color color) {
        _window.getSurface().drawLine(node1.xPos(), node1.yPos(), node2.xPos(), node2.yPos(), color);
    }

    /**
     * Draws the specified node with the given color.
     */
    private void drawNode(Node node, Color color) {
        final int radius = 2;
        _window.getSurface().drawCircle(node.xPos(), node.yPos(), radius, color);
    }

}
