import java.util.Scanner;

/**
 * Class to solve a small Sudoku board (9x9) using a backtracking algorithm.
 */
public class SudokuSolver
{

   /**
    * Attempts to recursively solve the Sudoku puzzle by trying all possible
    * tokens for each board space until one can be placed. When a point is
    * reached where no token can be placed at the current board space, the
    * algorithm recursively backtracks to the previous move made and tries
    * different unused tokens until either one works or it is forced to
    * backtrack another step.
    * 
    * @param solved
    *           is true if the puzzle has been solved.
    * @param row
    *           is the current board row.
    * @param col
    *           is the current board column.
    * @param SQRT_GRID_SIZE
    *           is the square root of the board length, which is used as the
    *           length of each sub-grid.
    * @param newToken
    *           is the token selected to be tested and possibly placed next.
    * @param tokenList
    *           is the list of tokens used in the puzzle, all of which should
    *           end up being found in each sub-grid, row, and column in a
    *           correct solution.
    * @param board
    *           is the Sudoku board in its current state.
    * @return true if the puzzle was solved.
    */
   private static boolean sudokuRecursive(boolean solved, int row, int col,
         int SQRT_GRID_SIZE, char newToken, char[] tokenList, char[][] board)
   {
      boolean permanentToken;

      // Attempt to place the currently selected token on the board, and only
      // proceed if the token is successfully placed.
      if (makeNextMove(newToken, row, col, SQRT_GRID_SIZE, board))
      {
         // Increment the current board coordinates by moving one column over,
         // and if it wraps around, the row is incremented.
         col = (col + 1) % board.length;

         if (col == 0)
         {
            row++;
         }

         // If there is no next board space, the puzzle is complete.
         if (row == board.length)
         {
            solved = true;
         }
         // Otherwise, attempt to place a valid token in the new location.
         else
         {
            permanentToken = board[row][col] != EMPTY_SPACE;

            // Iterate over the token list and attempt to place each one in
            // succession until one is valid and actually placed, at which point
            // we go down another recursive level. Leave the loop early if the
            // puzzle is completed at any time.
            for (int i = 0; i < tokenList.length && !solved; i++)
            {
               newToken = tokenList[i];
               solved = sudokuRecursive(solved, row, col, SQRT_GRID_SIZE,
                     newToken, tokenList, board);

               // At this point backtracking takes place, and the last token
               // placed is removed from the board, but only if that token was
               // placed by the algorithm, as opposed to the program input.
               if (!permanentToken && !solved)
               {
                  board[row][col] = EMPTY_SPACE;
               }
            } // for
         } // else
      } // if

      return solved;
   } // sudokuRecursive


   /**
    * Places the given token on the board at the row and column if there are no
    * tokens identical to it in either the prospective token's sub-grid, row, or
    * column, and if there is not already a permanent token there.
    * 
    * @param newToken
    *           is the currently selected token looking to be placed on the
    *           board at the row and column.
    * @param row
    *           is the current board row.
    * @param col
    *           is the current board column.
    * @param SQRT_GRID_SIZE
    *           is the square root of the board length, which is used as the
    *           length of each sub-grid.
    * @param board
    *           is the Sudoku board in its current state.
    * @return true if the token was placed on the board.
    */
   private static boolean makeNextMove(char newToken, int row, int col,
         int SQRT_GRID_SIZE, char[][] board)
   {
      boolean moveValid = true;

      // These mark the location of the top left square in the sub-grid that
      // the row/column combination map onto.
      final int SUB_GRID_ROW = (row / SQRT_GRID_SIZE) * SQRT_GRID_SIZE;
      final int SUB_GRID_COL = (col / SQRT_GRID_SIZE) * SQRT_GRID_SIZE;

      // Skip trying to place the token and return true if this is being called
      // for the first time, or if there is already a token in this board spot.
      if (col >= 0 && board[row][col] == EMPTY_SPACE)
      {
         // Check to see if the token is unique in the sub-grid.
         for (int i = SUB_GRID_ROW; i < SUB_GRID_ROW + SQRT_GRID_SIZE
               && moveValid; i++)
         {
            for (int j = SUB_GRID_COL; j < SUB_GRID_COL + SQRT_GRID_SIZE
                  && moveValid; j++)
            {
               if (board[i][j] == newToken)
               {
                  moveValid = false;
               }
            } // for
         } // for

         // Check to see if the token is unique in the row and column.
         for (int i = 0; i < board.length && moveValid; i++)
         {
            if (board[row][i] == newToken || board[i][col] == newToken)
            {
               moveValid = false;
            }
         }

         // If the token was not found in the current sub-grid, row, or column,
         // place it on the board.
         if (moveValid)
         {
            board[row][col] = newToken;
         }
      } // if

      return moveValid;
   } // lastMoveValid


