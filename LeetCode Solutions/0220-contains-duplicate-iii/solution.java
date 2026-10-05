// LeetSync metadata
// Source: LEETCODE
// Problem: 220. Contains Duplicate III
// Language: java

class Solution {
    public boolean containsNearbyAlmostDuplicate(int[] nums, int indexDiff, int valueDiff) {
        HashMap<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            for (int key : map.keySet()) {
                long diff = (long) nums[i] - (long) key;
                if (diff < 0) diff = - diff;

                if (diff <= valueDiff) {
                    int previousIndex = map.get(key);
                    if (i - previousIndex <= indexDiff) {
                        return true;
                    }
                }
            }
            map.put(nums[i], i);
        }

        return false;
    }
}
