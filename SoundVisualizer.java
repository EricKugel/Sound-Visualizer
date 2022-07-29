import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.sound.sampled.*;
import java.io.*;

import javax.swing.*;

public class SoundVisualizer extends JFrame {
    public static final int SCREEN_HEIGHT = 480;
    public static final int SCREEN_WIDTH = 640;
    public static final int DRAWING_PANEL_WIDTH = 640;
    public static final int DRAWING_PANEL_HEIGHT = 480;

    private int lastX = -1;
    private int lastY = -1;

    private JPanel main;
    private JPanel drawingPanel;
    private JSlider hertzSlider;

    private boolean[][] pixels = new boolean[DRAWING_PANEL_HEIGHT][DRAWING_PANEL_WIDTH];
    
    public SoundVisualizer() {
        setTitle("Sound Visualizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);
        initGUI();
        pack();
    }

    private void initGUI() {
        main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.X_AXIS));
        main.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        add(main);

        drawingPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                draw(g);
            }
        };
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        drawingPanel.setPreferredSize(new Dimension(300, 200));
        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                try {
                    if (x > 0 && y > 0) {
                        drawLineTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                } catch(Exception exception) {/* Do nothing */}
                repaint();
            }

            
        });

        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }
        });
        main.add(drawingPanel);

        hertzSlider = new JSlider(JSlider.VERTICAL, 55, 440 * 2 * 2, 440);
        hertzSlider.setMajorTickSpacing(100);
        hertzSlider.setMinorTickSpacing(25);
        hertzSlider.setPaintTicks(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        for (int i = 0; i < 8; i++) {
            int hertz = (int) (55 * Math.pow(2, i));
            labelTable.put(hertz, new JLabel("" + hertz));
        }
        hertzSlider.setLabelTable(labelTable);
        hertzSlider.setPaintLabels(true);
        main.add(hertzSlider);

        JPanel buttonPanel = new JPanel();
        add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        JButton clear = new JButton("Clear");
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pixels = new boolean[DRAWING_PANEL_HEIGHT][DRAWING_PANEL_WIDTH];
                repaint();
            }
        });
        buttonPanel.add(clear);

        JButton play = new JButton("Play");
        play.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                play();
            }
        });
        buttonPanel.add(play);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stop();
            }
        });
        buttonPanel.add(stop);
    }
    
    private void draw(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, DRAWING_PANEL_WIDTH, DRAWING_PANEL_HEIGHT);
        g.setColor(Color.RED);
        g.drawLine(0, DRAWING_PANEL_HEIGHT / 2, DRAWING_PANEL_WIDTH, DRAWING_PANEL_HEIGHT / 2);
        g.setColor(Color.BLACK);
        for (int row = 0; row < DRAWING_PANEL_HEIGHT; row++) {
            for (int col = 0; col < DRAWING_PANEL_WIDTH; col++) {
                if (pixels[row][col]) {
                    g.drawLine(col, row, col + 1, row + 1);
                }
            }
        }
        
    }

    private void drawLineTo(int x, int y) {
        int steps = (int) Math.hypot(x, y);
        for (int i = 0; i < steps; i++) {
            double percent = (double) i / steps;
            pixels[(int) ((y - lastY) * percent + lastY)][(int) ((x - lastX) * percent + lastX)] = true;
        }
    }

    private void play() {
        int frequency = hertzSlider.getValue();
        int sampleRate = 41000;
        double[] buffer = new double[sampleRate / frequency];
        for (int i = 0; i < buffer.length; i++) {
            int col = (int) ((double) i / buffer.length) * DRAWING_PANEL_WIDTH;
            int max = 0;
            for (int row = DRAWING_PANEL_HEIGHT - 1; row >= 0; row--) {
                if (pixels[row][col]) {
                    max = row;
                }
            }
            buffer[col] = ((DRAWING_PANEL_HEIGHT - max) - DRAWING_PANEL_HEIGHT / 2) / (double) (DRAWING_PANEL_HEIGHT / 2);
        }

        byte[] byteBuffer = new byte[buffer.length * 2];
        
        int bufferIndex = 0;
        for (int i = 0; i < byteBuffer.length; i += 2) {
            int x = (int) buffer[bufferIndex++] * 32767;
            byteBuffer[i] = (byte) x;
            byteBuffer[i + 1] = (byte) (x >>> 8);
        }

        boolean bigEndian = false;
        boolean signed = true;

        int bits = 16;
        int channels = 1;

        AudioFormat format = new AudioFormat((float) sampleRate, bits, channels, signed, bigEndian);
        ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
        AudioInputStream audioInputStream = new AudioInputStream(bais, format, buffer.length);
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public static void main(String[] arg0) {
        new SoundVisualizer();
    }
}