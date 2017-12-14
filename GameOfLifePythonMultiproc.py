'''
Created on Mar 10, 2016

@author: Jordan Jones
'''

# Imports
import sys
import multiprocessing
import math

# Constants
MAX_X = 500
MAX_Y = 500
STEPS_MAX = 200
UNCHANGED_MAX = 10
NSTEPS_INDEX = 0
VEGIES_INDEX = 1


###########################
# Method called by each worker process to run game of life simulations.
#
# INPUTS:
#     nx: The x dimension of the grid.
#     ny: The y dimension of the grid.
#     maxSteps: The max number of time steps to simulate.
#     maxUnchanged: The max number of time steps with no vegetation change to
#                   simulate.
#     prob: The probability of vegetation being placed in any given grid space.
#     seed0: The original seed given in the input.
#     mySims: The number of simulations this process will run.
#     myProcess: Which process number this is, starting at 1.
#     queue: The queue used to hold the results of all simulations run by this
#            process, where each is a pair of integers stored in a tuple.
# OUTPUTS: None.
###########################
def worker(nx, ny, maxSteps, maxUnchanged, prob, seed0, mySims, myProcess, 
           queue):
    steps = 0
    vegies = 0
    seed = 0
    grid = [[0 for i in xrange(MAX_X + 2)] for j in xrange(MAX_Y + 2)]
    
    for i in xrange(mySims):
        seed = seed0 * ((myProcess * mySims) - i)
        initializeGrid(grid, nx, ny, seed, prob)
        steps, vegies = gameOfLife(grid, nx, ny, maxSteps, maxUnchanged)
        queue.put((steps, vegies)) 
    return


###########################
# Initializes an empty grid given grid dimensions, a seed, and vegetation 
# probability.
#
# INPUTS:
#     grid: A grid of vegetation values.
#     nx: The x dimension of the grid.
#     ny: The y dimension of the grid.
#     seed: A random number seed.
#     prob: The probability of vegetation being placed in any given grid space.
# OUTPUTS: None.
###########################
def initializeGrid(grid, nx, ny, seed, prob):
    index = 0 # unique value for each grid cell
    newSeed = 0 # unique seed for each grid point

    for i in xrange(1, nx + 1):
        for j in xrange(1, ny + 1):
            index = ny * i + j
            newSeed = seed + index
            
            if (rand1(newSeed) > prob):
                grid[i][j] = 0
               
            else:
                grid[i][j] = 1
    return


###########################
# Runs a simulation of the game of life given an initialized grid, dimensions,
# and loop restrictions.
#
# INPUTS:
#     grid: A grid of vegetation values.
#     nx: The x dimension of the grid.
#     ny: The y dimension of the grid.
#     maxSteps: The max number of time steps to simulate.
#     maxUnchanged: The max number of time steps with no vegetation change to
#                   simulate.
# OUTPUTS:
#     The number of steps the simulation took and the final vegetation amount.
###########################
def gameOfLife(grid, nx, ny, maxSteps, maxUnchanged):
    steps = 1 # counts the time steps
    converged = False # true if the vegetation has stabilized
    nUnchanged = 0 # # of time steps with no vegetation change
    oldVegies = -1 # previous level of vegetation
    old2Vegies = -1 # previous level of vegetation
    old3Vegies = -1 # previous level of vegetation
    vegies = 1 # total amount of vegetation
    neighbors = 0 # quantity of neighboring vegetation
    tempGrid = [[0 for i in xrange(MAX_X)] for j in xrange(MAX_Y)]

    # Run simulation time steps as long as the vegetation has not stabilized,
    # there is still vegetation remaining, and we have not reached the
    # maximum number of steps.
    while (not converged and vegies > 0 and steps < maxSteps):
        
        # Count the total amount of vegetation.
        vegies = 0

        for i in xrange(1, nx + 1):
            for j in xrange(1, ny + 1):
                vegies = vegies + grid[i][j]

        # If the amount of vegetation is the same as it was in any of the last
        # three time steps, increment the number of unchanged steps, and check
        # to see if the population has stabilized.
        if (vegies == oldVegies or vegies == old2Vegies 
            or vegies == old3Vegies):
            nUnchanged += 1

            if (nUnchanged >= maxUnchanged):
                converged = True

        # Otherwise, the number of steps in a row where the vegetation has not
        # changed is reset to 0.
        else:
            nUnchanged = 0

        # Update the vegetation values for the last three time steps.
        old3Vegies = old2Vegies
        old2Vegies = oldVegies
        oldVegies = vegies
         
        # use to view step results in detail:
        # print(" step {}: vegies = {}".format(steps, vegies))

        # If the population has not stabilized, perform the next time step and
        # put the results in a temporary grid which will be copied over into
        # the original grid once it is complete.
        if (not converged):
            
            # Copy the sides of the grid to simulate edge wrapping.

            for i in xrange(1, nx + 1):
                grid[i][0] = grid[i][ny]
                grid[i][ny + 1] = grid[i][1]

            for j in xrange(0, ny + 2):
                grid[0][j] = grid[nx][j]
                grid[nx + 1][j] = grid[1][j]

            # Run one time step, putting the result in tempGrid.

            for i in xrange(1, nx + 1):
                for j in xrange(1, ny + 1):
                    neighbors = grid[i - 1][j - 1] + grid[i - 1][j] \
                        + grid[i - 1][j + 1] + grid[i][j - 1] + grid[i][j + 1] \
                        + grid[i + 1][j - 1] + grid[i + 1][j] \
                        + grid[i + 1][j + 1]
                    tempGrid[i][j] = grid[i][j]

                    # Decide how the current grid space is affected, based on 
                    # the quantity of neighboring vegetation.
                    if (neighbors >= 25 or neighbors <= 3):
                        # Too crowded or too sparse, lose vegetation.
                        tempGrid[i][j] -= 1
    
                        # Don't allow negative vegetation.
                        if (tempGrid[i][j] < 0):
                            tempGrid[i][j] = 0
                            
                    elif (neighbors <= 15):
                        # Just the right amount of neighbors for growth.
                        tempGrid[i][j] += 1
    
                        # Don't allow vegetation over 10 in one grid space.
                        if (tempGrid[i][j] > 10):
                            tempGrid[i][j] = 10

            # Copy tempGrid back to grid.
            for i in xrange(1, nx + 1):
                for j in xrange(1, ny + 1):
                    grid[i][j] = tempGrid[i][j]

            steps += 1

    return steps, vegies


