/*
 * JJonesLifeThreaded.cpp
 *
 *  Created on: Feb 19, 2016
 *      Author: Jordan Jones
 */

# include <cstdlib>
# include <iostream>
# include <iomanip>
# include "mpi.h"
# include "math.h"
# include <stdio.h>

using namespace std;

# define MAX_X 500
# define MAX_Y 500

# define STEPS_MAX 200
# define UNCHANGED_MAX 10

# define NVEGIES_INDEX 0
# define NSTEPS_INDEX 1


/**
 * Main method to run the game of life, using the MPI.
 */
int main(int argc, char *argv[])
{
   // Variables

   int mySimsToRun;
   int leftOverSims;
   int simulationNumber;
   const int MASTER = 0;
   const int NX_TAG = 1;
   const int NY_TAG = 2;
   const int PROB_TAG = 3;
   const int NSIMS_TAG = 4;
   const int SEED0_TAG = 5;

   int grid[MAX_X + 2][MAX_Y + 2]; /* grid of vegetation values */
   int nx; /* x dimension of grid */
   int ny; /* y dimension of grid */
   int maxSteps; /* max # timesteps to simulate */
   int maxUnchanged; /* max # timesteps with no vegetation change */
   int vegies; /* amount of stable vegetation */
   int nsteps; /* number of steps actually run */
   int nsims; /* number of simulations to perform */
   int ndied; /* # populations which die out */
   int nunsettled; /* # populations which don't stabilize */
   int nstable; /* # populations which do stabilize */
   float totStepsStable; /* total/average steps to stabilization */
   float totVegStable; /* total/average stable vegetation */
   double prob; /* population probability */
   int seed, seed0; /* random number seeds */
   int i, j; /* loop counters */
   void initializeGrid(int[][MAX_Y + 2], int, int, int, double);
   int gameOfLife(int[][MAX_Y + 2], int, int, int, int, int*);

   MPI::Status status;
   int myId;
   int numProcs;

   //*** Initialize MPI, get rank and size
   MPI::Init (argc, argv);
   numProcs = MPI::COMM_WORLD.Get_size();
   myId = MPI::COMM_WORLD.Get_rank();

   // Get input parameters in master and send values to all other processors.
   if (myId == MASTER)
   {
	   // Initialize variables that only the master node will need to use.
	   ndied = 0;
	   nunsettled = 0;
	   nstable = 0;
	   totStepsStable = 0;
	   totVegStable = 0;

       // Output initial greeting from master node.
       cout << "Processes available is " << numProcs << "\n";

	   nx = MAX_X + 1;

	   while (nx > MAX_X || ny > MAX_Y)
	   {
		  printf("Enter X and Y dimensions of wilderness: ");
		  scanf("%d%d", &nx, &ny);
	   }

	   printf("\nEnter population probability: ");
	   scanf("%lf", &prob);

	   printf("\nEnter number of simulations: ");
	   scanf("%d", &nsims);

	   printf("\nEnter random number seed: ");
	   scanf("%d", &seed0);

	   // Send input variables to all other processors.
       for (i = 1; i < numProcs; i++)
       {
           MPI::COMM_WORLD.Send(&nx, 1, MPI_INTEGER, i, NX_TAG);
           MPI::COMM_WORLD.Send(&ny, 1, MPI_INTEGER, i, NY_TAG);
           MPI::COMM_WORLD.Send(&prob, 1, MPI_DOUBLE, i, PROB_TAG);
           MPI::COMM_WORLD.Send(&nsims, 1, MPI_INTEGER, i, NSIMS_TAG);
           MPI::COMM_WORLD.Send(&seed0, 1, MPI_INTEGER, i, SEED0_TAG);
       }

   } // if
   else
   {
	   // Receive input variables from master node.
	   MPI::COMM_WORLD.Recv(&nx, 1, MPI::INTEGER, MASTER, NX_TAG, status);
	   MPI::COMM_WORLD.Recv(&ny, 1, MPI::INTEGER, MASTER, NY_TAG, status);
	   MPI::COMM_WORLD.Recv(&prob, 1, MPI::DOUBLE, MASTER, PROB_TAG, status);
	   MPI::COMM_WORLD.Recv(&nsims, 1, MPI::INTEGER, MASTER, NSIMS_TAG, status);
	   MPI::COMM_WORLD.Recv(&seed0, 1, MPI::INTEGER, MASTER, SEED0_TAG, status);
   }

   //*** Common Code to be executed to all nodes

   // Decide how many simulations each proc needs to run.
   mySimsToRun = nsims / numProcs;
   int simResultList[mySimsToRun * 2]; // 2d array represented in a normal array

   // For as many times as this proc needs to, run simulations and record the
   // results.
   for (i = 0; i < mySimsToRun; i++)
   {
      // Compute which simulation this is, so that the number can be used in
      // getting the seed. This replaces the "i" value in other versions.
	  simulationNumber = (myId * mySimsToRun) + i + 1;

      // Initialize the grid values using the given probability.
      seed = seed0 * simulationNumber;
      initializeGrid(grid, nx, ny, seed, prob);

      // Run a simulation and remember the vegetation and step results.
      maxSteps = STEPS_MAX;
      maxUnchanged = UNCHANGED_MAX;
      nsteps = gameOfLife(grid, nx, ny, maxSteps, maxUnchanged, &vegies);
      simResultList[(i * 2) + NVEGIES_INDEX] = vegies;
      simResultList[(i * 2) + NSTEPS_INDEX] = nsteps;

      printf("Number of time steps = %d, Vegetation total = %d\n", nsteps,
            vegies);
   } // for

   //*** Separation of manager/worker code
   if (myId != MASTER)
   {
      // Code for worker:
      MPI::COMM_WORLD.Send(simResultList, mySimsToRun * 2, MPI::INTEGER,
    		  MASTER, 1);
   }
   else
   {
      // Code for master:

      // Record the master's own results first, then results of all workers.
      for (i = 0; i < mySimsToRun; i++)
      {
    	  vegies = simResultList[(i * 2) + NVEGIES_INDEX];
    	  nsteps = simResultList[(i * 2) + NSTEPS_INDEX];

         if (vegies == 0)
         {
            ndied = ndied + 1;
         }
         else if (nsteps >= maxSteps)
         {
            nunsettled = nunsettled + 1;
         }
         else
         {
            nstable = nstable + 1;
            totStepsStable = totStepsStable + nsteps;
            totVegStable = totVegStable + vegies;
         }
      } // for

      // Get and record results of workers.
      for (i = 1; i < numProcs; i++)
      {
         MPI::COMM_WORLD.Recv(simResultList, mySimsToRun * 2, MPI::INTEGER,
        		 MPI::ANY_SOURCE, 1, status);

         for (j = 0; j < mySimsToRun; j++)
         {
        	vegies = simResultList[(j * 2) + NVEGIES_INDEX];
      	    nsteps = simResultList[(j * 2) + NSTEPS_INDEX];

            if (vegies == 0)
            {
               ndied = ndied + 1;
            }
            else if (nsteps >= maxSteps)
            {
               nunsettled = nunsettled + 1;
            }
            else
            {
               nstable = nstable + 1;
               totStepsStable = totStepsStable + nsteps;
               totVegStable = totVegStable + vegies;
            }
         } // for
      } // for

      // If there was at least one simulation that stabilized, update the total
      // steps and vegetation variables to reflect averages.
      if (nstable > 0)
      {
         totStepsStable = totStepsStable / nstable;
         totVegStable = totVegStable / nstable;
      }
   } // else

   //*** Shut down MPI.
   MPI::Finalize();

   //*** Display results
   if (myId == MASTER)
   {
      printf("Percentage which died out: %g%%\n", 100.0 * ndied / nsims);
      printf("Percentage unsettled:      %g%%\n", 100.0 * nunsettled / nsims);
      printf("Percentage stabilized:     %g%%\n", 100.0 * nstable / nsims);
      printf("  Of which:\n");
      printf("  Average steps:           %g\n", totStepsStable);
      printf("  Average vegetation:      %g\n", totVegStable);
   }

} // main


