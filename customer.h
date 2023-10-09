# ifndef CUSTOMER_H
# define CUSTOMER_H

struct {
    int arrival_time;
    char[2] seller; // keep track of who is the seller 
    int id;         // keep track of customer id
    int response_time;
    int turnaround_time;
    int throughput;
} customer;

#endif