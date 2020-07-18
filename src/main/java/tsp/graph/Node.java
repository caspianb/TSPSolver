package tsp.graph;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

public class Node {

    private int _xPos = 0;
    private int _yPos = 0;

    private Map<Node, Edge> _edges = new HashMap<Node, Edge>();

    public Node(int xPos, int yPos) {
        _xPos = xPos;
        _yPos = yPos;
    }

    public int xPos() {
        return _xPos;
    }

    public int yPos() {
        return _yPos;
    }

    public long distance(Node other) {
        return (long) Math.round(Point2D.distance(this._xPos, this._yPos, other._xPos, other._yPos));
    }

    public void addEdge(Node other) {
        Edge edge = new Edge(this, other);
        this._edges.put(other, edge);
        other._edges.put(this, edge);
    }

    public Edge getEdge(Node other) {
        return _edges.get(other);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + _xPos;
        result = prime * result + _yPos;
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
        Node other = (Node) obj;
        if (_xPos != other._xPos) {
            return false;
        }
        if (_yPos != other._yPos) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Node [_xPos=" + _xPos + ", _yPos=" + _yPos + "]";
    }
}
