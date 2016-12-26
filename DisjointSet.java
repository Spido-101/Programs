import java.util.Random;

/**
 * Class to use an optimized disjoint set of integers to create an N x N maze of
 * arbitrary size, where every cell is reachable from every other cell.
 */
public class DisjointSet
{

   /**
    * Constructs the disjoint set given the number of initially distinct
    * elements.
    * 
    * @param setSize
    *           is the number of elements in the disjoint set.
    */
   public DisjointSet(int setSize)
   {
      _disjointSet = new int[setSize];

      // Set each element in the list to -1, so each represents the root of a
      // tree of height 1. In this way, all negative values in the list
      // represent a root at that index with a tree height of the absolute value
      // of the element. All positive values represent the parent index of the
      // item at that index.
      for (int i = 0; i < _disjointSet.length; i++)
      {
         _disjointSet[i] = -1;
      }

   } // DisjointSet


   /**
    * Performs a union-by-height on the disjoint sets containing elements item1
    * and item2, which may or may not be in the same disjoint set.
    * Note that with path compression being implemented in the find operation,
    * this is really union-by-rank.
    * 
    * @param item1
    *           is the first element to union.
    * @param item2
    *           is the second element to union.
    */
   public void union(int item1, int item2)
   {
      item1 = find(item1);
      item2 = find(item2);
      
      // If item2 is the root of the deeper tree, make it the root of the new
      // combined tree.
      if (_disjointSet[item2] < _disjointSet[item1])
      {
         _disjointSet[item1] = item2;
      }

      // Otherwise, item1 will be the new root, and if the trees of item1 and
      // item2 are the same height, conceptually increase the height of item1's
      // tree by one by decrementing its negative value.
      else
      {
         if (_disjointSet[item1] == _disjointSet[item2])
         {
            _disjointSet[item1]--;
         }

         _disjointSet[item2] = item1;
      }

   } // union


   /**
    * Uses a "slow" find implementation in combination with path compression.
    * Note that this turns the union-by-height operation into union-by-rank.
    * 
    * @param index
    *           is the index representing the desired element in the disjoint
    *           set.
    * @return the index of the root to which the item at the given index
    *         belongs.
    */
   public int find(int index)
   {
      int root = index;
      int nextIndex;

      // While the value at the index of root is not negative, set root to
      // the parent value it points to, so that root ends up being the index of
      // the actual root.
      while (_disjointSet[root] > -1)
      {
         root = _disjointSet[root];
      }

      // Use path compression to reduce the time of future find operations. If
      // the index given in the parameters was not a root index, make the parent
      // value of every index on the path to the root the root index.
      if (index != root)
      {
         do
         {
            nextIndex = _disjointSet[index];
            _disjointSet[index] = root;
            index = nextIndex;
         }
         while (nextIndex != root);
      }

      return root;
   } // find


   /**
    * @param item1
    *           is the item being tested for connectivity with item2.
    * @param item2
    *           is the item being tested for connectivity with item1.
    * @returns true if item1 and item2 are in the same disjoint set.
    */
   public boolean isConnected(int item1, int item2)
   {
      return this.find(item1) == this.find(item2);
   } // isConnected


   /**
    * Main method to create an N x N maze where every cell can be reached from
    * every other cell.
    * 
    * @param args
    *           contains N, which determines the dimensions of the maze.
    */
   public static void main(String[] args)
   {
      int mazeLength = 0;
      int numRoots;
      int cell1;
      int cell2;
      int root1;
      int root2;
      int direction;
      int distanceToWall;
      int index;
      final int UP = 0;
      final int RIGHT = 1;
      final int DOWN = 2;
      final int DIRECTIONS = 4;
      final int TOP_ROW_SIZE;
      final int NORMAL_ROW_SIZE;
      StringBuilder mazeDrawing = new StringBuilder();
      Random generator = new Random();
      DisjointSet theSet;

      // Read in program input.
      try
      {
         mazeLength = Integer.parseInt(args[0]);
      }
      catch (IndexOutOfBoundsException | NumberFormatException e)
      {
         System.err.println("Program argument error.");
         System.exit(1);
      }

      // Create the initial maze.
      theSet = new DisjointSet(mazeLength * mazeLength);
      numRoots = mazeLength * mazeLength;
      TOP_ROW_SIZE = (mazeLength * 2) + 1;
      NORMAL_ROW_SIZE = (mazeLength * 2) + 2;

      // Draw the top row of horizontal walls.
      for (int i = 0; i < mazeLength; i++)
      {
         mazeDrawing.append(" _");
      }

      mazeDrawing.append("\n");

      // Draw all other rows of horizontal and vertical walls.
      for (int i = 0; i < mazeLength; i++)
      {
         for (int j = 0; j < mazeLength; j++)
         {
            mazeDrawing.append("|_");
         }

         mazeDrawing.append("|\n");
      } // for

      // Remove the top left and bottom right walls to create an entrance and
      // an exit.
      mazeDrawing.replace(1, 2, " ");
      mazeDrawing.replace(mazeDrawing.length() - 3, mazeDrawing.length() - 2,
            " ");

      // Remove randomly selected walls between cells that cannot reach each
      // other until every cell can be reached from every other cell.
      while (numRoots > 1)
      {
         cell1 = generator.nextInt(theSet._disjointSet.length);
         direction = generator.nextInt(DIRECTIONS);

         // Attempt to choose the cell adjacent to cell1 in the direction
         // chosen, and use the knowledge of the direction to store what will be
         // the distance from cell1's left wall to the wall to remove in the
         // maze drawing.
         if (direction == UP)
         {
            cell2 = cell1 - mazeLength;
            distanceToWall = (mazeLength * -2) - 1;
         }
         else if (direction == RIGHT)
         {
            cell2 = cell1 + 1;
            distanceToWall = 2;
         }
         else if (direction == DOWN)
         {
            cell2 = cell1 + mazeLength;
            distanceToWall = 1;
         }
         else
         {
            cell2 = cell1 - 1;
            distanceToWall = 0;
         }

         // Only proceed if cell2 is really adjacent to cell1 in the maze.
         // cell2 is invalid if it is outside the maze boundaries, or if both it
         // and cell1 are on opposite ends of rows of the maze.
         if ((cell2 > 0 && cell2 < theSet._disjointSet.length)
               && !((cell2 % mazeLength == 0
                     && cell1 % mazeLength == mazeLength - 1)
                     || (cell1 % mazeLength == 0
                           && cell2 % mazeLength == mazeLength - 1)))
         {
            root1 = theSet.find(cell1);
            root2 = theSet.find(cell2);

            // If the cells do not belong to the same root, perform a union on
            // the roots to internally join those sets of cells, and then remove
            // the appropriate wall in the string representation of the maze.
            if (root1 != root2)
            {
               theSet.union(root1, root2);
               numRoots--;

               // Locate the index of the wall to remove. First, skip past the
               // top row of underscores to get to the first row of cells.
               index = TOP_ROW_SIZE;

               // Increment the index to get to the left wall of cell1's row.
               index += NORMAL_ROW_SIZE * (cell1 / mazeLength);

               // Move up to cell1's left wall in the current row.
               index += (cell1 % mazeLength) * 2;

               // Add the distance, determined by the direction from cell1, to
               // get to the target wall.
               index += distanceToWall;
               mazeDrawing.replace(index, index + 1, " ");
            } // if
         } // if

      } // while

      System.out.println(mazeDrawing.toString());

   } // main

   private int[] _disjointSet;

} // DisjointSet