   /**
    * Given the board and sub-grid size, assemble and return a string
    * representation of the board.
    * 
    * @param board
    *           is the matrix of characters representing the board.
    * @param SQRT_GRID_SIZE
    *           is the length of one sub-grid on the board.
    * @return a string representation of the Sudoku board.
    */
   private static String toString(char[][] board, int SQRT_GRID_SIZE)
   {
      StringBuilder grid = new StringBuilder();

      for (int i = 0; i < board.length; i++)
      {
         for (int j = 0; j < board.length; j++)
         {
            grid.append(board[i][j]);

            // Add a new line if the last token in the row was just added.
            if (j == board.length - 1)
            {
               grid.append("\n");
            }
            // Otherwise if the last token in a sub-grid-length section of the
            // row was just added, excluding the borders, add a vertical line.
            else if (j % SQRT_GRID_SIZE == SQRT_GRID_SIZE - 1)
            {
               grid.append("|");
            }
            // Otherwise, add a space.
            else
            {
               grid.append(" ");
            }
         } // for

         // Add a series of dashes to the grid if in the appropriate spot, but
         // not on the borders of the grid.
         if (i % SQRT_GRID_SIZE == SQRT_GRID_SIZE - 1
               && i < board.length - SQRT_GRID_SIZE)
         {
            for (int j = 0; j < board.length; j++)
            {
               grid.append("- ");
            }

            grid.append("\n");
         } // if

      } // for

      return grid.toString();
   } // toString


   /**
    * Main method to build and attempt to solve the Sudoku board.
    * 
    * @param args
    *           will not be used.
    */
   public static void main(String[] args)
   {
      boolean solved;
      char token;
      char[] tokenList;
      char[][] board;
      final int SQRT_GRID_SIZE;
      int row;
      int column;
      String userInput;
      Scanner in = new Scanner(System.in);

      // Read in user input to construct the board.
      in.useDelimiter("[\\r\\n,]+");
      userInput = in.nextLine();
      tokenList = new char[userInput.length()];

      // Add the requested elements to the token list.
      for (int i = 0; i < userInput.length(); i++)
      {
         tokenList[i] = userInput.charAt(i);
      }

      board = new char[userInput.length()][userInput.length()];
      SQRT_GRID_SIZE = (int) Math.sqrt(board.length);

      // Initially fill the board with characters that represent empty spaces.
      for (int i = 0; i < board.length; i++)
      {
         for (int j = 0; j < board.length; j++)
         {
            board[i][j] = EMPTY_SPACE;
         }
      }

      // Read in input to add permanent tokens to the board.
      while (in.hasNext())
      {
         row = in.nextInt() - 1;
         column = in.nextInt() - 1;
         token = in.next().charAt(0);
         board[row][column] = token;
      } // while

      in.close();

      // Row and column will 0-based indices. They must start at -1 to
      // accommodate the first recursive call.
      row = -1;
      column = -1;

      // Attempt to solve the constructed Sudoku board.
      solved = sudokuRecursive(false, row, column, SQRT_GRID_SIZE, EMPTY_SPACE,
            tokenList, board);

      if (solved)
      {
         System.out.println(toString(board, SQRT_GRID_SIZE));
      }
      else
      {
         System.err.println("The given Sudoku board could not be solved.");
      }

   } // main

   final static char EMPTY_SPACE = '\0';

} // SudokuSolver
