package chaosgame;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

public final class ChaosGame extends Canvas
{
    private static final long serialVersionUID = -8353775565998489464L;
    
    private Random random;
    private int numberOfSides;
    private double distanceFraction;
    private Polygon polygon;
    private List<int[]> points;
    
    public ChaosGame(final Random random,
                     final int numberOfSides,
                     final double distanceFraction)
    {
        this.random = random;
        this.numberOfSides = numberOfSides;
        this.distanceFraction = 1.0d - distanceFraction;
        polygon = null;
        points = new ArrayList<int[]>();
        this.setBackground(Color.WHITE);
    }
    
    public ChaosGame(final int numberOfSides,
                     final double distanceFraction)
    {
        this(new Random(), numberOfSides, distanceFraction);
    }
    
    private static final int[][] getPolygonVertices(final int sides,
                                                    final int sideLength,
                                                    final int[] centre)
    {
        double theta = 2.0d * Math.PI / (double) sides;
        double x1 = 0.0d;
        double y1 = 0.0d;
        double x2;
        double y2;
        int[][] vertices = new int[sides][];
        for (int vertex = 0; vertex < vertices.length; ++vertex)
        {
            vertices[vertex] = new int[] { (int) x1 + centre[0] / 2, (int) y1 + centre[1] / 2 };
            x2 = x1 + sideLength * Math.cos(theta * vertex);
            y2 = y1 + sideLength * Math.sin(theta * vertex);
            x1 = x2;
            y1 = y2;
        }
        return vertices;
    }
    
    private static final int[] getRandomPointInPolygon(final Random random,
                                                       final Polygon polygon)
    {
        Rectangle2D bounds2d = polygon.getBounds2D();
        double x = bounds2d.getMinX() + (bounds2d.getMaxX() - bounds2d.getMinX()) * random.nextDouble();
        double y = bounds2d.getMinY() + (bounds2d.getMaxY() - bounds2d.getMinY()) * random.nextDouble();
        while (!polygon.contains(x, y))
        {
            x = bounds2d.getMinX() + (bounds2d.getMaxX() - bounds2d.getMinX()) * random.nextDouble();
            y = bounds2d.getMinY() + (bounds2d.getMaxY() - bounds2d.getMinY()) * random.nextDouble();
        }
        return new int[] { (int) x, (int) y };
    }
    
    private static final int[] getRandomVertex(final Random random,
                                               final Polygon polygon)
    {
        int point = random.nextInt(polygon.npoints);
        return new int[] { polygon.xpoints[point], polygon.ypoints[point] };
    }
    
    private final int[] getNextPoint()
    {
        int[] lastPoint = points.get(points.size() - 1);
        int[] randomVertex = getRandomVertex(random, polygon);
        return new int[] { (int) ((lastPoint[0] + (randomVertex[0] - lastPoint[0]) * distanceFraction)),
                          (int) ((lastPoint[1] + (randomVertex[1] - lastPoint[1]) * distanceFraction)) };
    }
    
    public final void addNextPoint()
    {
        if (polygon != null)
        {
            if (points.isEmpty())
            {
                points.add(getRandomPointInPolygon(random, polygon));
            }
            else
            {
                points.add(getNextPoint());
            }
        }
    }
    
    @Override
    public final void paint(final Graphics g)
    {
        super.paint(g);
        if (polygon == null)
        {
            polygon = new Polygon();
            int[][] vertices = getPolygonVertices(numberOfSides,
                                                  getWidth() / 2,
                                                  new int[] { getWidth() / 2,
                                                             getHeight() / 2 });
            for (int vertex = 0; vertex < vertices.length; vertex++)
            {
                polygon.addPoint(vertices[vertex][0], vertices[vertex][1]);
            }
        }
        g.drawPolygon(polygon);
        for (int point = 0; point < points.size(); point++)
        {
            g.fillOval(points.get(point)[0], points.get(point)[1], 2, 2);
        }
    }
    
    @Override
    public final void update(final Graphics g)
    {
        final Graphics offgc;
        Image offscreen = null;
        final Rectangle box = g.getClipBounds();
        offscreen = createImage(box.width, box.height);
        offgc = offscreen.getGraphics();
        offgc.setColor(getBackground());
        offgc.fillRect(0, 0, box.width, box.height);
        offgc.setColor(getForeground());
        offgc.translate(-box.x, -box.y);
        paint(offgc);
        g.drawImage(offscreen, box.x, box.y, this);
    }
    
    public static final void main(String[] args)
    {
        final int screenWidth = 800;
        final int screenHeight = 800;
        
        int firstArg;
        double secondArg;
        args = new String[] { "3", "0.5" };
        if (args.length == 2)
        {
            try
            {
                firstArg = Integer.parseInt(args[0]);
                try
                {
                    secondArg = Double.parseDouble(args[1]);
                    
                    System.out.println("Chaos Game");
                    System.out.println("~~~~~~~~~~");
                    System.out.println("1. Select initial random point inside of polygon.");
                    System.out.println("2. Select one of the vertices at random.");
                    System.out.println("3. Select point given fraction of distance between point and vertex.");
                    System.out.println("4. Repeat from 2 a large number of times.");
                    
                    final JFrame frame = new JFrame("Chaos Game");
                    final ChaosGame chaosGame = new ChaosGame(firstArg,
                                                              secondArg);
                    frame.add(chaosGame);
                    frame.setSize(screenWidth, screenHeight);
                    frame.setVisible(true);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    
                    final Thread timeRefresh = new Thread(new Runnable()
                    {
                        private long time = System.currentTimeMillis();
                        
                        @Override
                        public void run()
                        {
                            while (true)
                            {
                                if (System.currentTimeMillis() - time > 10)
                                {
                                    time = System.currentTimeMillis();
                                    chaosGame.revalidate();
                                    chaosGame.repaint();
                                    for (int point = 0; point < 10; point++)
                                    {
                                        chaosGame.addNextPoint();
                                    }
                                }
                            }
                        }
                    });
                    timeRefresh.start();
                }
                catch (NumberFormatException e)
                {
                    System.err.println("Second argument must be a double (fraction) for the distance between the current point and the next randomly picked vertex.");
                    System.exit(1);
                }
            }
            catch (NumberFormatException e)
            {
                System.err.println("First argument must be an integer for the number of regular polygon sides.");
                System.exit(1);
            }
        }
        else
        {
            System.err.println("First argument must be an integer for the number of regular polygon sides.");
            System.err.println("Second argument must be a double (fraction) for the distance between the current point and the next randomly picked vertex.");
            System.exit(1);
        }
    }
}