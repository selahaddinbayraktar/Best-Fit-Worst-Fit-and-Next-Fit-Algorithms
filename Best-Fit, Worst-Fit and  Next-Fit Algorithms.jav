package pack61;

	//This class provides a simple node structure to be used in our linked list structure. 
class Node {   				
    int start, size;  // start variable states where in the memory our allocation will start.
    Node next;		  // size variable is for stating how much memory our one singular node (free block in the memory to store the data) will consume.
    				  // This next pointer is vital and is the foundation of our linked list structure. This pointer is a must in every node since it
    				  // will point the next node in memory to create the linked list structure we desire.	
    
    // A basic node constructor structure includes the attributes that are necessary for this project. Whenever a user wants to create a node in the list,
    //  will use this constructor to reduce the code complexity.
    public Node(int start, int size) {
        this.start = start;
        this.size = size;
        this.next = null;
    }
}
package pack61;


   //This class at whole is for making things a lot easier. Further down this class will be used in main class to store all the allocation we made
   //in a list. This will make easier to track and freeing memory if necessary.
public class AllocationRecord {
    int start, size;

    public AllocationRecord(int start, int size) {
        this.start = start;
        this.size = size;
    }
}
package pack61;

import java.util.ArrayList;
import java.util.List;


class FitAllocator {
    static final int DISK_SIZE = 200; // Declaration of size has been made as final int to prevent future changes can be made by any user.

    Node freeList;               // Head of the linked list.
    Node lastAllocated;          // This pointer is exclusively for next fit algorithm. By it's working principle, it will need a reference point on where to start it's scanning.
    List<AllocationRecord> history; // This list will be helpful  when deleting an allocation since it will store every allocation by their size and starting point.

    public FitAllocator() {
        freeList = new Node(0, DISK_SIZE); // At the beginning we have nothing stored in our list so our list is one giant big block. 
        lastAllocated = freeList;         //  There are no allocations yet, so we can assign our tracker to the head of list.
        history = new ArrayList<>();      // Creating the list for storing future allocations.
    }

    // Best-fit algorithm
    public int allocateBest(int size) {  // Take the block's size as parameter.
        Node current = freeList;   // Scanning starts from the head of our list.
        Node best = null;          // We declare our best block. We haven't found it yet so we say it is null for now.
        int minWaste = Integer.MAX_VALUE; // This variable will store our wasted space after we find our block and allocate it. 
                                          // Giving it a really big number at start so our waste will be smaller at the end.  
        								  

        while (current != null) {     // Start searching for free blocks.
            if (current.size >= size) {  // We only need the blocks either has the same size or bigger to fit.
                int waste = current.size - size;  // Calculate the wasted memory by subtracting the allocated block's size from the free block's size.
                if (waste < minWaste) {  // Check if the place we found has less waste than the best one so far.
                    minWaste = waste;
                    best = current;  // If it has less, then this block is the best. 
                }
            }
            current = current.next; // Continue traversing by advancing a block forward.
            
        }
        return (best != null) ? finalizeAllocation(best, size) : -1; // If a block was found at the end of our loop, perform the allocation and return start address.
    }

  // Worst-fit algorithm 
    public int allocateWorst(int size) { // Take the block's size as parameter.
        Node current = freeList;  // Scanning again starts from the head.
        Node worst = null;		  // Declaring the worst block. It is not assigned yet so it's null.	
        int maxBlockSize = -1;    // tracks size of largest fitting block so far.

        while (current != null) {  // Start the searching.
            if (current.size >= size && current.size > maxBlockSize) { // Now we have two conditions. First check our block is fitting then check if it is larger than the previously found free block.
                maxBlockSize = current.size;  
                worst = current;             // Update the largest block found
            }
            current = current.next;  // Continue searching.
        }
        return (worst != null) ? finalizeAllocation(worst, size) : -1;  // If a block was found at the end of our loop, perform the allocation and return start address.
    }

    // Next-fit algorithm
    public int allocateNext(int size) { // Take the block's size as parameter.
        if (freeList == null) return -1; // If the list is empty this allocation method fails. 

        Node startNode = (lastAllocated != null) ? lastAllocated : freeList;  // This time our starting point is not from beginning. We start from the last allocation point.
        Node current = startNode;                                             // We determine that with the help of our "lastAllocated" pointer.

       
        while (current != null) {  // At first, we need to start from head. After we find our block we declare it as "lastAllocated". 
            if (current.size >= size) { 
                lastAllocated = current;        //  Update the pointer so our next call can start from this point.          
                return finalizeAllocation(current, size);  // If a block was found at the end of our loop, perform the allocation and return start address.
            }
            current = current.next;   // Otherwise continue searching.
        }

        
        current = freeList; 
        while (current != null && current != startNode) { // In this section we can imagine it as if we are turning our single linked list into a circular linked list to wrap it up.
            if (current.size >= size) {                   // To not check the list twice, we put our second condition where we stop at our currently last allocated block.
                lastAllocated = current;
                return finalizeAllocation(current, size);
            }
            current = current.next;  
        }

        return -1;   // If nothing fits anywhere we simply fail.
    }

