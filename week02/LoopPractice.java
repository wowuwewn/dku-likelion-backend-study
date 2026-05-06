public class LoopPractice {
    public static void main(String[] args) {
        // for문
        for (int i = 0; i < 10; i++) {
            System.out.print(i);     // 0123456789
        }
        System.out.println();

        // 2씩 증가
        for (int i = 0; i < 10; i += 2) {
            System.out.print(i);     // 02468
        }
        System.out.println();

        // while문
        int B = 10;
        while (B > 0) {
            System.out.println(B);
            B--;
        }

        // 배열 + for문 (MBTI)
        String[] MBTI = {"INTJ", "ENFP", "ISTP", "INFJ"};
        System.out.println(MBTI.length); // 4
        for (int i = 0; i < MBTI.length; i++) {
            System.out.println(MBTI[i]);
        }

        // 배열 값 수정
        int[] prices = new int[]{10000, 9000, 40000, 7000};
        prices[1] = 8000;
        System.out.println(prices[1]);   // 8000
    }
}