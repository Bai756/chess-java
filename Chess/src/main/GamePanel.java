package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.JPanel;

import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;

public class GamePanel extends JPanel implements Runnable{
	
	public static final int WIDTH = 1100;
	public static final int HEIGHT = 800;
	final int FPS = 60; 
	Thread gameThread; 
	Board board = new Board();
	Mouse mouse = new Mouse();
	
	public static ArrayList<Piece> pieces = new ArrayList<>();
	public static ArrayList<Piece> simPieces = new ArrayList<>();
	ArrayList<Piece> promotionP = new ArrayList<>();
	Piece activeP, checkingP;
	public static Piece castlingP;
	
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	int currentColor = WHITE;
	
	boolean canMove;
	boolean validSquare;
	boolean promotion;
	boolean gameover;
	boolean stalemate;
	
	public GamePanel () {
		
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(Color.black);
		addMouseMotionListener(mouse);
		addMouseListener(mouse);
		
		setPieces();
		copyPieces(pieces, simPieces);
	}
	
	public void launchGame() {
		gameThread = new Thread(this);
		gameThread.start();
	}
	
	public void setPieces() {
		
		
		  pieces.add(new Pawn(WHITE,0,6)); pieces.add(new Pawn(WHITE,1,6));
		  pieces.add(new Pawn(WHITE,2,6)); pieces.add(new Pawn(WHITE,3,6));
		  pieces.add(new Pawn(WHITE,4,6)); pieces.add(new Pawn(WHITE,5,6));
		  pieces.add(new Pawn(WHITE,6,6)); pieces.add(new Pawn(WHITE,7,6));
		  pieces.add(new Rook(WHITE,0,7)); pieces.add(new Knight(WHITE,1,7));
		  pieces.add(new Bishop(WHITE,2,7)); pieces.add(new Queen(WHITE,3,7));
		  pieces.add(new King(WHITE,4,7)); pieces.add(new Knight(WHITE,6,7));
		  pieces.add(new Bishop(WHITE,5,7)); pieces.add(new Rook(WHITE,7,7));
		  
		  pieces.add(new Pawn(BLACK,0,1)); pieces.add(new Pawn(BLACK,1,1));
		  pieces.add(new Pawn(BLACK,2,1)); pieces.add(new Pawn(BLACK,3,1));
		  pieces.add(new Pawn(BLACK,4,1)); pieces.add(new Pawn(BLACK,5,1));
		  pieces.add(new Pawn(BLACK,6,1)); pieces.add(new Pawn(BLACK,7,1));
		  pieces.add(new Rook(BLACK,0,0)); pieces.add(new Knight(BLACK,1,0));
		  pieces.add(new Bishop(BLACK,2,0)); pieces.add(new Queen(BLACK,3,0));
		  pieces.add(new King(BLACK,4,0)); pieces.add(new Knight(BLACK,6,0));
		  pieces.add(new Bishop(BLACK,5,0)); pieces.add(new Rook(BLACK,7,0));
		 

	}
	
	private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
		
		target.clear();
		