    // Freeing an occupied bloc and inserting it back into the free list.
    public void free(int start, int size) {
        Node newNode = new Node(start, size);  //  Create a new free block node describing the freed region.

        // insert into free list sorted by start address
        if (freeList == null || start < freeList.start) {  //If the list is empty or this block starts before the current head insert at the front.
            newNode.next = freeList;  
            freeList = newNode;
        } else {  //Otherwise traverse to find correct position and insert between nodes.
            Node curr = freeList;
            while (curr.next != null && curr.next.start < start) {
                curr = curr.next;
            }
            newNode.next = curr.next;
            curr.next = newNode;
        }

        merge(); // After inserting, merge adjacent free blocks to reduce fragmentation.
    }

   
    public boolean freeBySize(int size) {  // Frees the first allocated block whose size matches the size as we take as parameter.
        for (int i = 0; i < history.size(); i++) {
            AllocationRecord rec = history.get(i);  // Search the allocations 
            if (rec.size == size) {                 // If match found, remove
                free(rec.start, rec.size);          // Free the memory according to size and starting block
                history.remove(i);
                return true;
            }
        }
        return false; // If we cannot find an allocation at given size, we fail.
    }

   // A helper that actually performs the allocation once a suitable free block has been chosen.
    private int finalizeAllocation(Node target, int size) {
        int start = target.start;

        // record the allocation (needed for freeBySize)
        history.add(new AllocationRecord(start, size));

       
        if (target.size == size) {   // Split or remove the free block
           
            Node removed = target;  // If it’s an exact fit, remove this free block node from the free list.
            removeNode(target);

            
            if (lastAllocated == removed) { // Our removed node might be the one that keeps the track of "lastAllocated" node needed for next-fit algorithm.
                lastAllocated = freeList;   // To keep track we reset our pointer.
            }
        } else {
            target.start += size;  // If not exact fit, shrink the free block by moving its start forward and
            target.size -= size;   // reducing its size.
        }

        return start;
    }

    private void removeNode(Node target) { // Helper to delete a node from the free list.
        if (freeList == target) {          // If the target is the head, just move the head forward.
            freeList = freeList.next;
            return;
        }
        Node curr = freeList;
        while (curr.next != null && curr.next != target) { //  Otherwise traverse until "curr.next" is the target.
            curr = curr.next;
        }
        if (curr.next != null) {
            curr.next = curr.next.next;
        }
    }

  
    private void merge() {     // Combines touching free blocks into bigger blocks to reduce fragmentation.
        Node curr = freeList;  // Start from the head.
        while (curr != null && curr.next != null) {  // As long as there is a current and a next block ,  if current block ends exactly where next block begins, they are adjacent.
            if (curr.start + curr.size == curr.next.start) {  // Merge them to grow current size.
                curr.size += curr.next.size;
                curr.next = curr.next.next;
            } else {
                curr = curr.next;  // If not adjacent, move forward.
            }
        }
    }
 
