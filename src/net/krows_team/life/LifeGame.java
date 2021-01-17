package net.krows_team.life;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import net.krows_team.typeapi.Bind;
import net.krows_team.typeapi.InputManager;
import net.krows_team.typeapi.KeyboardManager;
import net.krows_team.typeapi.MouseManager;

public class LifeGame {
	
	private JFrame window;
	
	private Canvas canvas;
	
	private boolean allowNextStep;
	private boolean showGrid;
	private boolean endless;
	
	private boolean[][] cells;
	
	private InputManager input;
	
	private Point actualPoint;
	
	private Point selectedCell;
	
	private int set;
	private int w;
	private int h;
	
	public LifeGame() {
		
		canvas = new Canvas();
		
		window = new JFrame("Life");
		window.setSize(1280, 720);
		window.setLocationRelativeTo(null);
		window.setResizable(false);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.add(canvas);
		window.setVisible(true);
		
		start();
	}
	
	public void start() {
		
		init();
		
		Timer timer = new Timer(1000 / 60, e -> {
				
			update();
			render();
		});
		timer.setRepeats(true);
		timer.start();
		
		postInit();
	}
	
	public void init() {
		
		selectedCell = new Point();
		
		actualPoint = new Point();
		
		showGrid = true;
		
		endless = true;
		
		input = new InputManager();
		input.addInputManager(new KeyboardManager());
		input.addInputManager(new MouseManager());
		input.getKeyboardManager().addBind(new Bind(KeyEvent.VK_R, true, bind -> allowNextStep = !allowNextStep));
		input.getKeyboardManager().addBind(new Bind(KeyEvent.VK_C, true, bind -> showGrid = !showGrid));
		input.getKeyboardManager().addBind(new Bind(KeyEvent.VK_S, true, bind -> saveMap()));
		input.getKeyboardManager().addBind(new Bind(KeyEvent.VK_L, true, bind -> loadMap()));
		input.getKeyboardManager().addBind(new Bind(KeyEvent.VK_ENTER, true, bind -> updateSet()));
		input.getMouseManager().addBind(new Bind(MouseEvent.BUTTON1, true, bind -> setPixel()));
		
		canvas.requestFocus();
		canvas.addKeyListener(input.getKeyboardManager());
		canvas.addMouseListener(input.getMouseManager());
		canvas.addMouseMotionListener(input.getMouseManager());
		canvas.addMouseWheelListener(e -> {
				
			if(set != 0) {
				
				if(set == 1) w += e.getWheelRotation();
				else h += e.getWheelRotation();
			}
		});
		
		cells = new boolean[10][10];
		
		w = h = 10;
		
		for(int i = 0; i < cells.length; i++) {
			
			for(int j = 0; j < cells[i].length; j++) {
				
				cells[i][j] = false;
			}
		}
	}
	
	public void postInit() {
		
		flexSize();
	}
	
	int cellSize = 30;
	
	public void updateSet() {
		
		set = (set + 1) % 3;
		
		if(set == 0) {
			
			cells = new boolean[w][h];
			
			flexSize();
		}
	}
	
	public void flexSize() {
		
		cellSize = Math.min(canvas.getWidth(), canvas.getHeight()) / cells.length;
	}
	
	public void setPixel() {
		
		if(!allowNextStep) cells[selectedCell.x][selectedCell.y] = !cells[selectedCell.x][selectedCell.y];
	}
	
	int skips = 0;
	
	public void update() {

		input.update();
		
		actualPoint = input.getMouseManager().getMousePoint();
		actualPoint.translate(- canvas.getWidth() / 2, - canvas.getHeight() / 2);
		actualPoint.translate(cells.length * cellSize / 2, cells[0].length * cellSize / 2);
		
		if(actualPoint.x >= 0 && actualPoint.x < cells.length * cellSize && actualPoint.y >= 0 && actualPoint.y < cells[0].length * cellSize) {
			
			selectedCell = new Point(actualPoint.x / cellSize, actualPoint.y / cellSize);
		}
		
		if(allowNextStep && skips >= 6) {
			
			boolean[][] nc = new boolean[cells.length][cells[0].length];
			
			for(int i = 0; i < nc.length; i++) {
				
				for(int j = 0; j < nc[i].length; j++) {
					
					nc[i][j] = isAlive(i, j);
				}
			}
			
			cells = nc;
			
			skips = 0;
		}
		
		skips++;
	}
	
