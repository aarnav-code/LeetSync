// LeetSync metadata
// Source: LEETCODE
// Problem: 7. Reverse Integer
// Language: java

class Solution {
    public int reverse(int x) {
        long result = 0;
        if (x < 0) {
            long temp = (long) (-1) * x;
            while (temp > 0) {
                long digit = temp % 10;
                result = result * 10 + digit;
                temp /= 10;
            }

            result = -result;
        }

        long temp = x;
        while (temp > 0) {
            long digit = temp % 10;
            result = result * 10 + digit;
            temp /= 10;
        }

        if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            return 0;
        }
        return (int) result;
    }
}
