public class Variables {
    public static void main(String[] args) {
        // 변수와 자료형
        int number = 1;
        number = 10;                // 재할당
        double pi = 3.14;
        String greeting = "Hello";
        float b = 1.23F;            // F 안 붙이면 에러
        boolean flag = true;        // 1 대입 시 에러

        // 상수
        final double PI = 3.14;
        System.out.println(PI);
        // PI = 3.1592;             // 에러 발생

        // 타입 캐스팅
        int m = 80;
        double n = m;               // 자동 형변환 → 80.0
        System.out.println(n);

        // 강제 형변환 + 오버플로우
        short x = (short) 128;
        System.out.println(x);      // 128
    }
}