/**
  * Initializes an empty grid given grid dimensions, a seed, and vegetation
  * probability.
  *
  * @param grid
  *           is a grid of vegetation values
  * @param nx
  *           is the x dimension of the grid
  * @param ny
  *           is the y dimension of the grid
  * @param seed
  *           is a random number seed
  * @param prob
  *           is the population probability
  */
void initializeGrid(int grid[][MAX_Y + 2], int nx, int ny, int seed,
		double prob)
{
   int i, j; /* loop counters */
   int index; /* unique value for each grid cell */
   int newSeed; /* unique seed for each grid point */
   double rand1(int);

   for (i = 1; i <= nx; i++)
   {
      for (j = 1; j <= ny; j++)
      {
         index = ny * i + j;
         newSeed = seed + index;
         if (rand1(newSeed) > prob)
            grid[i][j] = 0;
         else
            grid[i][j] = 1;
      }
   }
} // initializeGrid


/**
  * Runs a simulation of the game of life given an initialized grid,
  * dimensions, and loop restrictions.
  *
  * @param grid
  *           is a grid of vegetation values
  * @param nx
  *           is the x dimension of the grid
  * @param ny
  *           is the y dimension of the grid
  * @param maxSteps
  *           is the max # of timesteps to simulate
  * @param maxUnchanged
  *           is the max # of timesteps with no vegetation change to simulate
  * @param pvegies
  *           is the vegatation amount for this simulation. Once this method is
  *           finished, the value will be updated.
  * @return the number of steps taken in the simulation
  */