###########################
# Generates a float, using on the given seed, that is between 0 and 1.
#
# INPUTS: 
#     iseed: The integer used to generate the resulting double.
# OUTPUTS:
#     A float between 0 and 1.
###########################
def rand1(iseed):
    aa = 16807.0
    mm = 2147483647.0
    sseed = 0

    for i in xrange(1, 6):
        sseed = iseed
        iseed = int(aa * sseed / mm)
        sseed = (aa * sseed) - (mm * iseed)
        iseed = int(sseed)

    return sseed / mm


###########################
# Main method to run the game of life, using the multiprocessing module.
###########################
def main():
    
    # grid of vegetation values
    nx = MAX_X + 1 # x dimension of grid
    ny = MAX_Y + 1 # y dimension of grid
    maxSteps = STEPS_MAX # max # of time steps to simulate
    maxUnchanged = UNCHANGED_MAX # max # of time steps with no vegetation change
    stepsResult = 0 # number of steps actually run
    vegiesResult = 0 # amount of stable vegetation
    nsims = 0 # number of simulations to perform
    ndied = 0 # # of populations which die out
    nunsettled = 0 # # of populations which don't stabilize
    nstable = 0 # # of populations which do stabilize
    totalStepsStable = 0.0 # total/average steps to stabilization
    totalVegiesStable = 0.0 # total/average stable vegetation
    probability = 0 # population probability
    seed0 = 0 # random number seed given by input
    numProcesses = 0 # total number of processes to use
    simsPerProcess = 0 # number of simulations each process will run
    queueList = [] # a list of queues
    processList = [] # a list of processes
    queueResults = () # a tuple which will hold the results of a process

    # read in all parameters
    
    try:
        numProcesses = int(math.pow(2, float(sys.argv[1])))
    except TypeError:
        sys.stderr.write("Program argument missing.")
        sys.exit()

    while (nx > MAX_X or ny > MAX_Y):
        print("Enter X and Y dimensions of wilderness: ")
        nx = int(input())
        ny = int(input())

    print("\nEnter population probability: ")
    probability = float(input())

    print("\nEnter number of simulations: ")
    nsims = int(input())

    print("\nEnter random number seed: ")
    seed0 = int(input())
    
    simsPerProcess = nsims // numProcesses
    
    # Create processes and run each one's simulations. Each one will have a 
    # corresponding queue, which will be used to retrieve the results of the 
    # processors simulations.
    
    for i in xrange(numProcesses):
        queue = multiprocessing.Queue()
        process = multiprocessing.Process(target=worker, args=(nx, ny, maxSteps,
                                          maxUnchanged, probability, seed0, 
                                          simsPerProcess, i + 1, queue,))
        queueList.append(queue)
        processList.append(process)
        process.start()
        
    # Get and record the results of each process's simulations.
    
    for i in xrange(numProcesses):
        processList[i].join()
        queue = queueList[i]
        
        while not queue.empty():
            queueResults = queue.get()
            stepsResult = queueResults[NSTEPS_INDEX]
            vegiesResult = queueResults[VEGIES_INDEX]
    
            print("Number of steps = {}, Vegetation total = {}"
                  .format(stepsResult, vegiesResult))

            # Record the results of this simulation.

            if (vegiesResult == 0):
                ndied += 1
            
            elif (stepsResult >= maxSteps):
                nunsettled += 1
            
            else:
                nstable += 1
                totalStepsStable += stepsResult
                totalVegiesStable += vegiesResult
    
    # If at least one population has stabilized, update the stable step and
    # vegetation averages.
    if (nstable > 0):
        totalStepsStable = totalStepsStable / nstable
        totalVegiesStable = totalVegiesStable / nstable

    # Print results.

    print("\nPercentage which died out: {}%".format(100.0 * ndied / nsims))
    print("Percentage unsettled:      {}%".format(100.0 * nunsettled / nsims))
    print("Percentage stabilized:     {}%".format(100.0 * nstable / nsims))
    print("  Of which:")
    print("  Average steps:           {}".format(totalStepsStable))
    print("  Average vegetation:      {}".format(totalVegiesStable))
    
    
if __name__ == '__main__':
    main()
    