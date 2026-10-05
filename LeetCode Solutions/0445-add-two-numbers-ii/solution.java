// LeetSync metadata
// Source: LEETCODE
// Problem: 445. Add Two Numbers II
// Language: java

/**
 * Definition for singly-linked list.
 * public class ListNode {
 *     int val;
 *     ListNode next;
 *     ListNode() {}
 *     ListNode(int val) { this.val = val; }
 *     ListNode(int val, ListNode next) { this.val = val; this.next = next; }
 * }
 */
class Solution {
    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        StringBuilder sb1 = new StringBuilder();
        ListNode curr = l1;
        while (curr != null) {
            sb1.append(curr.val);
            curr = curr.next;
        }

        StringBuilder sb2 = new StringBuilder();
        curr = l2;
        while (curr != null) {
            sb2.append(curr.val);
            curr = curr.next;
        }

        int carry = 0;
        int i = sb1.length() - 1;
        int j = sb2.length() - 1;
        ListNode dummy = new ListNode(0);
        ListNode currDummy = dummy;

        while (i >= 0 || j >= 0 || carry > 0) {
            int sum = carry;
            if (i >= 0) sum += sb1.charAt(i--) - '0';
            if (j >= 0) sum += sb2.charAt(j--) - '0';

            carry = sum / 10;
            ListNode newNode = new ListNode(sum % 10);
            newNode.next = dummy.next;
            dummy.next = newNode;
        }

        return dummy.next;
    }
}
