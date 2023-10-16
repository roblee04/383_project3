import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

public class Main {
    // Constants for seller counts, simulation parameters, and concert dimensions
    static final int highSellerCount = 1;
    static final int mediumSellerCount = 3;
    static final int lowSellerCount = 6;
    static final int totalSellers = highSellerCount + mediumSellerCount + lowSellerCount;
    static final int concertRows = 10;
    static final int concertCols = 10;
    static final int simulationDuration = 60;

    // Seller Structure
    static class Seller {
        char sellerNo;  // Identifier for the seller
        char sellerType;  // Type of seller (H: High, M: Medium, L: Low)
        Queue<Customer> sellerQueue;  // Queue of customers waiting for this seller

        Seller(char sellerNo, char sellerType, Queue<Customer> sellerQueue) {
            this.sellerNo = sellerNo;
            this.sellerType = sellerType;
            this.sellerQueue = sellerQueue;
        }
    }

    // Customer Structure
    static class Customer {
        char custNo;  // Identifier for the customer
        int arrivalTime;  // Time when the customer arrives
        int responseTime;  // Time it takes for the customer to be served
        int turnaroundTime;  // Total time the customer spends, including wait and service time

        Customer(char custNo, int arrivalTime) {
            this.custNo = custNo;
            this.arrivalTime = arrivalTime;
        }
    }

    // Global simulation variables
    static int simulationTime;  // Current time in the simulation
    static int N = 15;  // Default number of customers per seller

    // Variables to track response and turnaround times for each seller type
    static int responseTimeForHigh = 0;
    static int responseTimeForMedium = 0;
    static int responseTimeForLow = 0;
    static int turnaroundTimeForHigh = 0;
    static int turnaroundTimeForMedium = 0;
    static int turnaroundTimeForLow = 0;

    // 3D array to represent the concert seat matrix
    static String[][][] seatMatrix = new String[concertRows][concertCols][3];

    // Thread-related variables
    static Thread[] sellerThreads = new Thread[totalSellers];
    static Lock threadCountLock = new ReentrantLock();
    static Lock threadWaitingLock = new ReentrantLock();
    static Lock reservationLock = new ReentrantLock();
    static Lock conditionLock = new ReentrantLock();
    static Condition condition = conditionLock.newCondition();

    static int threadCount = 0;
    static int threadsWaitingForClockTick = 0;
    static int activeThread = 0;

    public static void main(String[] args) {
        // Parse command line argument (if provided) to set the number of customers per seller
        if (args.length == 1) {
            N = Integer.parseInt(args[0]);
        }

        // Initialize the concert seat matrix
        for (int r = 0; r < concertRows; r++) {
            for (int c = 0; c < concertCols; c++) {
                seatMatrix[r][c][0] = "-";
            }
        }

        // Create seller threads for high, medium, and low sellers
        createSellerThreads('H', highSellerCount);
        createSellerThreads('M', mediumSellerCount);
        createSellerThreads('L', lowSellerCount);

        // Wait for threads to finish initialization and synchronize with the clock tick
        while (true) {
            threadCountLock.lock();
            if (threadCount == 0) {
                threadCountLock.unlock();
                break;
            }
            threadCountLock.unlock();
        }

        // Simulate the time slices
        System.out.println("Starting Simulation");

        threadsWaitingForClockTick = 0;
        wakeupAllSellerThreads(); // For the first clock tick

        do {
            // Wake up all threads, simulate one time slice, and wait for thread completion
            waitForThreadToServeCurrentTimeSlice();
            simulationTime = simulationTime + 1;
            wakeupAllSellerThreads();
        } while (simulationTime < simulationDuration);

        // Wake up all threads to prevent further waiting for clock ticks
        wakeupAllSellerThreads();

        while (activeThread > 0) ;

        // Display the final concert seat chart and statistics
        System.out.println("\n\nFinal Concert Seat Chart");
        System.out.println("========================\n");

        int highCustomers = 0, mediumCustomers = 0, lowCustomers = 0;
        for (int r = 0; r < concertRows; r++) {
            for (int c = 0; c < concertCols; c++) {
                if (c != 0)
                    System.out.print("\t");

                String seatInfo = seatMatrix[r][c][0];

                if (seatMatrix[r][c][1] != null) {
                    seatInfo += seatMatrix[r][c][1];
                }

                if (seatMatrix[r][c][2] != null) {
                    seatInfo += seatMatrix[r][c][2];
                }

                System.out.printf("%-12s", seatInfo);

                if (seatMatrix[r][c][0].equals("H")) highCustomers++;
                if (seatMatrix[r][c][0].equals("M")) mediumCustomers++;
                if (seatMatrix[r][c][0].equals("L")) lowCustomers++;
            }
            System.out.println();
        }

        System.out.println("\n\n");
        System.out.println("=============================");
        System.out.println("Multi-threaded Ticket Sellers");
        System.out.println("Stat for N = " + N);
        System.out.println("==============================\n");
        System.out.println(" =============================================================\n");
        System.out.println("|   | No of Customers | Got Seat | Returned | Throughput|\n");
        System.out.println(" =============================================================\n");
        System.out.printf("| H | %15d | %8d | %8d | %.2f \n", highSellerCount * N, highCustomers, (highSellerCount * N) - highCustomers, (highCustomers / 60.0));
        System.out.printf("| M | %15d | %8d | %8d | %.2f \n", mediumSellerCount * N, mediumCustomers, (mediumSellerCount * N) - mediumCustomers, (mediumCustomers / 60.0));
        System.out.printf("| L | %15d | %8d | %8d | %.2f \n", lowSellerCount * N, lowCustomers, (lowSellerCount * N) - lowCustomers, (lowCustomers / 60.0));
        System.out.println(" =============================================================\n");

        System.out.println(" -----------------------------------------------");
        System.out.println("|    | Avg response Time | Avg turnaround time| (in simulation time units) ");
        System.out.println(" ------------------------------------------------");
        System.out.printf("| %3c  | %.2f          | %.2f \t\t|\n", 'H', (double) responseTimeForHigh / (N * 1.0), (double) turnaroundTimeForHigh / (N * 1.0));
        System.out.printf("| %3c  | %.2f          | %.2f \t\t|\n", 'M', (double) responseTimeForMedium / (3.0 * N), (double) turnaroundTimeForMedium / (3.0 * N));
        System.out.printf("| %3c  | %.2f          | %.2f \t\t|\n", 'L', (double) responseTimeForLow / (6.0 * N), (double) turnaroundTimeForLow / (6.0 * N));
        System.out.println(" ------------------------------------------------");
    }

