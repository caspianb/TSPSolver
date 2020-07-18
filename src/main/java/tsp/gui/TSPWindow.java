package tsp.gui;

import tsp.graph.Graph;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.swing.*;

public class TSPWindow extends JFrame implements MouseListener, KeyListener {

    private static final long serialVersionUID = 556431231692371497L;

    private static enum Button {
        GENERATE,
        RELEASE_ANTS,
        HILL_CLIMBING,
        TWO_OPT,
        THREE_OPT
    }

    private JPanel _controlPane;
    private JTextField _txtGenerateNodeCount;
    private JTextField _txtNodeCount;
    private JTextField _txtGreedyPathLength;
    private JTextField _txtBestPathLength;
    private JTextField _txtElapsedTimeToLastSolution;
    private JTextField _txtRandomSeedValue;
    private JLabel _txtStatusInfo;
    private JCheckBox _chkDisplayGreedyPath;
    private JCheckBox _chkDisplayBestPath;
    private JCheckBox _chkDisplayBackgroundData;

    private boolean _releaseAntsButtonClicked = false;
    private boolean _hillClimbingButtonClicked = false;
    private boolean _twoOptButtonClicked = false;
    private boolean _threeOptButtonClicked = false;
    private boolean _generateButtonClicked = false;

    private Set<Integer> _keysDown = new HashSet<Integer>();

    private TSPDrawPane _drawPane;
    private Graph _graph;

    public TSPWindow(int width, int height, String windowName, Graph graph) {
        //
        setTitle(windowName);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);
        _graph = graph;
        setResizable(true);

        setFocusable(true);
        addKeyListener(this);

        Container contentPane = getContentPane();
        BorderLayout layout = new BorderLayout();
        contentPane.setLayout(layout);

        initControlPane(contentPane);
        initDrawPane(contentPane);
        initStatusBarPar(contentPane);

