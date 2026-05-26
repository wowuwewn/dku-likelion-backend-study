public class SavingsAccount extends BankAccount implements Withdrawable{

    boolean isOverdraft;

    void transfer() {};

    // 외부에서 접근해서 사용할 수 있도록 퍼블릭

    public void withdraw() {
        System.out.println("Withdraw");
    };

}
