package tsp.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;

public class TSPDrawPane extends JPanel {

    private static final long serialVersionUID = 1356708741246858622L;
    private Map<Object, Color> _objectMap = new LinkedHashMap<Object, Color>();

    TSPDrawPane() {
        super(true);
    }

    public synchronized void clearBuffer() {
        _objectMap.clear();
    }

    public synchronized void drawCircle(int xPos, int yPos, int radius, Color color) {
        Ellipse2D.Float circle = new Ellipse2D.Float(xPos - radius, yPos - radius, radius * 2, radius * 2);
        _objectMap.remove(circle);
        _objectMap.put(circle, color);
    }

    public synchronized void drawLine(int x1, int y1, int x2, int y2, Color color) {
        Line2D.Float line = new Line2D.Float(x1, y1, x2, y2);
        _objectMap.remove(line);
        _objectMap.put(line, color);
    }

    @Override
    public synchronized void paintComponent(Graphics comp) {
        //
        Graphics2D comp2D = (Graphics2D) comp;
        comp2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Clear background
        comp2D.setColor(getBackground());
        comp2D.fillRect(0, 0, getWidth(), getHeight());

        for (Object object : _objectMap.keySet()) {
            Color color = _objectMap.get(object);
            comp2D.setColor(color);
            if (object instanceof Line2D.Float) {
                comp2D.draw((Line2D.Float) object);
            }
            else if (object instanceof Ellipse2D.Float) {
                comp2D.fill((Ellipse2D.Float) object);
            }
        }
    }

}
