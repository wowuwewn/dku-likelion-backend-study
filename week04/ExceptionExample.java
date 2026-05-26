import java.util.ArrayList;

public class ExceptionExample {

    public static void main(String[] args) {

        // 예외(Exceptions)

//        int a = 10;
//        int b = 0;
//        int c = a / b;


        ArrayList arrayList = new ArrayList(3);

        try {

            int a = 10;
            int b = 0;
            int c = a / b;
//            arrayList.get(10);
        } catch (IndexOutOfBoundsException ioe) {
            System.out.println("IndexOutOfBoundsException 발생");
        } catch (IllegalArgumentException iae) {
            System.out.println("IllegalArgumentException 발생");
        } catch (Exception e) {
            System.out.println("Exception 발생");
        } finally {
            System.out.println("finally");
        }

    }
}
