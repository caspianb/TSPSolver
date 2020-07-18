package tsp.graph;

import tsp.app.TSPSolver;

public class Edge {

    private Node _node1;
    private Node _node2;
    private long _distanceWeight = 0;
    private double _pheromoneWeight = TSPSolver.INIT_WEIGHT;

    public Edge(Node node1, Node node2) {
        if (node1.equals(node2)) {
            throw new RuntimeException("Can not create edge from node to itself!");
        }
        _node1 = node1;
        _node2 = node2;
        _distanceWeight = node1.distance(node2);
    }

    public boolean contains(Node node) {
        return (_node1.equals(node) || _node2.equals(node));
    }

    public boolean connects(Node node1, Node node2) {
        return ((_node1.equals(node1) && _node2.equals(node2)) || (_node2.equals(node1) && _node1.equals(node2)));
    }

    public long getDistanceWeight() {
        return _distanceWeight;
    }

    public void setPheromoneWeight(double pheromoneWeight) {
        _pheromoneWeight = pheromoneWeight;
    }

    public double getPheromoneWeight() {
        return _pheromoneWeight;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_node1 == null) ? 0 : _node1.hashCode());
        result = prime * result + ((_node2 == null) ? 0 : _node2.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        Edge other = (Edge) obj;
        return (other.contains(_node1) && (other.contains(_node2)));
    }

    @Override
    public String toString() {
        return "Edge [_node1=" + _node1 + ", _node2=" + _node2 + ", _weight=" + _distanceWeight + "]";
    }
}
