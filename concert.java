import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;


//Initialize
public class Main {

        static final int hsellercount = 1;
        static final int msellercount = 3;
        static final int lsellercount = 6;
        static final int total_seller = hsellercount + msellercount + lsellercount;
        static final int concertrow = 10;
        static final int concertcol = 10;
        static final int simulation_duration = 60;

        // Seller  Structure
        static class Seller {
            char sellerno;
            char sellertype;
            Queue<Customer> sellerqueue;

            Seller(char sellerno, char seller_type, Queue<Customer> seller_queue) {
                this.sellerno = sellerno;
                this.sellertype = seller_type;
                this.sellerqueue = seller_queue;
            }
        }

     // Customer  Structure
        static class Customer {
            char cust_no;
            int arrival_time;

            Customer(char cust_no, int arrival_time) {
                this.cust_no = cust_no;
                this.arrival_time = arrival_time;
            }
        }

        static int sim_time;
        static int N = 5;
        static String[][][] seatmatrix = new String[concertrow][concertcol][3];

        // Thread Variable
        static Thread[] seller_t = new Thread[total_seller];
        static Lock threadCountLock = new ReentrantLock();
        static Lock threadWaitingLock = new ReentrantLock();
        static Lock reservationLock = new ReentrantLock();
        static Lock conditionLock = new ReentrantLock();
        static Condition condition = conditionLock.newCondition();

        static int thread_count = 0;
        static int threads_waiting_for_clock_tick = 0;
        static int active_thread = 0;
        //static int verbose = 0;

        public static void main(String[] args) {
            if (args.length == 1) {
                N = Integer.parseInt(args[0]);
            }

            // Initialize Global Variables
            for (int r = 0; r < concertrow; r++) {
                for (int c = 0; c < concertcol; c++) {
                    //seatmatrix[r][c] = new String("-");
                    seatmatrix[r][c][0] = "-";
                }
            }

            // Create all threads
            createSellerThreads('H', hsellercount);
            createSellerThreads('M', msellercount);
            createSellerThreads('L', lsellercount);

            // Wait for threads to finish initialization and wait for synchronized clock tick
            while (true) {
                threadCountLock.lock();
                if (thread_count == 0) {
                    threadCountLock.unlock();
                    break;
                }
                threadCountLock.unlock();
            }

            // Simulate each time quanta/slice as one iteration
            System.out.println("Starting Simulation");
            threads_waiting_for_clock_tick = 0;
            wakeup_all_seller_threads(); // For the first tick

            do {
                // Wake up all threads
                waitForThreadToServeCurrentTimeSlice();
                sim_time = sim_time + 1;
                wakeup_all_seller_threads();
                // Wait for thread completion
            } while (sim_time < simulation_duration);

            // Wake up all threads so that no more threads keep waiting for clock Tick
            wakeup_all_seller_threads();

            while (active_thread > 0) ;

            // Display concert chart
            System.out.println("\n\nFinal Concert Seat Chart");
            System.out.println("========================\n");

            int h_customers = 0, m_customers = 0, l_customers = 0;
            for (int r = 0; r < concertrow; r++) {
                for (int c = 0; c < concertcol; c++) {
                    if (c != 0)
                        System.out.print("\t");

                    String seatInfo = seatmatrix[r][c][0];

                    if (seatmatrix[r][c][1] != null) {
                        seatInfo += seatmatrix[r][c][1];
                    }

                    if (seatmatrix[r][c][2] != null) {
                        seatInfo += seatmatrix[r][c][2];
                    }

                    System.out.printf("%-12s", seatInfo);

                    if (seatmatrix[r][c][0].equals("H")) h_customers++;
                    if (seatmatrix[r][c][0].equals("M")) m_customers++;
                    if (seatmatrix[r][c][0].equals("L")) l_customers++;
                }
                System.out.println();
            }

            System.out.println("\n\nStat for N = " + N);
            System.out.println("===============\n");
            System.out.println(" ============================================\n");
            System.out.println("|   | No of Customers | Got Seat | Returned |\n");
            System.out.println(" ============================================\n");
            System.out.printf("| H | %15d | %8d | %8d |\n", hsellercount * N, h_customers, (hsellercount * N) - h_customers);
            System.out.printf("| M | %15d | %8d | %8d |\n", msellercount * N, m_customers, (msellercount * N) - m_customers);
            System.out.printf("| L | %15d | %8d | %8d |\n", lsellercount * N, l_customers, (lsellercount * N) - l_customers);
            System.out.println(" ============================================\n");




        }