		for (int i = 0; i < source.size(); i++) {
			target.add(source.get(i));
		}
	}
	 
	@Override
	public void run() {
		
		double drawInterval = 1000000000/FPS;
		double delta = 0;
		long lastTime = System.nanoTime();
		long currentTime;
		
		while (gameThread != null) {
			
			currentTime = System.nanoTime();
			
			delta += (currentTime - lastTime)/drawInterval;
			lastTime = currentTime;
			
			if (delta >= 1) { 
				update(); 
				repaint(); 
				delta--;
			}
		}
	}
	
	private void update() {
		
		if (promotion) {
			promoting();
		}
		else if (gameover == false && stalemate == false) {
			
			if (mouse.pressed) {
				if (activeP == null) {
					
					for (Piece piece : simPieces) {
						
						if (piece.color == currentColor &&
								piece.col == mouse.x/Board.SQUARE_SIZE &&
								piece.row == mouse.y/Board.SQUARE_SIZE) {
							
							activeP = piece;
						}
					}
				}
				else {
					simulate();
				}
			}
			
			if (mouse.pressed == false) {
				if (activeP != null) {
					
					if (validSquare) {
						
						//move confirmed
						
						copyPieces(simPieces, pieces);
						activeP.updatePosition();
						if (castlingP != null) {
							castlingP.updatePosition();
						}
						
						
						if (kingInCheck() && isCheckmate()) {
							gameover = true;
						}
						else if (isStalemate() && kingInCheck() == false) {
							stalemate = true;
						}
						else {
							if (canPromote()) {
								promotion = true;
							}
							else {
								changePlayer();
							}
						}
						
					}
					else {
						copyPieces(pieces, simPieces);
						activeP.resetPosition();
						activeP = null;
					}
				}
			}
		}
	}

	private void simulate() {
		
		canMove = false;
		validSquare = false;
		
		copyPieces(pieces, simPieces);
		
		//reset castling
		if (castlingP != null) {
			castlingP.col = castlingP.preCol;
			castlingP.x = castlingP.getX(castlingP.col);
			castlingP = null;
		}
		
		activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
		activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
		activeP.col = activeP.getCol(activeP.x);
		activeP.row = activeP.getRow(activeP.y);
		
		if (activeP.canMove(activeP.col, activeP.row)) {
			
			canMove = true;
			
			if (activeP.hittingP != null) {
				simPieces.remove(activeP.hittingP.getIndex());
			}
			
			checkCastling();
			
			if (isIllegal(activeP) == false && canOpponentCaptureKing() == false) {
				validSquare = true;
			}

		}
	}
	private boolean isIllegal(Piece king) {
		
		if (king.type == Type.KING) {
			for (Piece piece : simPieces) {
				if (piece != king && piece.color != king.color && piece.canMove(king.col,king.row)) {
					return true;
				}
			}
		}
		
		return false;
	}
	private boolean canOpponentCaptureKing() {
		
		Piece king = getKing(false);
		
		for (Piece piece : simPieces) {
			if (piece.color != king.color && piece.canMove(king.col, king.row)) {
				return true;
			}
		}
		
		return false;
	}
	private boolean kingInCheck() {
		
		Piece king = getKing(true);
		
		for (Piece piece : simPieces) {
			
			if (piece.color == currentColor && piece.canMove(king.col,king.row)) {
				checkingP = activeP;
				return true;
			}
			else {
				checkingP = null;
			}
		}
		
		return false;
	}
	// if opponent == true then return enemy king, else return own king
	private Piece getKing(boolean opponent) {
		
		Piece king = null;
		
		for (Piece piece : simPieces) {
			if (opponent) {
				if (piece.type == Type.KING && piece.color != currentColor) {
					king = piece;
				}
			}
			else {
				if (piece.type == Type.KING && piece.color == currentColor) {
					king = piece;
				}
			}
		}
		return king;
	}
	
	private boolean isCheckmate() {
		
		Piece king = getKing(true);
		
		if (kingCanMove(king)) {
			return false;
		}
		else {
			
			int colDiff = Math.abs(king.col - checkingP.col);
			int rowDiff = Math.abs(king.row - checkingP.row);
			
			if (colDiff == 0) {
				// vertically
				if (checkingP.row < king.row) {
					// above the king
					for (int row = checkingP.row; row < king.row; row++) {
						for (Piece piece : simPieces) {
							if (piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
								return false;
							}
						}
					}
				}
				if (checkingP.row > king.row) {
					// below the king
					for (int row = checkingP.row; row > king.row; row--) {
						for (Piece piece : simPieces) {
							if (piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
								return false;
							}
						}
					}
				}
			}
			else if (rowDiff == 0) {
				// horizontally
				if (checkingP.col < king.col) {
					// left of the king
					for (int col = checkingP.col; col < king.col; col++) {
						for (Piece piece : simPieces) {
							if (piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
								return false;
							}
						}
					}
				}
				if (checkingP.col > king.col) {
					// right of the king
					for (int col = checkingP.col; col > king.col; col--) {
						for (Piece piece : simPieces) {
							if (piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
								return false;
							}
						}
					}
				}
				
			}
			else if (colDiff == rowDiff) {
				//diagonally
				if (checkingP.row < king.row) {
					//above
					if (checkingP.col < king.col) {
						// upper left
						for (int col = checkingP.col, row = checkingP.row; col < king.col; col++, row++) {
							for (Piece piece : simPieces) {
								if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
									return false;
								}
							}
						}
					}
					if (checkingP.col > king.col) {
						// upper right
						for (int col = checkingP.col, row = checkingP.row; col > king.col; col--, row++) {
							for (Piece piece : simPieces) {
								if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
									return false;
								}
							}
						}
					}
				}
				if (checkingP.row > king.row) {
					//below
					if (checkingP.col < king.col) {
						// lower left
						for (int col = checkingP.col, row = checkingP.row; col < king.col; col++, row--) {
							for (Piece piece : simPieces) {
								if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
									return false;
								}
							}
						}
					}
					if (checkingP.col > king.col) {
						// lower right
						for (int col = checkingP.col, row = checkingP.row; col > king.col; col--, row--) {
							for (Piece piece : simPieces) {
								if (piece != king && piece.color != currentColor && piece.canMove(col, row)) {
									return false;
								}
							}
						}
					}
				}
				
			}
			
			
		}
		
		return true;
	}
	private boolean kingCanMove(Piece king) {
		
		if (isValidMove(king, -1, -1)) {return true;}
		if (isValidMove(king, -1, 0)) {return true;}
		if (isValidMove(king, -1, 1)) {return true;}
		if (isValidMove(king, 0, -1)) {return true;}
		if (isValidMove(king, 0, 1)) {return true;}
		if (isValidMove(king, 1, -1)) {return true;}
		if (isValidMove(king, 1, 0)) {return true;}
		if (isValidMove(king, 1, 1)) {return true;}
		
		
		return false;
	}
	private boolean isValidMove(Piece king, int colPlus, int rowPlus) {
		
		boolean isValidMove = false;
		
		//update king position
		king.col += colPlus;
		king.row += rowPlus;
		
		if (king.canMove(king.col, king.row)) {
			
			if (king.hittingP != null) {
				simPieces.remove(king.hittingP.getIndex());
			}
			if (isIllegal(king) == false) {
				isValidMove = true;
			}
		}
		
		king.resetPosition();
		copyPieces(pieces, simPieces);
		
		return isValidMove;
		
	}
	private boolean isStalemate() {
		
		Piece king = null;
		Piece preCheckingP = null;
		int oldCol, oldRow;
		
		for (Piece piece : simPieces) {
			if (piece.type == Type.KING && piece.color != currentColor) {
				king = piece;
			}
		}
		
		if (kingCanMove(king)) {
			return false;
		}

		for (Piece piece : simPieces) {
			if (piece.color != currentColor && piece.type != Type.KING) {
				for (int col = 0; col < 8; col++) {
					for (int row = 0; row < 8; row++) {
						
						if (piece.canMove(col, row)) {
							preCheckingP = checkingP;
							oldCol = piece.col;
							oldRow = piece.row;
							
							piece.col = col;
							piece.row = row;
							
								if (kingInCheck() == false) {
									
									checkingP = preCheckingP;
									piece.col = oldCol;
									piece.row = oldRow;
									return false;
								
							}
							checkingP = preCheckingP;
							piece.col = oldCol;
							piece.row = oldRow;
						}
					}
				}
			}
		}
		
		return true;
	}
	private void checkCastling() {
		
		if (castlingP != null) {
			if (castlingP.col == 0) {
				castlingP.col += 3;
			}
			else if (castlingP.col == 7){
				castlingP.col -= 2;
			}
			castlingP.x = castlingP.getX(castlingP.col);
		}
	}
	private void changePlayer() {
		if (currentColor == WHITE) {
			currentColor = BLACK;
			
			for (Piece piece : simPieces) {
				if (piece.color == BLACK) {
					piece.twoMoved = false;
				}
			}
		}
		else {
			currentColor = WHITE;
			for (Piece piece : simPieces) {
				if (piece.color == WHITE) {
					piece.twoMoved = false;
				}
			}
		}
		activeP = null;
	}
	private boolean canPromote() {
		
		if (activeP.type == Type.PAWN) {
			
			if (currentColor == WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7) {
				promotionP.clear();
				promotionP.add(new Queen(currentColor, 9, 2));
				promotionP.add(new Rook(currentColor, 9, 3));
				promotionP.add(new Bishop(currentColor, 9, 4));
				promotionP.add(new Knight(currentColor, 9, 5));
				return true;
			}
		}
		
		return false;
	}
	private void promoting() {
		
		if (mouse.pressed) {
			
			for (Piece piece : promotionP) {
				if (piece.col == mouse.x/Board.SQUARE_SIZE && piece.row == mouse.y/Board.SQUARE_SIZE) {
					
					switch(piece.type) {
					case QUEEN: simPieces.add(new Queen(currentColor, activeP.col, activeP.row)); break;
					case ROOK: simPieces.add(new Rook(currentColor, activeP.col, activeP.row)); break;
					case BISHOP: simPieces.add(new Bishop(currentColor, activeP.col, activeP.row)); break;
					case KNIGHT: simPieces.add(new Knight(currentColor, activeP.col, activeP.row)); break;
					default: break;
					}
					
					simPieces.remove(activeP.getIndex());
					copyPieces(simPieces, pieces);
					activeP = null;
					promotion = false;
					changePlayer();
					
				}
			}
		}
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g); 
		
		Graphics2D g2 = (Graphics2D) g;
		
		board.draw(g2);
		
		for(Piece p : simPieces) {
			p.draw(g2);
		}
		
		//active piece
		if (activeP != null) {
			
			if (canMove) {
				if (isIllegal(activeP) || canOpponentCaptureKing()) {
					g2.setColor(Color.gray);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
					g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE, 
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}
				else {
					g2.setColor(Color.white);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
					g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE, 
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}
			}
			
			activeP.draw(g2);
		}
		//status messages
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(new Font("Book Antiqua", Font.PLAIN, 35));
		g2.setColor(Color.white);
		
		if (promotion) {
			g2.drawString("Promote to:", 840, 150);
			for (Piece piece : promotionP) {
				g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), 
						Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
			}
		}
		else {
			
			if (currentColor == WHITE) {
				g2.drawString("White's turn", 850, 550);
				if (checkingP != null && checkingP.color == BLACK) {
					g2.setColor(Color.red);
					g2.drawString("The King", 875, 650);
					g2.drawString("is in check", 860, 700);
				}
			}
			else {
				g2.drawString("Black's turn", 850, 250);
				if (checkingP != null && checkingP.color == WHITE) {
					g2.setColor(Color.red);
					g2.drawString("The King", 875, 100);
					g2.drawString("is in check", 860, 150);
				}
			}
		}
		
		if (gameover) {
			String s = "";
			if (currentColor == WHITE) {
				s = "White Wins";
			}
			else {
				s = "Black Wins";
			}
			g2.setFont(new Font("Ariel", Font.PLAIN, 90));
			g2.setColor(Color.green);
			g2.drawString(s, 200, 420);
		}
		
		if (stalemate) {
			g2.setFont(new Font("Ariel", Font.PLAIN, 90));
			g2.setColor(Color.gray);
			g2.drawString("Stalemate", 200, 420);
		}
	}
}