     // Printing the whole list.
    public void printFreeList() {
        Node curr = freeList;
        if (curr == null) {     // Check if the list is empty. 
            System.out.println("Free List: [EMPTY]");
            return;
        }
        System.out.print("Free List: ");
        while (curr != null) {  // If not traverse all the blocks from the start. 
            System.out.printf("[%d-%d, size %d] ",  // Print each free block as :
                    curr.start,                     // start address
                    curr.start + curr.size - 1,     // end address
                    curr.size);                     // size 
            curr = curr.next;                       // Advance to the next node.
        }
        System.out.println();
    }
}
package pack61;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class FitAlgorithmsComparison {

    static final int[] TRACE = {10, 5, 20, -5, 12, -10, 8, 6, 7, 3, 10};  // Positive numbers  represents the allocation requests and negative numbers are for freeing the first block of that size.

    private static int allocateWithAlg(FitAllocator a, String alg, int size) {  // Choose which allocation strategy to be used.
        if (alg.equals("Best Fit")) return a.allocateBest(size);
        if (alg.equals("Worst Fit")) return a.allocateWorst(size);
        return a.allocateNext(size);
    }

   
    public static void experiment1AllocationTrace() {

        System.out.println("\n  EXPERIMENT 1: ALLOCATION TRACE  \n");  

        String[] algs = {"Best Fit", "Worst Fit", "Next Fit"};  // All of our algorithms

        for (String alg : algs) {  //  Runs the allocation trace once per algorithm.

            System.out.println("\n------------------------------------------------");
            System.out.println("Algorithm: " + alg);
            System.out.println("------------------------------------------------");

            FitAllocator allocator = new FitAllocator();  // Creates a fresh memory allocator. Ensures algorithms do not interfere with each other.


            int step = 1;  // Step counter to show sequence progress.

            for (int req : TRACE) {  // Iterates through the fixed allocation

                System.out.println("\nStep " + step++);  // Prints the step number, then increments it

                if (req > 0) {
                    int start = allocateWithAlg(allocator, alg, req);  // Calls the correct allocation algorithm.
                    System.out.println("Request: allocate(" + req + ")");
                    System.out.println("Result : start address = " + start);
                } else {
                    int sizeToFree = -req;
                    boolean freed = allocator.freeBySize(sizeToFree);
                    System.out.println("Request: free(" + sizeToFree + ")");
                    System.out.println("Result : success = " + freed);   // Prints whether the free operation succeeded.
                }

                System.out.print("Free List: ");  // Prints the entire free list after every operation.
                allocator.printFreeList();
            }
        }
    }

   
    public static void experiment2FragmentationTest() {

        System.out.println("\n EXPERIMENT 2: FRAGMENTATION TEST \n");  

        String[] algs = {"Best Fit", "Worst Fit", "Next Fit"};  // Our algorithms

        for (String alg : algs) {    //  Runs the allocation trace once per algorithm.

            System.out.println("\n------------------------------------------------");
            System.out.println("Algorithm: " + alg);
            System.out.println("------------------------------------------------");

            FitAllocator allocator = new FitAllocator();  // New allocator for clean memory state.
            Random rand = new Random(42);
            List<AllocationRecord> allocated = new ArrayList<>(); //  A list to  store currently allocated blocks. This will make every allocation more accessible when deleting them. 

            for (int i = 0; i < 12; i++) {  // Loop for required 12 allocations.
                int size = rand.nextInt(10) + 3;  // Generates sizes between 3 and 12.
                int start = allocateWithAlg(allocator, alg, size);  // Allocates memory using selected algorithm.

                System.out.println("allocate(" + size + ") -> start=" + start);

                if (start != -1) {
                    allocated.add(new AllocationRecord(start, size));  // Saves successful allocations for later freeing.
                }
            }

            System.out.println("\nFreeing 4 random blocks...");

            for (int i = 0; i < 4 && !allocated.isEmpty(); i++) {  // Selects and removes a random allocated block.
                AllocationRecord rec = allocated.remove(rand.nextInt(allocated.size()));
                allocator.free(rec.start, rec.size);
                System.out.println("freed block of size " + rec.size);
            }

            System.out.print("\nFinal Free List: ");
            allocator.printFreeList();  // Shows final memory state.

            int big = allocateWithAlg(allocator, alg, 25);  // Attempts to allocate a large block of size 25.
            System.out.println("Attempt allocate(25) -> " + (big != -1));  // Prints success or failure.
        }
    }

  
    public static void experiment3SpeedTest() {

        System.out.println("\n EXPERIMENT 3: SPEED TEST \n");

        String[] algs = {"Best Fit", "Worst Fit", "Next Fit"};   // Algorithms

        for (String alg : algs) {  // Tests each algorithm separately.

            FitAllocator allocator = new FitAllocator();  // Initialize an allocator.
            Random rand = new Random(7);                  // Random generator.
            List<AllocationRecord> allocated = new ArrayList<>(); // An array list to store allocations.

            long startTime = System.nanoTime();  // Start the timer.

            for (int i = 0; i < 200; i++) {  
                int size = rand.nextInt(10) + 1; // Assign a random size between 1-10.
                int start = allocateWithAlg(allocator, alg, size);  // Allocate the memory.

                if (start != -1) {
                    allocated.add(new AllocationRecord(start, size));
                }

                if (!allocated.isEmpty()) {  // If there is something to free , free a random previously allocated block
                    AllocationRecord rec = allocated.remove(rand.nextInt(allocated.size()));
                    allocator.free(rec.start, rec.size);
                }
            }

            long endTime = System.nanoTime(); //  stop the timer to get the results.
            double elapsedMs = (endTime - startTime) / 1_000_000.0;  // Measurement of time was done by nanoseconds. We convert it to milliseconds create more readable output.

            System.out.printf("%s execution time: %.3f ms%n", alg, elapsedMs); // Prints total execution time for the algorithm.
        }
    }


    public static void main(String[] args) {
        experiment1AllocationTrace();
        experiment2FragmentationTest();
        experiment3SpeedTest();
    }
}


