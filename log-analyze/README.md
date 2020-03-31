# Constraint Table
Implemented:
+ Ai + Ri <= 1
+ Near-miss guided releasing and acquiring generation:

    sum(Ai) >= 1, sum(Ri) >= 1
    sum(Ai) <= 2, sum(Ri) <= 2
+ 0 <= Ai,Ri <=1

To be implementated:
+ try to add constraint : match the releasing and acquiring varibles.

# Ongoing Problems

## P2: Understanding the results for toy example
For releasing sites, we made two mistakes : one is a Monitor.Enter(), the other is a heap write. 
For acquiring sites, we should implement window+1 strategy to include the potential delayed acquiring operation.

## P3: Add a parameter for regulation penalty
current it is 1.

## P4: Prepare the logs of a real application

# Solved Problems

## P1: Upload the toy C# example
Done.