    // Create seller threads for a given seller type and count
    public static void createSellerThreads(char sellerType, int numberOfSellers) {
        // Create all threads
        for (int tNo = 0; tNo < numberOfSellers; tNo++) {
            Queue<Customer> sellerQueue = generateCustomerQueue(N);
            Seller sellerArg = new Seller((char) tNo, sellerType, sellerQueue);

            threadCountLock.lock();
            threadCount++;
            threadCountLock.unlock();

            sellerThreads[tNo] = new Thread(() -> sell(sellerArg));
            sellerThreads[tNo].start();
        }
    }

    // Wait for all threads to finish serving the current time slice
    public static void waitForThreadToServeCurrentTimeSlice() {
        while (true) {
            threadWaitingLock.lock();
            if (threadsWaitingForClockTick == activeThread) {
                threadsWaitingForClockTick = 0;
                threadWaitingLock.unlock();
                break;
            }
            threadWaitingLock.unlock();
        }
    }

    // Wake up all seller threads
    public static void wakeupAllSellerThreads() {
        conditionLock.lock();
        condition.signalAll();
        conditionLock.unlock();
    }

    // Logic to simulate the behavior of a seller
    public static void sell(Seller args) {
        // Initializing thread
        char sellerType = args.sellerType;
        int sellerNo;

        if (sellerType == 'H') {
            sellerNo = 0;
        } else {
            sellerNo = args.sellerNo + 1;
        }

        Queue<Customer> customerQueue = args.sellerQueue;
        Queue<Customer> sellerQueue = new LinkedList<>();

        threadCountLock.lock();
        threadCount--;
        activeThread++;
        threadCountLock.unlock();

        Customer cust = null;
        int randomWaitTime = 0;

        while (simulationTime < simulationDuration) {
            // Waiting for clock tick
            conditionLock.lock();
            threadWaitingLock.lock();
            threadsWaitingForClockTick++;
            threadWaitingLock.unlock();

            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            conditionLock.unlock();

            // Sell
            if (simulationTime == simulationDuration)
                break;

            // All New Customers Arrived
            while (!customerQueue.isEmpty() && customerQueue.peek().arrivalTime <= simulationTime) {
                Customer temp = customerQueue.poll();
                sellerQueue.add(temp);
                System.out.println("00:" + String.format("%02d", simulationTime) + " " + sellerType + sellerNo + " " + "Customer No "
                        + sellerType + sellerNo + String.format("%02d", (int) temp.custNo) + " arrived");
            }

            // Serve the next customer
            if (cust == null && !sellerQueue.isEmpty()) {
                cust = sellerQueue.poll();
                cust.responseTime = simulationTime - cust.arrivalTime;
                System.out.println("00:" + String.format("%02d", simulationTime) + " " + sellerType + sellerNo + " " + "Serving Customer No "
                        + sellerType + sellerNo + String.format("%02d", (int) cust.custNo) + " with a response time of " + cust.responseTime);

                switch (sellerType) {
                    case 'H':
                        randomWaitTime = (int) (Math.random() * 2) + 1;
                        responseTimeForHigh += cust.responseTime;
                        break;
                    case 'M':
                        randomWaitTime = (int) (Math.random() * 3) + 2;
                        responseTimeForMedium += cust.responseTime;
                        break;
                    case 'L':
                        randomWaitTime = (int) (Math.random() * 4) + 4;
                        responseTimeForLow += cust.responseTime;
                }
            }

            if (cust != null) {
                if (randomWaitTime == 0) {
                    // Selling a seat
                    reservationLock.lock();

                    // Find an available seat
                    int seatIndex = findAvailableSeat(sellerType);
                    if (seatIndex == -1) {
                        System.out.println("00:" + String.format("%02d", simulationTime) + " " + sellerType + sellerNo + " Customer No "
                                + sellerType + sellerNo + String.format("%02d", (int) cust.custNo) + " has been told the concert is Sold Out.");
                    } else {
                        int rowNo = seatIndex / concertCols;
                        int colNo = seatIndex % concertCols;
                        cust.turnaroundTime += simulationTime;
                        seatMatrix[rowNo][colNo][0] = String.valueOf(sellerType);
                        seatMatrix[rowNo][colNo][1] = String.valueOf(sellerNo);
                        seatMatrix[rowNo][colNo][2] = String.format("%02d", (int) cust.custNo);

                        System.out.println("00:" + String.format("%02d", simulationTime) + " " + sellerType + sellerNo + " Customer No "
                                + sellerType + sellerNo + String.format("%02d", (int) cust.custNo) + " assigned seat " + rowNo + "," + colNo
                                + " with a turnaround time of " + cust.turnaroundTime);

                        if (sellerType == 'H') {
                            turnaroundTimeForHigh += cust.turnaroundTime;
                        } else if (sellerType == 'M') {
                            turnaroundTimeForMedium += cust.turnaroundTime;
                        } else if (sellerType == 'L') {
                            turnaroundTimeForLow += cust.turnaroundTime;
                        }
                    }
                    reservationLock.unlock();
                    cust = null;
                } else {
                    randomWaitTime--;
                }
            }
        }

        while (cust != null || !sellerQueue.isEmpty()) {
            if (cust == null)
                cust = sellerQueue.poll();
            System.out.println("00:" + String.format("%02d", simulationTime) + " " + sellerType + sellerNo + " Ticket" + " Sale Closed. Customer No " + sellerType + sellerNo + String.format("%02d", (int) cust.custNo) + " Leaves");
            cust = null;
        }

        threadCountLock.lock();
        activeThread--;
        threadCountLock.unlock();
    }

