public class StringPractice {
    public static void main(String[] args) {
        // 참조형 비교
        String A = new String("Hello");
        String B = "Hello";
        if (A == B) {
            System.out.println("같은 주소");
        } else {
            System.out.println("다른 주소");      // 출력됨
        }
        System.out.println(A.equals(B));         // true

        // 문자열 병합 (+)
        String str1 = "Hello";
        String str2 = "World";
        String result = str1 + " " + str2;
        System.out.println(result);              // Hello World

        // StringBuilder
        StringBuilder sb = new StringBuilder();
        sb.append("Hello");
        sb.append("World");
        System.out.println(sb.toString());       // HelloWorld

        // 문자열 메서드
        String name = "김자바";
        String subName = name.substring(0, 2);
        System.out.println(subName);             // 김자

        String lower = "abc";
        String upper = lower.toUpperCase();
        System.out.println(upper);               // ABC

        String s1 = "abc";
        String s2 = "ABC";
        System.out.println(s1.equalsIgnoreCase(s2)); // true

        // 공백 제거
        String original = "  Hello World  ";
        System.out.println(original.trim());          // 양쪽 공백 제거
        System.out.println(original.replace(" ", "")); // 모든 공백 제거
    }
}