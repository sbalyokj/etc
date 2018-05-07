package math.geometry;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

@SuppressWarnings("serial")
public class FenceVisualTest extends JFrame {
	private BufferedImage bufferedImage;
	private Fence fence;
	
	public FenceVisualTest() {
		setBounds(0, 0, 1000, 1000);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		
		bufferedImage = new BufferedImage(getBounds().width, getBounds().height, BufferedImage.TYPE_INT_ARGB);
	
		Graphics graphics = bufferedImage.getGraphics();
		graphics.setColor(Color.black);
		graphics.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
		repaint();
	}
	
	private void buildFence() {
		List<Point2D> points = new ArrayList<>();
		for (int i = 0; i < 472; i++) {
			double r = Math.min(i, 400);
			double x = r * Math.cos(Math.toRadians(i * 5)) + 500;
			double y = r * Math.sin(Math.toRadians(i * 5)) + 500;
			points.add(new Point2D(x, y));
		}
		Graphics graphics = bufferedImage.getGraphics();
		Point2D before = null;
		for (Point2D point2d : points) {
			if (before != null) {
				graphics.drawLine((int)Math.round(before.getX()), (int)Math.round(before.getY()), (int)Math.round(point2d.getX()), (int)Math.round(point2d.getY()));
			}
			before = point2d;
		}
		repaint();
		fence = new Fence(points, false, 0.0001);
	}
	
	@Override
	public void paint(Graphics g) {
		getGraphics().drawImage(bufferedImage, 0, 0, null);
	}

	private void doMoveChain() {
		Graphics graphics = bufferedImage.getGraphics();
		graphics.setColor(Color.cyan);
		Move2D before = new Move2D();
		before.getStart().set(510, 510);
		before.getDirection().set(1, 1);
		Move2D after = new Move2D();
		while (fence.collide(before, after)) {			
			graphics.drawLine((int)before.getStart().getX(), (int)before.getStart().getY(), (int)after.getStart().getX(), (int)after.getStart().getY());
			repaint();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// swap move objects
			Move2D temp = before;
			before = after;
			after = temp;
		}
		System.out.println("out!!");
	}

	private void start() {
		buildFence();
		doMoveChain();
	}

	public static void main(String[] args) {
		FenceVisualTest fenceVisualTest = new FenceVisualTest();
		fenceVisualTest.start();
	}
}
