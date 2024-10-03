package piece;

import main.GamePanel;
import main.Type;

public class Queen extends Piece{

	public Queen(int color, int col, int row) {
		super(color, col, row);
		
		type = Type.QUEEN;
		
		if (color == GamePanel.WHITE) {
			image = getImage("/piece/queen");
		}
		else {
			image = getImage("/piece/queen1");
		}
	}
	public boolean canMove(int targetCol, int targetRow) {
		
		if (isWithinBoard(targetCol, targetRow) && isSameSquare(targetCol, targetRow) == false) {
			
			//diagonal
			if (Math.abs(targetCol - preCol) == Math.abs(targetRow - preRow)) {
				
				if (isValidSquare(targetCol, targetRow) &&
						isPieceOnDiagonal(targetCol, targetRow) == false) {
					return true;
				}
			}
			
			//horizontal and vertical
			if (targetCol == preCol || targetRow == preRow) {
				
				if (isValidSquare(targetCol, targetRow) &&
					isPieceOnStraightLine(targetCol, targetRow) == false) {
					return true;
				}
			}
		}
		return false;
	}
}
