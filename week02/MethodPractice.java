import java.util.ArrayList;
import java.util.HashMap;

public class MethodPractice {

    // 입력 O, 출력 O
    static int modOperation(int A, int B) {
        int result = A % B;
        return result;
    }

    // 입력 O, 출력 X
    static void printNumber(int A) {
        System.out.println(A);
    }

    // 입력 X, 출력 O
    static String greeting() {
        return "Hello";
    }

    // 입력 X, 출력 X
    static void greet() {
        System.out.println("Hello");
    }

    // ArrayList를 매개변수로
    static void printListElements(ArrayList<Integer> list) {
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
    }

    public static void main(String[] args) {
        int modResult = modOperation(3, 2);
        System.out.println(modResult);   // 1

        printNumber(10);
        System.out.println(greeting());  // Hello
        greet();                          // Hello

        // ArrayList
        ArrayList<Integer> myList = new ArrayList<>();
        myList.add(10);
        myList.add(100);
        printListElements(myList);       // 10, 100

        // HashMap
        HashMap<String, Object> map = new HashMap<>();
        map.put("age", 30);
        map.put("MBTI", "ENFP");
        System.out.println(map.get("age"));   // 30
        System.out.println(map.get("MBTI"));  // ENFP
    }
}