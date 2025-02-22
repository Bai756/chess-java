package piece;

import main.GamePanel;
import main.Type;

public class Pawn extends Piece{

	public Pawn(int color, int col, int row) {
		super(color, col, row);
		
		type = Type.PAWN;
		
		if (color == GamePanel.WHITE) {
			image = getImage("/piece/pawn");
		}
		else {
			image = getImage("/piece/pawn1");
		}
	}
	public boolean canMove(int targetCol, int targetRow) {
		
		if (isWithinBoard(targetCol, targetRow) && isSameSquare(targetCol, targetRow) == false) {
			
			int moveValue;
			if (color == GamePanel.WHITE) {
				moveValue = -1;
			}
			else {
				moveValue = 1;
			}
			
			hittingP = getHittingP(targetCol, targetRow);
			
			//1 square
			if (targetCol == preCol && targetRow == preRow + moveValue && hittingP == null) {
				return true;
			}
			
			//2 squares
			if (targetCol == preCol && targetRow == preRow + moveValue*2 && hittingP == null && moved == false &&
					isPieceOnStraightLine(targetCol, targetRow) == false) {
				return true;
			}
			
			//capturing
			if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue && hittingP != null &&
					hittingP.color != color) {
				return true;
			}
			
			//en passant
			if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue) {
				for (Piece piece : GamePanel.simPieces) {
					if (piece.col == targetCol && piece.row == preRow && piece.twoMoved == true) {
						hittingP = piece;
						return true;
					}
				}
			}
			
		}	
		return false;
	}

}