    // Find an available seat for a given seller type
    public static int findAvailableSeat(char sellerType) {
        int seatIndex = -1;

        if (sellerType == 'H') {
            for (int rowNo = 0; rowNo < concertRows; rowNo++) {
                for (int colNo = 0; colNo < concertCols; colNo++) {
                    if (seatMatrix[rowNo][colNo][0].equals("-")) {
                        seatIndex = rowNo * concertCols + colNo;
                        return seatIndex;
                    }
                }
            }
        } else if (sellerType == 'M') {
            int mid = concertRows / 2;
            int rowJump = 0;
            for (rowJump = 0; ; rowJump++) {
                int rowNo = mid + rowJump;
                if (mid + rowJump < concertRows) {
                    for (int colNo = 0; colNo < concertCols; colNo++) {
                        if (seatMatrix[rowNo][colNo][0].equals("-")) {
                            seatIndex = rowNo * concertCols + colNo;
                            return seatIndex;
                        }
                    }
                }
                rowNo = mid - rowJump;
                if (mid - rowJump >= 0) {
                    for (int colNo = 0; colNo < concertCols; colNo++) {
                        if (seatMatrix[rowNo][colNo][0].equals("-")) {
                            seatIndex = rowNo * concertCols + colNo;
                            return seatIndex;
                        }
                    }
                }
                if (mid + rowJump >= concertRows && mid - rowJump < 0) {
                    break;
                }
            }
        } else if (sellerType == 'L') {
            for (int rowNo = concertRows - 1; rowNo >= 0; rowNo--) {
                for (int colNo = concertCols - 1; colNo >= 0; colNo--) {
                    if (seatMatrix[rowNo][colNo][0].equals("-")) {
                        seatIndex = rowNo * concertCols + colNo;
                        return seatIndex;
                    }
                }
            }
        }

        return -1;
    }

    // Generate a customer queue with random arrival times
    public static Queue<Customer> generateCustomerQueue(int N) {
        Queue<Customer> customerQueue = new LinkedList<>();
        char custNo = 0;

        while (N-- > 0) {
            int arrivalTime = (int) (Math.random() * simulationDuration);
            Customer cust = new Customer(custNo, arrivalTime);
            customerQueue.add(cust);
            custNo++;
        }

        customerQueue = sortCustomerQueue(customerQueue);
        custNo = 0;

        for (Customer cust : customerQueue) {
            custNo++;
            cust.custNo = custNo;
        }

        return customerQueue;
    }

    // Sort a customer queue based on arrival time
    public static Queue<Customer> sortCustomerQueue(Queue<Customer> customerQueue) {
        // Convert the Queue to an ArrayList
        List<Customer> customerList = new ArrayList<>(customerQueue);

        // Create a custom comparator to sort based on arrival time
        Comparator<Customer> customerComparator = Comparator.comparingInt(customer -> customer.arrivalTime);

        // Sort the list using the custom comparator
        customerList.sort(customerComparator);

        // Convert the sorted ArrayList back to a Queue
        Queue<Customer> sortedCustomerQueue = new LinkedList<>(customerList);

        return sortedCustomerQueue;
    }
}
