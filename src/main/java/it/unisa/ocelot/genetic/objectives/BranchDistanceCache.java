package it.unisa.ocelot.genetic.objectives;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;

import it.unisa.ocelot.genetic.edges.FunBranchNameAndFitness;

public class BranchDistanceCache {

	private static HashMap<String, Double> fitnessHashMap = new HashMap<String, Double>();
	public static void cacheFitnessValues() {
		fitnessHashMap.clear();
		//readAndPrintBinaryFile("fitnessValues.bin");
		/*try (DataInputStream dis = new DataInputStream(new FileInputStream("./fitnessValues.bin"))) {
            while (true) {
                try {
                    int len = dis.readInt();
                    byte[] strBytes = new byte[len];
                    dis.readFully(strBytes);
                    System.out.println("DEBUG: string length = " + len);
                    String record = new String(strBytes, "UTF-8");
                    System.out.println("DEBUG: read record = " + record);
                    double fitness = dis.readDouble();


                    System.out.println(record + " -> " + fitness);
                } catch (EOFException eof) {
                	System.out.println("DEBUG: EOF reached");
                    break; // normal exit at EOF
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
		/*try (DataInputStream dis = new DataInputStream(new FileInputStream("fitnessValues.bin"))){
			while (true) {
                try {
                    int len = dis.readInt();  // may throw EOFException
                    byte[] strBytes = new byte[len];
                    dis.readFully(strBytes);

                    String record = new String(strBytes);
                    double fitness = dis.readDouble();

                    System.out.println(record + " -> " + fitness);
                } catch (EOFException eof) {
                	System.out.println("reach EOF");
                    break; // clean exit when end of file is reached
                }
            }
		}
		catch (IOException e) {
			System.err.println("Error reading fitnessValues.bin file: " + e.getMessage());
		}
		 */


		try (BufferedReader f_Val_File = new BufferedReader(new FileReader("./fitnessValues.txt"))) {
			String lineBr = f_Val_File.readLine();
			while (lineBr != null) {
				FunBranchNameAndFitness infoFromLinebr = readInfoFromLine(lineBr);
				if(fitnessHashMap.containsKey(infoFromLinebr.getFunBranchName()) && fitnessHashMap.get(infoFromLinebr.getFunBranchName()) < infoFromLinebr.getCurrFitnessVal()) {
					// Do nothing
				}
				else {
					fitnessHashMap.put(infoFromLinebr.getFunBranchName(), infoFromLinebr.getCurrFitnessVal());
				}
				lineBr = f_Val_File.readLine();
			}
		} catch (IOException e) {
			System.err.println("Error reading fitnessValues.txt file: " + e.getMessage());
		}
	}

	private static void readAndPrintBinaryFile(String filename) {
		File file = new File(filename);

		// Check if file exists
		if (!file.exists()) {
			System.err.println("Error: File does not exist: " + filename);
			return;
		}

		try (FileInputStream fis = new FileInputStream(file);
				FileChannel channel = fis.getChannel()) {

			// Acquire shared lock for reading
			FileLock lock = null;
			int attempts = 0;

			while (lock == null && attempts < 10) {
				try {
					lock = channel.tryLock(0, Long.MAX_VALUE, true);
					if (lock == null) {
						Thread.sleep(50);
						attempts++;
					}
				} catch (Exception e) {
					Thread.sleep(50);
					attempts++;
				}
			}

			if (lock == null) {
				System.err.println("Error: Could not acquire file lock");
				return;
			}

			try (DataInputStream dis = new DataInputStream(new BufferedInputStream(fis))) {
				//System.out.println("=== Reading Binary File: " + filename + " ===");
				//System.out.println("File size: " + file.length() + " bytes\n");

				int recordCount = 0;

				// Read all records from the file
				while (fis.available() > 0) {
					try {
						// Read string length (4 bytes) - LITTLE ENDIAN
						byte[] lengthBytes = new byte[4];
						int bytesRead = dis.read(lengthBytes);
						if (bytesRead < 4) break;

						ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
						lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
						int length = lengthBuffer.getInt();

						// Validate length
						if (length <= 0 || length > 1000) {
							System.err.println("Invalid string length: " + length + 
									" at record " + (recordCount + 1));
							break;
						}

						// Read string bytes
						byte[] strBytes = new byte[length];
						bytesRead = dis.read(strBytes);
						if (bytesRead < length) {
							System.err.println("Incomplete string data");
							break;
						}
						String branchId = new String(strBytes);

						// Read double value (8 bytes) - LITTLE ENDIAN
						byte[] doubleBytes = new byte[8];
						bytesRead = dis.read(doubleBytes);
						if (bytesRead < 8) {
							System.err.println("Incomplete double data");
							break;
						}

						ByteBuffer doubleBuffer = ByteBuffer.wrap(doubleBytes);
						doubleBuffer.order(ByteOrder.LITTLE_ENDIAN);
						double fitnessValue = doubleBuffer.getDouble();

						// Print the record
						recordCount++;
						//System.out.printf("Record %d: %s = %.6f%n",recordCount, branchId, fitnessValue);
						String lineBr=branchId+";"+fitnessValue;
						//System.out.println("\n"+lineBr);
						FunBranchNameAndFitness infoFromLinebr = readInfoFromLine(lineBr);
						if(fitnessHashMap.containsKey(infoFromLinebr.getFunBranchName()) && fitnessHashMap.get(infoFromLinebr.getFunBranchName()) < infoFromLinebr.getCurrFitnessVal()) {
							// Do nothing
						}
						else {
							fitnessHashMap.put(infoFromLinebr.getFunBranchName(), infoFromLinebr.getCurrFitnessVal());
						}
					} catch (EOFException e) {
						System.out.println("\nReached end of file.");
						break;
					}
				}

				//System.out.println("\nTotal records read: " + recordCount);

			} finally {
				// Release the lock before the channel closes
				if (lock != null && lock.isValid()) {
					try {
						lock.release();
					} catch (IOException e) {
						// Ignore - channel may already be closed
					}
				}
			}

		} catch (FileNotFoundException e) {
			System.err.println("Error: File not found - " + filename);
		} catch (IOException e) {
			System.err.println("Error reading file: " + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.err.println("Error: Thread interrupted while waiting for lock");
			Thread.currentThread().interrupt();
		}
	}

	private static FunBranchNameAndFitness readInfoFromLine(String lineBr) {
		FunBranchNameAndFitness infoFromLinebr = new FunBranchNameAndFitness();
		String listOfItems[] = lineBr.split(";");
		String fName = listOfItems[0];
		String branchName = listOfItems[1];
		String fitnessVal = listOfItems[2];
		String fun_BranchName = fName + ":" + branchName;
		fitnessVal = fitnessVal.replace(",", ".");
		double currFitness = Double.parseDouble(fitnessVal);
		if (currFitness > 1)
			System.err.println("Wrong fitness value! Branch:" + fun_BranchName + " Fitness:" + currFitness);
		infoFromLinebr.setFunBranchName(fun_BranchName);
		infoFromLinebr.setCurrFitnessVal(currFitness);
		return infoFromLinebr;
	}

	public static HashMap<String, Double> getBranchDistances() {
		return fitnessHashMap;
	}


}
