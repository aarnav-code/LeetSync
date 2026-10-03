import java.util.Scanner;

class Solution {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the no. of terms in the array: ");
        int terms = scanner.nextInt();

        int[] arr = new int[terms];

        for (int i = 0; i < terms; i++) {
            System.out.print("Enter element #" + (i+1) + ": ");
            arr[i] = scanner.nextInt();
        }

        System.out.print("Enter the target value: ");
        int target = scanner.nextInt();
        scanner.nextLine();

        Solution solution = new Solution();
        int[] result = solution.twoSum(arr, target);

        if (result.length == 0) {
            System.out.println("NO SOLUTION");
        }else {
            System.out.println("[" + result[0] + ", " + result[1] + "]");
        }

    }
    int[] twoSum(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int[] {i, j};
                }
            }
        }
        return new int[] {};
    }
}