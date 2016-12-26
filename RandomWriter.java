import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Class that is used to output a random set of characters of a given length
 * based on a given text input and a level of analysis, where higher levels will
 * result in characters that more closely resemble the source text.
 */
public class RandomWriter
{

   /**
    * Uses a given reader to read input one character at a time, and
    * conceptually maps each character onto the possible seed that precedes it.
    * The keys of the map are unique sub-strings of length 'seedLength', each
    * with a value that is a list of characters found to follow occurrences of
    * the sub-string.
    * 
    * @param inputReader
    *           is the Reader that will be used to read input.
    * @param possibleSeeds
    *           is the map of strings to lists of characters.
    * @param seedLength
    *           is the length of each sub-string we will examine.
    * @throws IOException
    *            if an I/O error occurs during reading.
    */
   private static void buildMapFromInput(Reader inputReader,
         Map<String, ArrayList<Character>> possibleSeeds, int seedLength)
         throws IOException
   {
      int result;
      final int DONE = -1;
      StringBuilder currentSeed = new StringBuilder();
      ArrayList<Character> associatedCharacters;

      // Read the first X characters and make that our initial seed.
      for (int i = 0; i < seedLength; i++)
      {
         currentSeed.append((char) inputReader.read());
      }

      // Continue reading until the end of the file or input stream. Each new
      // character will be added to the list of associated characters for the
      // current seed, and the seed will be conceptually moved forward one
      // character, using the character read to update it.
      do
      {
         result = inputReader.read();

         // If we have not just reached the end of the input, put a new
         // association in the map if the current seed is new. Add the new 
         // character read to the seed's corresponding list of characters, 
         // update the map association, and update the seed.
         if (result != DONE)
         {
            // If the seed is not found in the map, give it a listing with a 
            // new list of characters.
            if (!possibleSeeds.containsKey(currentSeed.toString()))
            {
               possibleSeeds.put(currentSeed.toString(),
                     new ArrayList<Character>());
            }

            // Get the seed's list of characters from the map, add the new
            // character to it, and put the association back in the map.
            associatedCharacters = possibleSeeds.get(currentSeed.toString());
            associatedCharacters.add((char) result);
            possibleSeeds.put(currentSeed.toString(), associatedCharacters);

            // Move the seed over one in the text by deleting the first 
            // character and appending the new character.
            currentSeed.deleteCharAt(0);
            currentSeed.append((char) result);
         } // if
      }
      while (result != DONE);

   } // buildMapFromInput


   /**
    * Uses the given map of possible seeds to obtain a list of keys from the
    * map, and returns a StringBuilder that holds a random key.
    * 
    * @param generator
    *           is used to generate a random integer in a determined range.
    * @param possibleSeeds
    *           is the map of possible seeds.
    * @return the seed that has been generated, as a StringBuilder.
    */
   private static StringBuilder generateRandomSeed(Random generator,
         Map<String, ArrayList<Character>> possibleSeeds)
   {
      // Create a list of keys that is of the length of the map's keySet. Then,
      // use the keySet to fill in the list of keys. The StringBuilder we return
      // will be constructed from a randomly selected element in the key list.
      Set<String> keySet = possibleSeeds.keySet();
      String[] keyList = new String[keySet.size()];
      keyList = keySet.toArray(keyList);
      return new StringBuilder(keyList[generator.nextInt(keyList.length)]);
   } // generateRandomSeed


