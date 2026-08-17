package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.qd.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Live clickable heatmap for any two-axis generic quality archive. */
public final class QualityArchiveHeatmapPanel<C> extends JPanel {
    private static final int LEFT = 125, RIGHT = 35, TOP = 48, BOTTOM = 96;
    private static final Color EMPTY = new Color(42, 45, 51);
    private final String title;
    private final QualityDescriptor<C> descriptor;
    private final QualityEliteRenderer<C> renderer;
    private final AtomicReference<QualityArchiveSnapshot<C>> pending = new AtomicReference<>();
    private QualityArchiveSnapshot<C> displayed;

    public QualityArchiveHeatmapPanel(String title, QualityDescriptor<C> descriptor,
                                      QualityEliteRenderer<C> renderer) {
        if (descriptor.axes().size() != 2) throw new IllegalArgumentException("Heatmaps require exactly two axes");
        this.title = title;
        this.descriptor = descriptor;
        this.renderer = renderer;
        displayed = new QualityArchiveSnapshot<>(0, Map.of(), descriptor.axes());
        setBackground(new Color(30, 33, 38));
        setForeground(new Color(235, 237, 240));
        setPreferredSize(new Dimension(940, 700));
        setToolTipText("");
        new Timer(100, ignored -> consumeLatest()).start();
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) { openEliteAt(event.getPoint()); }
        });
    }

    /** Thread-safe and coalescing: evolution never waits for Swing painting. */
    public void submit(QualityArchiveSnapshot<C> snapshot) { pending.set(snapshot); }

    private void consumeLatest() {
        QualityArchiveSnapshot<C> latest = pending.getAndSet(null);
        if (latest != null) { displayed = latest; repaint(); }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            DescriptorAxis xAxis = descriptor.axes().get(0), yAxis = descriptor.axes().get(1);
            Rectangle grid = gridBounds();
            int cellWidth = grid.width / xAxis.bins(), cellHeight = grid.height / yAxis.bins();
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            for (int x = 0; x < xAxis.bins(); x++) for (int y = 0; y < yAxis.bins(); y++)
                drawCell(g, grid, cellWidth, cellHeight, x, y);
            drawAxes(g, grid, cellWidth, cellHeight, xAxis, yAxis);
            drawLegend(g, grid);
        } finally { g.dispose(); }
    }

    private void drawCell(Graphics2D g, Rectangle grid, int width, int height, int xBin, int yBin) {
        int x = grid.x + xBin * width;
        int y = grid.y + (descriptor.axes().get(1).bins() - 1 - yBin) * height;
        C elite = displayed.elites().get(new BehaviorCell(java.util.List.of(xBin, yBin)));
        double value = elite == null ? 0 : renderer.colourValue(elite);
        g.setColor(elite == null ? EMPTY : ViridisColourMap.colour(value));
        g.fillRect(x + 2, y + 2, width - 4, height - 4);
        if (elite == null) return;
        g.setColor(value > 0.55 ? new Color(25, 27, 30) : Color.WHITE);
        drawCentred(g, renderer.firstLine(elite), x, y + height / 2 - 4, width);
        drawCentred(g, renderer.secondLine(elite), x, y + height / 2 + 17, width);
    }

    private void drawAxes(Graphics2D g, Rectangle grid, int width, int height,
                          DescriptorAxis xAxis, DescriptorAxis yAxis) {
        g.setColor(getForeground());
        for (int bin = 0; bin < xAxis.bins(); bin++)
            drawCentred(g, xAxis.binLabels().get(bin), grid.x + bin * width,
                    grid.y + grid.height + 24, width);
        for (int bin = 0; bin < yAxis.bins(); bin++) {
            int y = grid.y + (yAxis.bins() - 1 - bin) * height + height / 2 + 5;
            String label = yAxis.binLabels().get(bin);
            g.drawString(label, grid.x - 12 - g.getFontMetrics().stringWidth(label), y);
        }
        g.setFont(getFont().deriveFont(Font.BOLD, 15f));
        drawCentred(g, xAxis.label(), grid.x, grid.y + grid.height + 55, grid.width);
        g.rotate(-Math.PI / 2);
        drawCentred(g, yAxis.label(), -grid.y - grid.height, grid.x - 88, grid.height);
        g.rotate(Math.PI / 2);
        g.setFont(getFont().deriveFont(Font.BOLD, 18f));
        g.drawString("%s — %,d evaluations — %d / %d niches".formatted(title,
                displayed.evaluations(), displayed.elites().size(), xAxis.bins() * yAxis.bins()), grid.x, 29);
    }

    private void drawLegend(Graphics2D g, Rectangle grid) {
        int x = grid.x + grid.width - 175, y = grid.y + grid.height + 70, width = 175;
        for (int i = 0; i < width; i++) {
            g.setColor(ViridisColourMap.colour(i / (double) (width - 1)));
            g.drawLine(x + i, y, x + i, y + 12);
        }
        g.setColor(getForeground());
        g.setFont(getFont().deriveFont(11f));
        g.drawString("quality / (1 + violations)", x, y - 3);
    }

    private void openEliteAt(Point point) {
        BehaviorCell cell = cellAt(point);
        if (cell == null) return;
        C elite = displayed.elites().get(cell);
        if (elite != null) renderer.open(elite);
    }

    @Override public String getToolTipText(MouseEvent event) {
        BehaviorCell cell = cellAt(event.getPoint());
        if (cell == null) return null;
        C elite = displayed.elites().get(cell);
        return elite == null ? "Empty niche" : renderer.tooltip(elite);
    }

    private BehaviorCell cellAt(Point point) {
        Rectangle grid = gridBounds();
        if (!grid.contains(point)) return null;
        int xBins = descriptor.axes().get(0).bins(), yBins = descriptor.axes().get(1).bins();
        int width = grid.width / xBins, height = grid.height / yBins;
        int x = Math.min((point.x - grid.x) / width, xBins - 1);
        int row = Math.min((point.y - grid.y) / height, yBins - 1);
        return new BehaviorCell(java.util.List.of(x, yBins - 1 - row));
    }

    private Rectangle gridBounds() {
        int xBins = descriptor.axes().get(0).bins(), yBins = descriptor.axes().get(1).bins();
        int width = Math.max(xBins, getWidth() - LEFT - RIGHT);
        int height = Math.max(yBins, getHeight() - TOP - BOTTOM);
        return new Rectangle(LEFT, TOP, width - width % xBins, height - height % yBins);
    }

    private void drawCentred(Graphics2D g, String text, int x, int baseline, int width) {
        g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, baseline);
    }
}
