package tsp.app;

import tsp.graph.Edge;
import tsp.graph.Graph;
import tsp.graph.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Agent {

    private Graph _graph;
    private List<Node> _currentTour = new ArrayList<Node>();
    private List<Node> _remainingNodes = new ArrayList<Node>();
    private List<Node> _lastCompletedTour = new ArrayList<Node>();
    private long _lastCompletedTourLength = 0;
    private Random _rand = new Random();

    public Agent(Graph graph) {
        _graph = graph;
    }

    /**
     * Resets the agent to prepare for another tour of the graph.
     */
    public void reset() {
        _currentTour.clear();
        _remainingNodes.clear();
    }

    /**
     * Advances the agent one node in its tour of the graph.
     * Returns true if agent is done with its tours. 
     */
    public void update() {

        if (_currentTour.isEmpty()) {
            // Not on any tour yet? Start this agent at a random node in the graph
            _remainingNodes = new ArrayList<Node>(_graph.getNodes());
            if (_remainingNodes.isEmpty()) return;

            int startNodeIndex = _rand.nextInt(_remainingNodes.size());
            _currentTour.add(_remainingNodes.remove(startNodeIndex));
        }

        if (!_remainingNodes.isEmpty()) {
            // We still have nodes to visit, so find the best one to visit next!

            // Retrieve the last node we visited and figure out the next one to go to
            Node currentNode = _currentTour.get(_currentTour.size() - 1);
            Node nextNode = null;

            double val = _rand.nextDouble();
            if (val < TSPSolver.Q0_PARAM) {
                // Choose the highest weighted pheromone trail to follow
                nextNode = pickHighestWeightedNode(currentNode, _remainingNodes);
            }
            else {
                // Pick a edge using a random chance based on pheromone levels
                nextNode = pickRandomWeightedNode(currentNode, _remainingNodes);
            }

            if (nextNode == null) {
                // Next node is still null, so pick a node at random
                nextNode = pickRandomNode(currentNode, _remainingNodes);
            }

            _currentTour.add(nextNode);
            _remainingNodes.remove(nextNode);
            if (_remainingNodes.isEmpty()) {
                // We're at the end of the tour? Go back to the start!
                _currentTour.add(_currentTour.get(0));

                _lastCompletedTour.clear();
                _lastCompletedTour.addAll(_currentTour);
                _lastCompletedTourLength = _graph.calculatePathLength(_lastCompletedTour);
            }
        }
    }

    /**
     * Returns a boolean indicating if the agent is done with its tour.
     */
    public boolean complete() {
        return (!_currentTour.isEmpty() && _remainingNodes.isEmpty());
    }

    /**
     * Choose the path with the highest pheromone weight to follow from the current node.
     */
    private Node pickHighestWeightedNode(Node currentNode, List<Node> remainingNodes) {
        // Find highest weighted pheromone edge from this node and follow it
        Node bestNode = null;
        double highestPheromoneWeight = 0;
        for (Node node : _remainingNodes) {
            double pheromoneWeight = calculateAdjustedPheromoneWeight(currentNode, node);
            if (pheromoneWeight != 0 && (bestNode == null || pheromoneWeight > highestPheromoneWeight)) {
                bestNode = node;
                highestPheromoneWeight = pheromoneWeight;
            }
        }

        return bestNode;
    }

    /**
     * Randomly pick the next node to follow using the pheromone values to weight the random selection.
     */
    private Node pickRandomWeightedNode(Node currentNode, List<Node> remainingNodes) {
        // Calculate the probability of choosing each remaining node
        Node nextNode = null;
        Map<Node, Double> probMap = calculateRandomProportionRule(currentNode, _remainingNodes);
        double probValue = _rand.nextDouble();
        double sumValue = 0;
        Node lastNode = null;
        for (Node node : probMap.keySet()) {
            lastNode = node;
            sumValue += probMap.get(node);
            if (sumValue >= probValue) {
                nextNode = node;
                break;
            }
        }
        if (nextNode == null) nextNode = lastNode;
        return nextNode;
    }

    private Node pickRandomNode(Node currentNode, List<Node> remainingNodes) {
        int randIndex = _rand.nextInt(remainingNodes.size());
        return remainingNodes.get(randIndex);
    }

    @SuppressWarnings("unused")
    private Node pickClosestNode(Node currentNode, List<Node> remainingNodes) {
        Node closestNode = null;
        long closestDistance = 0;
        for (Node node : remainingNodes) {
            long distance = currentNode.distance(node);
            if (closestNode == null || distance < closestDistance) {
                closestNode = node;
                closestDistance = distance;
            }
        }
        return closestNode;
    }

    /**
     * Returns the current tour the agent is on.
     */
    public List<Node> getTour() {
        return new ArrayList<Node>(_currentTour);
    }

    /**
     * Returns the most recently completed tour by this agent.
     */
    public List<Node> getLastCompletedTour() {
        return new ArrayList<Node>(_lastCompletedTour);
    }

    /**
     * Returns the distance of the tour returned from getTour()
     */
    public long getTourDistance() {
        return _graph.calculatePathLength(_currentTour);
    }

    public long getLastCompletedTourDistance() {
        return _lastCompletedTourLength;
    }

    /**
     * Performs local pheromone trail updating for the currently completed tour on the agent. 
     */
    public void updateEdgeLocal() {
        if (!complete()) return;

        for (int i = 0; i < _currentTour.size() - 1; i++) {
            Node node1 = _currentTour.get(i);
            Node node2 = _currentTour.get(i + 1);
            Edge edge = node1.getEdge(node2);
            double pheromoneWeight = edge.getPheromoneWeight();
            pheromoneWeight = (1 - TSPSolver.DECAY_VALUE) * pheromoneWeight + TSPSolver.INCREASE_WEIGHT;
            edge.setPheromoneWeight(pheromoneWeight);
        }
    }

    /**
     * Returns a map giving the probabilities to visit each node in the remaining nodes list
     * from the specified node.
     */
    private Map<Node, Double> calculateRandomProportionRule(Node nextNode, List<Node> remainingNodes) {
        Map<Node, Double> probabilityMap = new LinkedHashMap<Node, Double>();

        for (Node sNode : remainingNodes) {

            // Calculate the numerator values
            double numerator = calculateAdjustedPheromoneWeight(nextNode, sNode);

            // Calculate the denominator values
            double denominator = 0;
            for (Node uNode : remainingNodes) {
                denominator += calculateAdjustedPheromoneWeight(nextNode, uNode);
            }

            if (numerator > Double.MIN_VALUE && denominator > Double.MIN_VALUE) {
                double nodeProbability = numerator / denominator;
                probabilityMap.put(sNode, nodeProbability);
            }
        }

        // Normalize probability map to add up to 1.0 for easier random picking
        double sumProbability = 0;
        for (Node node : probabilityMap.keySet()) {
            double nodeProbability = probabilityMap.get(node);
            sumProbability += nodeProbability;
        }

        for (Node node : probabilityMap.keySet()) {
            double nodeProbability = probabilityMap.get(node);
            nodeProbability = (nodeProbability / sumProbability) * 1.0;
            probabilityMap.put(node, nodeProbability);
        }

        return probabilityMap;
    }

    private double calculateAdjustedPheromoneWeight(Node node1, Node node2) {
        double t = getPheromoneWeight(node1, node2);
        double n = Math.pow(getInverseDistance(node1, node2), TSPSolver.DISTANCE_WEIGHT);
        double value = t * n;
        return (value > Double.MIN_VALUE) ? value : 0;
    }

    private double getPheromoneWeight(Node node1, Node node2) {
        return node1.getEdge(node2).getPheromoneWeight();
    }

    private double getInverseDistance(Node node1, Node node2) {
        return 1.0d / node1.distance(node2);
    }
}
