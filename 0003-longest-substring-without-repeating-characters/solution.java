class Solution {
    public int lengthOfLongestSubstring(String s) {
        if (s.length() == 0) return 0;
        if (s.length() == 1) return 1;

        StringBuilder temp = new StringBuilder();
        StringBuilder temp2 = new StringBuilder();
        temp.append(s.charAt(0));
        temp2.append(s.charAt(0));

        for (int i = 1; i < s.length(); i++) {
            int count = 0;
            for (int j = 0; j < temp2.length(); j++) {
                if (s.charAt(i) == temp2.charAt(j)) {
                    count++;
                }
            }
            if (count == 0) {
                temp2.append(s.charAt(i));
            } else {
                int dupIndex = temp2.indexOf(String.valueOf(s.charAt(i)));
                temp2.delete(0, dupIndex + 1);
                temp2.append(s.charAt(i));
            }
            if (temp.length() < temp2.length()) {
                temp = new StringBuilder(temp2);
                //temp = temp2;
            }
        }
        return temp.length();
    }
}