   /**
    * Generates and prints 'length' characters based on the entries in
    * 'possibleSeeds'. Output is based on the frequency that different
    * characters were found to occur after instances of possible seeds in the
    * text.
    * 
    * @param length
    *           is the number of characters to output.
    * @param possibleSeeds
    *           is the map of possible seeds the input text contained.
    * @param generator
    *           is used to generate a random integer in a determined range.
    * @param output
    *           is the output stream to write to.
    */
   private static void generateResultingText(int length,
         Map<String, ArrayList<Character>> possibleSeeds, Random generator,
         PrintStream output)
   {
      char resultingChar;
      ArrayList<Character> occurrences;

      // Generate a random seed to start with.
      StringBuilder seed = generateRandomSeed(generator, possibleSeeds);

      for (int i = 0; i < length; i++)
      {
         // If the current seed is not found in our map of possible seeds,
         // generate a new random one before attempting to generate the
         // resulting character.
         while (!possibleSeeds.containsKey(seed.toString()))
         {
            seed = generateRandomSeed(generator, possibleSeeds);
         }

         // Look up the seed in the map of possible seeds and get the matching
         // list of character occurrences. Pick a random character from the list
         // to be the resulting character, use it to update the seed, and print
         // it.
         occurrences = possibleSeeds.get(seed.toString());
         resultingChar = occurrences.get(generator.nextInt(occurrences.size()));
         seed.deleteCharAt(0);
         seed.append(resultingChar);
         output.print(resultingChar);

      } // for

   } // generateResultingText


   /**
    * Uses the given arguments to generate a random output of characters. If no
    * files are given to be read, the program will wait for the user to enter
    * input and terminate the input stream, and it will use that as the text to
    * generate characters from.
    * 
    * @param args
    *           should contain k, the level of analysis, in index 0, the length
    *           of characters to be generated in index 1, and optionally, a list
    *           of files to read text from in starting at index 2, with each
    *           successive index holding another file.
    */
   public static void main(String[] args)
   {
      final int MINIMUM_ARGS = 2;
      int seedLength = 0;
      int length = 0;
      File currentFile;
      FileReader currentFileReader;
      InputStreamReader in;
      PrintStream output = System.out;
      Random generator = new Random();
      Map<String, ArrayList<Character>> possibleSeeds = new HashMap<>();

      // Make sure required number of arguments exists.
      if (args.length < MINIMUM_ARGS)
      {
         System.err.println("Insufficient arguments given");
         System.exit(1);
      }

      // Make sure first two arguments are valid integers.
      try
      {
         seedLength = Integer.parseInt(args[0]);
         length = Integer.parseInt(args[1]);
      }
      catch (NumberFormatException e)
      {
         System.err.println("A non-integer argument was given that should have"
               + " been an integer");
         System.exit(1);
      }

      // Both integers must be greater than zero
      if (seedLength < 1 || length < 1)
      {
         System.err.println("Both integer arguments must be greater than zero");
         System.exit(1);
      }

      // Use given parameters to put entries into the map of possible seeds
      // (string "keys" to character list "values").

      // If more than the minimum args were given, take each argument after the
      // first two to be files, and for each file, verify that it exists and is
      // readable, and then add entries to the map based on the text's possible
      // seeds.
      if (args.length > MINIMUM_ARGS)
      {
         // Iterate over every file as explained above.
         for (int i = MINIMUM_ARGS; i < args.length; i++)
         {
            currentFile = new File(args[i]);

            // Make sure the file can be found.
            try
            {
               currentFileReader = new FileReader(currentFile);
            }
            catch (FileNotFoundException e)
            {
               throw new IllegalArgumentException("A file was given that could"
                     + " not be found");
            }

            // Attempt to read through the file and add entries to the map as we
            // go along. If an I/O error occurs at this point, the file exists
            // but was not readable.
            try
            {
               buildMapFromInput(currentFileReader, possibleSeeds, seedLength);
            }
            catch (IOException e)
            {
               throw new IllegalArgumentException("Something went wrong when "
                     + "attempting to read the file " + currentFile.toPath());
            }

         } // for

      } // if

      // Otherwise no files were given, so use the characters given in System.in
      // to create our map.
      else
      {
         in = new InputStreamReader(System.in);

         try
         {
            buildMapFromInput(in, possibleSeeds, seedLength);
         }
         catch (IOException e)
         {
            throw new IllegalArgumentException("Something went wrong when "
                  + "attempting to read the user input");
         }

      } // else

      // If the map of possible seeds is empty, we know there was not sufficient
      // input because there was not enough text to make any seeds out of.
      if (possibleSeeds.isEmpty())
      {
         System.err.println("Insufficient text input given");
         System.exit(1);
      }

      // Generate and print out a resulting string of characters.
      generateResultingText(length, possibleSeeds, generator, output);

   } // main

} // RandomWriter