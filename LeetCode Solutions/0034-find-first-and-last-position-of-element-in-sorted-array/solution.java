// LeetSync metadata
// Source: LEETCODE
// Problem: 34. Find First and Last Position of Element in Sorted Array
// Language: java

class Solution {
    public int[] searchRange(int[] nums, int target) {
        /*int left = 0;
        int right = nums.length - 1;
        int[] result = {-1, -1};
        while (left <= right) {
            if (nums[left] == target) {
                if (result[0] == -1) {
                    result[0] = left;
                }
                else {
                    if (result[1] != -1) {
                        result[1] = left;
                    }
                }
            }
            else if (nums[right] == target) {
                if (result[1] == -1) {
                    result[1] = right;
                }
                else {
                    if (result[0] == -1) {
                        result[0] = right;
                    }
                }
            }

            left++;
            right--;
        }

        return result;*/

        int first = findFirst(nums, target);
        int last = findLast(nums, target);
        return new int[]{first, last};
    }

    int findFirst(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        int index = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] >= target) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
            if (mid >= 0 && mid < nums.length && nums[mid] == target) index = mid;
        }
        return index;
    }    
    
    int findLast(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        int index = -1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] <= target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
            if (mid >= 0 && mid < nums.length && nums[mid] == target) index = mid;
        }
        return index;
    }
}
