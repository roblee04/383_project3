/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */


package project3;
import java.util.Random;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        System.out.println("testing abab");

        Customer[] customers = generate_customers('b', 5);
        int ar[] = { 1, 2, 3, 4, 5, 6, 7, 8 };


        // try to print out all customers arrival times
        for(int i = 0; i < ar.length(); i++) { // cannot find symbol .length() or .size()?
            // System.out.println(customers[i].arrival_time);
            System.out.println(ar[i]);
        }
    }

    public Customer[] generate_customers(char seller, int num_customers) {
        Customer[] customer_list = new Customer[num_customers];

        // check who the seller is

        for (int i = 0; i < num_customers; i++) {
            // fill in fields for customer
            // int arrival_time, int id, int response_time, int turnaround_time, int throughput

            // random time between 0 - 60
            Random rand = new Random();
            int arrival_time = rand.nextInt(61);
            int id = i;
            int service_time = generate_service_time(seller);

            Customer customer = new Customer(arrival_time, id, service_time, 0, 0, 0);


            // add customer to the customer list
            customer_list[i] = customer;
        }

        return customer_list;
    }

    public int generate_service_time(char seller) {
        // function for parsing h/m/l and returning
//         High-price ticket customers get
        // the fastest service, and each requires randomly exactly 1 or 2 minutes to complete
        // a ticket sale. Medium-price ticket customers each requires randomly exactly 2, 3,
        // or 4 minutes to complete a sale. Low-price ticket customers each requires ran-
        // domly exactly 4, 5, 6, or 7 minutes to complete a sale.

        return 2;
    }

}