        setVisible(true);
    }

    /**
     * Create top control panel pane and buttons/controls.
     */
    private void initControlPane(Container contentPane) {
        //
        _controlPane = new JPanel(new GridLayout(2, 2));
        JPanel topLeftPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel topRightPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel bottomLeftPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel bottomRightPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        //
        // Setup top left pane
        //
        JButton btnReleaseAnts = new JButton("Release Ants");
        btnReleaseAnts.addActionListener(actionEvent -> _releaseAntsButtonClicked = true);
        topLeftPane.add(btnReleaseAnts);

        JButton btnHillClimbing = new JButton("Hill Climbing");
        btnHillClimbing.addActionListener(actionEvent -> _hillClimbingButtonClicked = true);
        topLeftPane.add(btnHillClimbing);

        JButton btnTwoOpt = new JButton("Two Opt");
        btnTwoOpt.addActionListener(actionEvent -> _twoOptButtonClicked = true);
        topLeftPane.add(btnTwoOpt);

        JButton btnThreeOpt = new JButton("Three Opt");
        btnThreeOpt.addActionListener(actionEvent -> _threeOptButtonClicked = true);
        topLeftPane.add(btnThreeOpt);

        //
        // Setup bottom left pane
        //

        _chkDisplayGreedyPath = new JCheckBox("Display Greedy");
        _chkDisplayBestPath = new JCheckBox("Display Best");
        _chkDisplayBackgroundData = new JCheckBox("Display Pheromones/Hill-climbing Tour");

        bottomLeftPane.add(_chkDisplayGreedyPath);
        bottomLeftPane.add(_chkDisplayBestPath);
        bottomLeftPane.add(_chkDisplayBackgroundData);

        //
        // Setup top right pane
        //
        JButton btnGenerate = new JButton("Generate Cities");
        btnGenerate.addActionListener(this::onClickGenerateButton);

        _txtGenerateNodeCount = new NumericTextField("50", 10);
        topRightPane.add(new JLabel("Number: "));
        topRightPane.add(_txtGenerateNodeCount);
        topRightPane.add(btnGenerate);

        //
        // Setup bottom right pane
        //
        bottomRightPane.add(new JLabel("Random Seed: "));
        _txtRandomSeedValue = new NumericTextField("", 10);
        bottomRightPane.add(_txtRandomSeedValue);

        _controlPane.add(topLeftPane);
        _controlPane.add(topRightPane);
        _controlPane.add(bottomLeftPane);
        _controlPane.add(bottomRightPane);
        contentPane.add(_controlPane, BorderLayout.NORTH);
    }

    /**
     * Create output drawing pane.
     */
    private void initDrawPane(Container contentPane) {
        _drawPane = new TSPDrawPane();
        _drawPane.setBackground(Color.BLACK);
        contentPane.add(_drawPane, BorderLayout.CENTER);

        _drawPane.addMouseListener(this);
    }

    /**
     * Create bottom status bar bar.
     */
    private void initStatusBarPar(Container contentPane) {
        JPanel statusBar = new JPanel(new BorderLayout());
        JPanel statusBarRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel statusBarLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBackground(Color.BLACK);
        statusBarRight.setBackground(Color.BLACK);
        statusBarLeft.setBackground(Color.BLACK);

        _txtStatusInfo = new JLabel("");
        _txtStatusInfo.setForeground(Color.WHITE);
        statusBarLeft.add(_txtStatusInfo);

        JLabel lblElapsedTime = new JLabel("Time to find solution: ");
        lblElapsedTime.setForeground(Color.WHITE);
        statusBarRight.add(lblElapsedTime);

        _txtElapsedTimeToLastSolution = new JTextField("", 10);
        _txtElapsedTimeToLastSolution.setEnabled(false);
        _txtElapsedTimeToLastSolution.setBackground(Color.BLACK);
        _txtElapsedTimeToLastSolution.setForeground(Color.WHITE);
        statusBarRight.add(_txtElapsedTimeToLastSolution);

        JLabel lblBestLength = new JLabel("Best path length: ");
        lblBestLength.setForeground(Color.WHITE);
        statusBarRight.add(lblBestLength);

        _txtBestPathLength = new NumericTextField("", 5);
        _txtBestPathLength.setEditable(false);
        _txtBestPathLength.setBackground(Color.BLACK);
        _txtBestPathLength.setForeground(Color.WHITE);
        statusBarRight.add(_txtBestPathLength);

        JLabel lblGreedyLength = new JLabel("Greedy path length: ");
        lblGreedyLength.setForeground(Color.WHITE);
        statusBarRight.add(lblGreedyLength);

        _txtGreedyPathLength = new NumericTextField("", 5);
        _txtGreedyPathLength.setEditable(false);
        _txtGreedyPathLength.setBackground(Color.BLACK);
        _txtGreedyPathLength.setForeground(Color.WHITE);
        statusBarRight.add(_txtGreedyPathLength);

        JLabel lblNodeCount = new JLabel("Nodes: ");
        lblNodeCount.setForeground(Color.WHITE);
        statusBarRight.add(lblNodeCount);

        _txtNodeCount = new NumericTextField("0", 5);
        _txtNodeCount.setEditable(false);
        _txtNodeCount.setBackground(Color.BLACK);
        _txtNodeCount.setForeground(Color.WHITE);

        statusBarRight.add(_txtNodeCount);

        statusBar.add(statusBarLeft, BorderLayout.WEST);
        statusBar.add(statusBarRight, BorderLayout.EAST);

        contentPane.add(statusBar, BorderLayout.SOUTH);
    }

    private void setRandomSeedValue(long seedValue) {
        _txtRandomSeedValue.setText(String.valueOf(seedValue));
    }

    private long getRandomSeedValue() {
        if (!_txtRandomSeedValue.getText().isEmpty()) {
            try {
                return ((NumericTextField) _txtRandomSeedValue).getNumberValue().longValue();
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    private int getGenerateNodeCount() {
        if (!_txtGenerateNodeCount.getText().isEmpty()) {
            try {
                return ((NumericTextField) _txtGenerateNodeCount).getNumberValue().intValue();
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return 100; // Default to generating 100 nodes
    }

    public TSPDrawPane getSurface() {
        return _drawPane;
    }

    public boolean isDisplayGreedyChecked() {
        return _chkDisplayGreedyPath.isSelected();
    }

    public boolean isDisplayBestChecked() {
        return _chkDisplayBestPath.isSelected();
    }

    public boolean isDisplayDataChecked() {
        return _chkDisplayBackgroundData.isSelected();
    }

    /**
     * Sets the status text in the status pane.
     */
    public void setStatusText(String text) {
        _txtStatusInfo.setText(text);
    }

    /**
     * Returns true if the generate button has been clicked since the last call to this method.
     */
    public boolean generateButtonClicked() {
        boolean clicked = _generateButtonClicked;
        _generateButtonClicked = false;
        return clicked;
    }

    /**
     * Returns if the release ants button has been clicked since the last call to this method.
     */
    public boolean releaseAntsButtonClicked() {
        boolean clicked = _releaseAntsButtonClicked;
        _releaseAntsButtonClicked = false;
        return clicked;
    }

    /**
     * Returns if the hill climbing button has been clicked since the last call to this method.
     */
    public boolean hillClimbingButtonClicked() {
        boolean clicked = _hillClimbingButtonClicked;
        _hillClimbingButtonClicked = false;
        return clicked;
    }

    /**
     * Returns if the two opt button has been clicked since the last call to this method.
     */
    public boolean twoOptButtonClicked() {
        boolean clicked = _twoOptButtonClicked;
        _twoOptButtonClicked = false;
        return clicked;
    }

    /**
     * Returns if the two opt button has been clicked since the last call to this method.
     */
    public boolean threeOptButtonClicked() {
        boolean clicked = _threeOptButtonClicked;
        _threeOptButtonClicked = false;
        return clicked;
    }

    // **********************************************************
    // Event listeners for buttons/mouse events defined below
    // **********************************************************

    public void onClickGenerateButton(ActionEvent event) {
        //
        _graph.clear();
        Integer numberToGenerate = getGenerateNodeCount();

        long seedValue = getRandomSeedValue();
        if (seedValue == 0) {
            seedValue = System.currentTimeMillis();
            setRandomSeedValue(seedValue);
        }
        Random rand = new Random(seedValue);
        for (int i = 0; i < numberToGenerate; i++) {
            int padding = 15;
            int xPos = rand.nextInt(getSurface().getWidth() - padding * 2) + padding;
            int yPos = rand.nextInt(getSurface().getHeight() - padding * 2) + padding;
            _graph.createNode(xPos, yPos);
        }
        _generateButtonClicked = true;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && isKeyPressed(KeyEvent.VK_CONTROL)) {
            _graph.createNode(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        _keysDown.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        _keysDown.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private boolean isKeyPressed(int vkCode) {
        return _keysDown.contains(vkCode);
    }

    @Override
    public void repaint() {
        super.repaint();

        // Update our status bar using the most recent graph data
        int numNodes = _graph.getNodeCount();
        _txtNodeCount.setText(String.valueOf(numNodes));

        long greedyPathLength = _graph.getGreedyPathLength();
        _txtGreedyPathLength.setText(String.valueOf(greedyPathLength));

        long bestPathLength = _graph.getBestPathLength();
        _txtBestPathLength.setText(String.valueOf(bestPathLength));

        long elapsedTime = _graph.getElapsedTime();
        _txtElapsedTimeToLastSolution.setText(String.valueOf(elapsedTime) + "ms");
    }

}
