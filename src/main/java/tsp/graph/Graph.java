package tsp.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Graph {

    private Set<Node> _nodes = new HashSet<Node>();
    private List<Node> _greedyPath = new ArrayList<Node>();
    private long _greedyPathLength = 0;

    private List<Node> _bestPath = new ArrayList<Node>();
    private long _bestPathLength = 0;
    private long _elapsedTimeToBestPath = 0;

    /**
     * Erase all nodes and edges from graph.
     */
    public void clear() {
        _nodes.clear();
        resetCache();
    }

    private void resetCache() {
        _greedyPath.clear();
        _greedyPathLength = 0;
        _bestPath.clear();
        _bestPathLength = 0;
        _elapsedTimeToBestPath = 0;
    }

    /**
     * Create a new node and add it to the graph. This method will also
     * automatically create edges linking the new node to all existing nodes.
     * Returns the newly created node.
     * 
     * @param xPos
     * @param yPos
     */
    public Node createNode(int xPos, int yPos) {

        // Create the new node
        Node newNode = new Node(xPos, yPos);

        // Check if node is too close to an existing node; if so, do nothing!
        final int MIN_DISTANCE = 5;
        for (Node node : _nodes) {
            if (newNode.distance(node) < MIN_DISTANCE) return null;
        }

        // Create edges linking this node to all existing nodes
        for (Node node : _nodes) {
            createEdge(newNode, node);
        }

        // And finally, add node to graph
        _nodes.add(newNode);

        // Reset cached data
        resetCache();

        return newNode;
    }

    /**
     * Create an edge connecting the specified nodes.
     */
    private void createEdge(Node node1, Node node2) {
        node1.addEdge(node2);
    }

    /**
     * Calculates the length of a given path.
     * @param path
     * @return
     */
    public long calculatePathLength(List<Node> path) {
        long pathLength = 0;
        if (path != null) {
            for (int i = 0; i < path.size() - 1; i++) {
                long distance = path.get(i).distance(path.get(i + 1));
                pathLength += distance;
            }
        }
        return pathLength;
    }

    /**
     * Finds the best Greedy path inside the current graph and caches it
     * 
     * @return
     */
    private List<Node> calculateGreedyPath(int startNodeIndex) {

        List<Node> greedyPath = new ArrayList<Node>();
        Set<Node> remainingNodes = getNodes();

        if (_nodes.size() < 3) {
            return greedyPath;
        }

        Node currNode = remainingNodes.iterator().next();
        greedyPath.add(currNode);
        remainingNodes.remove(currNode);

        while (remainingNodes.size() > 0) {
            // Find the closest node in remaining nodes list
            Node closestNode = null;
            long closestDistance = 0;
            for (Node node : remainingNodes) {
                long distance = currNode.distance(node);
                if (closestNode == null || distance < closestDistance) {
                    closestNode = node;
                    closestDistance = distance;
                }
            }
            // Add closest node to our path, set it as current node, and remove it from remaining nodes
            greedyPath.add(closestNode);
            currNode = closestNode;
            remainingNodes.remove(closestNode);
        }

        // Add the first node in path to the end of path to travel back to starting point
        greedyPath.add(greedyPath.get(0));

        return greedyPath;
    }

    /**
     * Returns the cached greedy path (or calculates one if none is cached).
     */
    public List<Node> getGreedyPath() {
        if (_greedyPath.isEmpty()) {
            for (int i = 0; i < _nodes.size(); i++) {
                List<Node> currentTour = calculateGreedyPath(i);
                long currentLength = calculatePathLength(currentTour);
                if (_greedyPath.isEmpty() || currentLength < _greedyPathLength) {
                    _greedyPath = currentTour;
                    _greedyPathLength = currentLength;
                }
            }
        }
        return new ArrayList<Node>(_greedyPath);
    }

    /**
     * Returns the length of the currently cached greedy path.
     */
    public long getGreedyPathLength() {
        if (_greedyPathLength == 0) {
            getGreedyPath();
        }
        return _greedyPathLength;
    }

    /**
     * Attempts to set the best path of the graph to the specified path.
     * Returns true if the argument is the best path found so far. Method
     * returns false if a better path was already found (and no change is made
     * to the best path).
     * @param path
     * @return
     */
    public boolean setBestPath(List<Node> path) {
        if (path != null) {
            long pathLength = calculatePathLength(path);
            if (_bestPath.isEmpty() || pathLength < _bestPathLength) {
                _bestPath = new ArrayList<Node>(path);
                _bestPathLength = calculatePathLength(path);
                return true;
            }
        }
        return false;
    }

    public void setElapsedTime(long elapsedTime) {
        _elapsedTimeToBestPath = elapsedTime;
    }

    public long getElapsedTime() {
        return _elapsedTimeToBestPath;
    }

    public List<Node> getBestPath() {
        if (!_bestPath.isEmpty() && !_bestPath.containsAll(_nodes)) {
            _bestPath.clear();
            _bestPathLength = 0;
        }
        return new ArrayList<Node>(_bestPath);
    }

    public long getBestPathLength() {
        return _bestPathLength;
    }

    /**
     * Returns the list of all nodes in the graph.
     */
    public Set<Node> getNodes() {
        return new HashSet<Node>(_nodes);
    }

    public int getNodeCount() {
        return _nodes.size();
    }

}
