package com.ll;

import java.util.Scanner;

public class App {
    private Scanner scanner;

    private TodoController todoController;

    private SystemController systemController;


    public App() {
        scanner = new Scanner(System.in);
        systemController = new SystemController();
        todoController = new TodoController();
    }


    public void run() {
            while (true) {
                System.out.print("명령) ");
                String cmd = scanner.nextLine().trim();

                if (cmd.equals("exit")) {
                    systemController.exit();
                    break;
                }
                else if (cmd.equals("add")) {
                    todoController.add();
                } else if (cmd.equals("list")) {
                    todoController.list();
                } else if (cmd.equals("del")) {
                    todoController.del();
                } else if (cmd.equals("modify")) {
                    todoController.modify();
                }
            }

        scanner.close();
    }

}