int gameOfLife(int grid[][MAX_Y + 2], int nx, int ny, int maxSteps,
		int maxUnchanged, int *pvegies)
{
   int step; /* counts the time steps */
   int converged; /* has the vegetation stabilized? */
   int numUnchanged; /* # timesteps with no vegetation change */
   int oldVegies; /* previous level of vegetation */
   int old2Vegies; /* previous level of vegetation */
   int old3Vegies; /* previous level of vegetation */
   int vegies; /* total amount of vegetation */
   int neighbors; /* quantity of neighboring vegetation */
   int tempGrid[MAX_X][MAX_Y]; /* grid to hold updated values */
   int i, j; /* loop counters */

   step = 1;
   vegies = 1;
   oldVegies = -1;
   old2Vegies = -1;
   old3Vegies = -1;
   numUnchanged = 0;
   converged = 0;

   while (!converged && vegies > 0 && step < maxSteps)
   {

      /* Count the total amount of vegetation. */

      vegies = 0;
      for (i = 1; i <= nx; i++)
      {
         for (j = 1; j <= ny; j++)
         {
            vegies = vegies + grid[i][j];
         }
      }
      if (vegies == oldVegies || vegies == old2Vegies || vegies == old3Vegies)
      {
         numUnchanged = numUnchanged + 1;
         if (numUnchanged >= maxUnchanged)
            converged = 1;
      }
      else
      {
         numUnchanged = 0;
      }
      old3Vegies = old2Vegies;
      old2Vegies = oldVegies;
      oldVegies = vegies;

      // Use to show step results in detail:
      //printf(" step %d: vegies = %d\n", step, vegies);

      if (!converged)
      {
         /* Copy the sides of the grid to make torus simple. */
         for (i = 1; i <= nx; i++)
         {
            grid[i][0] = grid[i][ny];
            grid[i][ny + 1] = grid[i][1];
         }

         for (j = 0; j <= ny + 1; j++)
         {
            grid[0][j] = grid[nx][j];
            grid[nx + 1][j] = grid[1][j];
         }

         /* Now run one time step, putting result in tempGrid. */

         for (i = 1; i <= nx; i++)
         {
            for (j = 1; j <= ny; j++)
            {
               neighbors = grid[i - 1][j - 1] + grid[i - 1][j]
                     + grid[i - 1][j + 1] + grid[i][j - 1] + grid[i][j + 1]
                     + grid[i + 1][j - 1] + grid[i + 1][j] + grid[i + 1][j + 1];
               tempGrid[i][j] = grid[i][j];
               if (neighbors >= 25 || neighbors <= 3)
               {
                  tempGrid[i][j] = tempGrid[i][j] - 1;
                  if (tempGrid[i][j] < 0)
                     tempGrid[i][j] = 0;
               }
               else if (neighbors <= 15)
               {
                  tempGrid[i][j] = tempGrid[i][j] + 1;
                  if (tempGrid[i][j] > 10)
                     tempGrid[i][j] = 10;
               }
            } // for
         } // for

         /* Now copy tempGrid back to grid. */

         for (i = 1; i <= nx; i++)
         {
            for (j = 1; j <= ny; j++)
            {
               grid[i][j] = tempGrid[i][j];
            }
         }
         step = step + 1;
      } // if
   } // while

   *pvegies = vegies;
   return (step);
} // gameOfLife


/**
  * Generates a random double, based on the given seed, that is between 0 and 1.
  *
  * @param iseed
  *           is the given seed used in generating the result.
  * @return the double that was generated.
  */
double rand1(int iseed)
{
   double aa = 16807.0;
   double mm = 2147483647.0;
   double sseed;
   int jseed;
   int i;

   jseed = iseed;

   for (i = 1; i <= 5; i++)
   {
      sseed = jseed;
      jseed = aa * sseed / mm;
      sseed = aa * sseed - mm * jseed;
      jseed = sseed;
   }

   iseed = jseed;

   return (sseed / mm);
} // rand1

