#include <map>
#include <cstdio>
#include <iostream>

using namespace std;

int main() {
    int count = 0;
    map<int, int> keys;
    int x;
    while (scanf("%d %*s", &x) >= 1) {
        ++count;
        keys[x]++;
    }
    cout << "#keys: " << keys.size() << '\n';
    cout << "#values: " << count << '\n';
    cout << "avg: " << double(count) / keys.size() << '\n';
    int max = 0;
    for (auto p : keys) {
        if (p.second > max) max = p.second;
    }
    cout << "max: " << max << '\n';
}