	public boolean isAlive(int x, int y) {
		
		return cells[x][y] ? equals(countNeighbors(x, y), 2, 3) : equals(countNeighbors(x, y), 3);
	}
	
	public int countNeighbors(int x, int y) {
		
		if(endless) return countEndless(x, y);
		
		int result = 0;
		
		int[] nbs = {1, 2, 3, 4, 5, 6, 7, 8};
		
		if(x == 0) remove(nbs, 1, 7, 8);
		if(y == 0) remove(nbs, 1, 2, 3);
		if(x == cells.length - 1) remove(nbs, 3, 4, 5);
		if(y == cells[0].length - 1) remove(nbs, 5, 6, 7);
		
		for(int nb : nbs) {
			
			if(nb == 0) continue;
			if(cells[x + (equals(nb, 1, 7, 8) ? - 1 : equals(nb, 2, 6) ? 0 : 1)][y + (equals(nb, 1, 2, 3) ? - 1 : equals(nb, 8, 4) ? 0 : 1)]) result++;
		}
		
		return result;
	}
	
	public int countEndless(int x, int y) {
		
		int result = 0;
		
		for(int i = - 1; i <= 1; i++) {
			
			for(int j = - 1; j <= 1; j++) {
				
				if(i == 0 && j == 0) continue;
				
				int x1 = x + i < 0 ? cells.length - 1 : x + i > cells.length - 1 ? 0 : x + i;
				int y1 = y + j < 0 ? cells[0].length - 1 : y + j > cells[0].length - 1 ? 0 : y + j;
				
				if(cells[x1][y1]) result++;
			}
		}
		
		return result;
	}
	
	public int[] remove(int[] a, int... nums) {
		
		for(int i = 0; i < a.length; i++) {
			
			if(equals(a[i], nums)) a[i] = 0;
		}
		
		return a;
	}
	
	public boolean equals(int num, int... values) {
		
		for(int i : values) if(num == i) return true;
		
		return false;
	}
	
	public void render() {
		
		if(canvas.getBufferStrategy() == null) canvas.createBufferStrategy(2);
		
		BufferStrategy bs = canvas.getBufferStrategy();
		
		Graphics2D g = (Graphics2D) bs.getDrawGraphics();
		
		render(g);
		
		g.dispose();
		
		bs.show();
	}
	
	public void render(Graphics2D g) {
		
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		g.translate(canvas.getWidth() / 2, canvas.getHeight() / 2);
		g.translate(- cells.length * cellSize / 2, - cells[0].length * cellSize / 2);
		
		for(int i = 0; i < cells.length; i++) {
			
			for(int j = 0; j < cells[i].length; j++) {
				
				if(cells[i][j]) {
					
					g.setColor(Color.BLACK);
					g.fillRect(i * cellSize, j * cellSize, cellSize, cellSize);
				}
				if(showGrid) {
					
					g.setColor(Color.WHITE);
					g.drawRect(i * cellSize, j * cellSize, cellSize, cellSize);
				}
			}
		}
		
		if(!allowNextStep) {
			
			g.setColor(Color.YELLOW);
			g.fillRect(selectedCell.x * cellSize, selectedCell.y * cellSize, cellSize, cellSize);
		}
		if(set != 0) {
			
			g.setColor(Color.BLACK);
			
			if(set == 1) g.drawString("Width: " + w, 10, 10);
			else if(set == 2) g.drawString("Height: " + h, 10, 10);
		}
	}
	
	public void saveMap() {
		
		try(DataOutputStream out = new DataOutputStream(new FileOutputStream("res\\maps\\save.map"))) {
			
			out.writeInt(cells.length);
			out.writeInt(cells[0].length);
			
			for(boolean[] bb : cells) {
				
				for(boolean b : bb) {
					
					out.write(b ? 1 : 0);
				}
			}
		} catch(Exception e) {
			
			e.printStackTrace();
		}
	}
	
	public void loadMap() {
		
		try(DataInputStream in = new DataInputStream(new FileInputStream("res\\maps\\save.map"))) {
			
			boolean[][] bb = new boolean[in.readInt()][in.readInt()];
			
			for(int i = 0; i < bb.length; i++) {
				
				for(int j = 0; j < bb[i].length; j++) {
					
					bb[i][j] = in.read() == 1;
				}
			}
			
			cells = bb;
			
			flexSize();
		} catch(Exception e) {
			
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * Main method.
	 * 
	 * @param args Arguments for method.
	 * 
	 */
	public static void main(String[] args) {
		
		new LifeGame();
	}
}