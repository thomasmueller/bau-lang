// https://github.com/jabbalaci/SpeedTests/blob/master/c/main.c

#include <stdio.h>
#include <math.h>

#define MAX 440000000

int cache[10];
int is_munchausen(int number);
void set_cache();

int main() {
    set_cache();
    for (int i = 0; i < MAX; ++i)
        if (is_munchausen(i))
            printf("%d\n", i);
    return 0;
}

int is_munchausen(int number) {
    int n = number;
    int total = 0;
    while (n > 0) {
        int digit = n % 10;
        total += cache[digit];
        if (total > number)
            return 0;
        n /= 10;
    }
    return total == number;
}

void set_cache() {
    cache[0] = 0;
    for (int i = 1; i <= 9; ++i)
        cache[i] = pow(i, i);
}