        public static void createSellerThreads(char seller_type, int no_of_sellers) {
            // Create all threads
            for (int t_no = 0; t_no < no_of_sellers; t_no++) {
                Queue<Customer> seller_queue = generateCustomerQueue(N);
                Seller seller_arg = new Seller((char) t_no, seller_type, seller_queue);

                threadCountLock.lock();
                thread_count++;
                threadCountLock.unlock();

                seller_t[t_no] = new Thread(() -> sell(seller_arg));
                seller_t[t_no].start();
            }
        }



        public static void waitForThreadToServeCurrentTimeSlice() {
            // Check if all threads have finished their jobs for this time slice
            while (true) {
                threadWaitingLock.lock();
                if (threads_waiting_for_clock_tick == active_thread) {
                    threads_waiting_for_clock_tick = 0;
                    threadWaitingLock.unlock();
                    break;
                }
                threadWaitingLock.unlock();
            }
        }

        public static void wakeup_all_seller_threads() {
            conditionLock.lock();
            condition.signalAll();
            conditionLock.unlock();
        }

        public static void sell(Seller args) {
            // Initializing thread
            char seller_type = args.sellertype;
            int seller_no;

            if (seller_type =='H'){
                seller_no = 0;
            }else{
                seller_no = args.sellerno + 1;
            }



            Queue<Customer> customer_queue = args.sellerqueue;
            Queue<Customer> seller_queue = new LinkedList<>();

            threadCountLock.lock();
            thread_count--;
            active_thread++;
            threadCountLock.unlock();

            Customer cust = null;
            int random_wait_time = 0;

            while (sim_time < simulation_duration) {
                // Waiting for clock tick
                conditionLock.lock();
                threadWaitingLock.lock();
                threads_waiting_for_clock_tick++;
                threadWaitingLock.unlock();

                try {
                    condition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                conditionLock.unlock();

                // Sell
                if (sim_time == simulation_duration)
                    break;

                // All New Customer Came
                while (!customer_queue.isEmpty() && customer_queue.peek().arrival_time <= sim_time) {
                    Customer temp = customer_queue.poll();
                    seller_queue.add(temp);
                    System.out.println("00:" + String.format("%02d", sim_time) + " " + seller_type + seller_no + " " +"Customer No " + seller_type + seller_no + String.format("%02d", (int) temp.cust_no) + " arrived");
                }

                // Serve next customer
                if (cust == null && !seller_queue.isEmpty()) {
                    cust = seller_queue.poll();
                    System.out.println("00:" + String.format("%02d", sim_time) + " " + seller_type + seller_no + " " +"Serving Customer No " + seller_type + seller_no + String.format("%02d",(int) cust.cust_no));

                    switch (seller_type) {
                        case 'H':
                            random_wait_time = (int) (Math.random() * 2) + 1;
                            break;
                        case 'M':
                            random_wait_time = (int) (Math.random() * 3) + 2;
                            break;
                        case 'L':
                            random_wait_time = (int) (Math.random() * 4) + 4;
                    }
                }

                if (cust != null) {
                    if (random_wait_time == 0) {
                        // Selling Seat
                        reservationLock.lock();

                        // Find seat
                        int seatIndex = findAvailableSeat(seller_type);
                        //System.out.println("seatIndex"+seatIndex);
                        if (seatIndex == -1) {
                            System.out.println("00:" + String.format("%02d", sim_time) + " " + seller_type + seller_no + " Customer No " + seller_type + seller_no + String.format("%02d",(int) cust.cust_no) + " has been told Concert Sold Out.");
                        } else {
                            int row_no = seatIndex / concertcol;
                            int col_no = seatIndex % concertcol;
                            seatmatrix[row_no][col_no][0] = String.valueOf(seller_type);
                            seatmatrix[row_no][col_no][1] = String.valueOf(seller_no);
                            seatmatrix[row_no][col_no][2] = String.format("%02d", (int) cust.cust_no);

                            System.out.println("00:" + String.format("%02d", sim_time) + " " + seller_type + seller_no + " Customer No " + seller_type + seller_no + String.format("%02d", (int) cust.cust_no) + " assigned seat " + row_no + "," + col_no);
                        }
                        reservationLock.unlock();
                        cust = null;
                    } else {
                        random_wait_time--;
                    }
                }
            }

            while (cust != null || !seller_queue.isEmpty()) {
                if (cust == null)
                    cust = seller_queue.poll();
                System.out.println("00:" + String.format("%02d", sim_time) + " " + seller_type + seller_no + " Ticket" +" Sale Closed. Customer No " + seller_type + seller_no + String.format("%02d", (int) cust.cust_no) + " Leaves");
                cust = null;
            }

            threadCountLock.lock();
            active_thread--;
            threadCountLock.unlock();
        }

        public static int findAvailableSeat(char seller_type) {
            int seatIndex = -1;

            if (seller_type == 'H') {
                for (int row_no = 0; row_no < concertrow; row_no++) {
                    for (int col_no = 0; col_no < concertcol; col_no++) {
                        if (seatmatrix[row_no][col_no][0].equals("-")) {
                            seatIndex = row_no * concertcol + col_no;
                            return seatIndex;
                        }
                    }
                }
            } else if (seller_type == 'M') {
                int mid = concertrow / 2;
                int row_jump = 0;
                //int next_row_no = mid;
                for (row_jump = 0; ; row_jump++) {
                    // code for rows 5,6 and so on
                    int row_no = mid + row_jump;
                    if (mid + row_jump < concertrow) {
                        for (int col_no = 0; col_no < concertcol; col_no++) {
                            if (seatmatrix[row_no][col_no][0].equals("-")) {
                                seatIndex = row_no * concertcol + col_no;
                                return seatIndex;
                            }
                        }
                    }
                    // code for rows 4,3 and so on
                    row_no = mid - row_jump;
                    if (mid - row_jump >= 0) {
                        for (int col_no = 0; col_no < concertcol; col_no++) {
                            if (seatmatrix[row_no][col_no][0].equals("-")) {
                                seatIndex = row_no * concertcol + col_no;
                                return seatIndex;
                            }
                        }
                    }
                    if (mid + row_jump >= concertrow && mid - row_jump < 0) {
                        break;
                    }
                }
            } else if (seller_type == 'L') {
                for (int row_no = concertrow - 1; row_no >= 0; row_no--) {
                    for (int col_no = concertcol - 1; col_no >= 0; col_no--) {
                        if (seatmatrix[row_no][col_no][0].equals("-")) {
                            seatIndex = row_no * concertcol + col_no;
                            return seatIndex;
                        }
                    }
                }
            }

            return -1;
        }

        public static Queue<Customer> generateCustomerQueue(int N) {
            Queue<Customer> customerQueue = new LinkedList<>();
            char cust_no = 0;

            while (N-- > 0) {
                int arrival_time = (int) (Math.random() * simulation_duration);
                Customer cust = new Customer(cust_no, arrival_time);
                customerQueue.add(cust);
                cust_no++;
            }

            customerQueue = sortCustomerQueue(customerQueue);
            cust_no = 0;

            for (Customer cust : customerQueue) {
                cust_no++;
                cust.cust_no = cust_no;
            }

            return customerQueue;
        }

        public static Queue<Customer> sortCustomerQueue(Queue<Customer> customerQueue) {
            // Convert the Queue to an ArrayList
            List<Customer> customerList = new ArrayList<>(customerQueue);

            // Create a custom comparator to sort based on arrival time
            Comparator<Customer> customerComparator = Comparator.comparingInt(customer -> customer.arrival_time);

            // Sort the list using the custom comparator
            customerList.sort(customerComparator);

            // Convert the sorted ArrayList back to a Queue
            Queue<Customer> sortedCustomerQueue = new LinkedList<>(customerList);

            return sortedCustomerQueue;
        }





}



