import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Virtual Memory Manager program.
 * @author Drew Osborne
 *
 *Help found online by Teraph G.
 *
 *-------------------------------------
 *Needs file location changed per user.
 *-------------------------------------
 */
public class Manager
{
	private final static int MemorySize = 256;
	private static int[] pageTable;
	private static int[] frameTable;
	private static HashMap<Integer, Integer> TLB;
	private static ArrayList<Integer> TLBQue;
	private static byte[] physMemory;
	private static int physMemoryMarker;
	private static int freeFrame;
	private static int bitMask;
	private static double faultCounter;
	private static double addresses;
	private static double faultRate;
	private static double TLBHits;
	private static String filename;
	static NumberFormat formatter;


	/**
	 * Main method. Initializes all variables.
	 * @param args
	 */
	public static void main(String[] args)
	{
		pageTable = new int[MemorySize];
		frameTable = new int[MemorySize];
		new ArrayList<Integer>();
		TLB = new HashMap<Integer, Integer>(17, 1);
		TLBQue = new ArrayList<Integer>();
		physMemory = new byte[MemorySize * MemorySize];
		physMemoryMarker = 0;
		freeFrame = 0;
		bitMask = 0x00FF;
		faultCounter = 0;
		addresses = 0;
		faultRate = 0;
		TLBHits = 0;
		filename = "InputFile.txt";
		formatter = new DecimalFormat("#0.0");
		new Manager().run();
	}


	/**
	 * Runs the program.
	 */
	private void run()
	{
		// File for disk.
		File faultPath;
		RandomAccessFile faultFile = null;

		// Create pageTable and fill it with -1's to represent empty.
		for (int i = 0; i < MemorySize - 1; i++)
		{
			pageTable[i] = -1;
		}

		// Create frameTable and initialize each index starting at 0 and
		// increasing by entry size at each iteration.
		int frameNum = 0;
		for (int i = 0; i < MemorySize - 1; i++)
		{
			frameNum = i * MemorySize;
			frameTable[i] = frameNum;
		}

		BufferedReader br = null;
		try
		{
			//Import file and load data.
			File file = new File(filename);
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String input;

			while ((input = br.readLine()) != null)
			{
				// Set virtual location.
				int pageNumber = (Integer.parseInt(input) >> 8);
				int offset = (Integer.parseInt(input) & bitMask);
				int val = 0;
				int frameNumber = 0;

				// Page Fault handling
				if ((!(TLB.containsKey(pageNumber)) && pageTable[pageNumber] == -1))
				{

					try
					{
						// *******************************************************
						// This path will need to be changed per user.
						// *******************************************************
						// Create a new file in eclipse and put the file path here
						// with double \'s.
						// *******************************************************
						faultPath = new File(
								"C:\\Users\\Drew Osborne\\Google Drive\\workspace\\Virtual Memory Manager\\FaultFile");
						faultFile = new RandomAccessFile(faultPath, "r");

						// seek to byte position.
						faultFile.seek((pageNumber) * (MemorySize));
						faultFile.read(physMemory, physMemoryMarker, MemorySize);
						val = physMemory[physMemoryMarker + offset];
						physMemoryMarker += MemorySize;

						// Update PageTable
						pageTable[pageNumber] = frameTable[freeFrame];
						frameNumber = pageTable[pageNumber];
						freeFrame++;

						// Update TLB
						updateTLB(pageNumber, frameNumber);
						faultCounter++;

						// Places in physical address.
						int physicalAddress = frameNumber | offset;
						System.out.println("VA: " + input + " PA: "
								+ physicalAddress + " Value: " + val);
						addresses++;
					}
					catch (IOException e)
					{
						System.out.println(e);
						System.err.println("Unable to start the disk");
					}
					finally
					{
						faultFile.close();
					}

				}
				else
				{
					if (TLB.containsKey(pageNumber))
					{
						//Output and increase TLB hits.
						frameNumber = checkTLB(pageNumber);
						int physicalAddress = frameNumber | offset;
						output(input, offset, frameNumber, physicalAddress);
						TLBHits++;
						addresses++;
					}
					else
					{
						//Output.
						frameNumber = pageTable[pageNumber];
						int physicalAddress = frameNumber | offset;
						output(input, offset, frameNumber, physicalAddress);
						addresses++;
					}
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			System.out.println(e + "File not Found.");
		}
		finally
		{
			try
			{
				if (br != null)
				{
					//Close file after finished.
					br.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		printStats();
	}


	/**
	 * updates the TLB
	 * 
	 * @param pageNumber
	 * @param frameNumber
	 */
	private void updateTLB(int pageNumber, int frameNumber)
	{
		if (TLB.size() <= 16)
		{
			addtoTLB(pageNumber, frameNumber);
		}
		else
		{
			TLB.remove(TLBQue.get(0));

		}
	}


	/**
	 * Prints the memory if no page fault.
	 * 
	 * @param data
	 * @param offset
	 * @param frameNumber
	 * @param physicalAddress
	 */
	private void output(String data, int offset, int frameNumber, int physicalAddress)
	{
		System.out.println("VA: " + data + " PA: "
				+ physicalAddress + " Value: " + physMemory[frameNumber + offset]);
	}


	/**
	 * Add value to Hash Map
	 * 
	 * @param pageNumber
	 * @param frameNumber
	 */
	public void addtoTLB(int pageNumber, int frameNumber)
	{
		TLB.put(pageNumber, frameNumber);
		TLBQue.add(pageNumber);
	}


	/**
	 * Check the TLB
	 * 
	 * @param pageNumber
	 * @return
	 */
	public int checkTLB(int pageNumber)
	{
		return (int) TLB.get(pageNumber);
	}


	/**
	 * Print final Statistics.
	 */
	public void printStats()
	{
		faultRate = faultCounter / addresses;
		System.out.println("-----------------------------\n\n");
		System.out.println("Number of Translated Addresses: " + addresses);
		System.out.println("Page Faults: " + faultCounter);
		System.out.println("Page Fault Percentage: " + formatter.format(faultRate * 100) + "%");
		System.out.println("TLB Hits: " + TLBHits);
		System.out.println("TLB Hit Percentage: " + formatter.format((TLBHits / addresses) * 100) + "%");
	}
}
