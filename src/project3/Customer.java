/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package project3;

public class Customer {
    int arrival_time;
    char[] seller; // keep track of who is the seller
    int id;         // keep track of customer id
    int response_time;
    int turnaround_time;
    int throughput;

    public Customer(int arrival_time, int id, int response_time, int turnaround_time, int throughput) {
        this.arrival_time = arrival_time;
        this.seller = new char[2];
        this.id = id;
        this.response_time = response_time;
        this.turnaround_time = turnaround_time;
        this.throughput = throughput;
    }

    public void generate_customers(int num_customers) {

    }

}
