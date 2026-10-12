// LeetSync metadata
// Source: LEETCODE
// Problem: 4. Median of Two Sorted Arrays
// Language: java

import java.util.Scanner;

public class Solution {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the no. of elements in Array #1: ");
        int size1 = scanner.nextInt();

        System.out.print("Enter the no. of elements in Array #2: ");
        int size2 = scanner.nextInt();

        int[] arr1 = new int[size1];
        int[] arr2 = new int[size2];

        System.out.print("Enter the elements of Array #1: ");
        for (int i = 0; i < size1; i++) {
            arr1[i] = scanner.nextInt();
        }

        System.out.print("Enter the elements of Array #2: ");
        for (int i = 0; i < size2; i++) {
            arr2[i] = scanner.nextInt();
        }

        double result = findMedianSortedArrays(arr1, arr2);
        System.out.println("Median of the sorted arrays: " + result);
    }

    static double findMedianSortedArrays(int[] nums1, int[] nums2) {
        int[] tempArray = new int[nums1.length + nums2.length];

        int i = 0;
        for (i = 0; i < nums1.length; i++) {
            tempArray[i] = nums1[i];
        }
        for (int j = 0; j < nums2.length; j++) {
            tempArray[i+j] = nums2[j];
        }

        int size = tempArray.length;
        for (int k = 0; k < size; k++) {
            int min = k;
            for (int m = k + 1; m < size; m++) {
                if (tempArray[min] > tempArray[m]) {
                    min = m;
                }
            }
            int temp = tempArray[min];
            tempArray[min] = tempArray[k];
            tempArray[k] = temp;
        }

        double median;
        if (size % 2 == 0) {
            median = (double) (tempArray[size / 2] + tempArray[size / 2 - 1]) /2;
        }
        else{
            median = tempArray[size/2];
        }

        return median;
    }
}
