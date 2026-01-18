package Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class WorkloadParser {

    private static String orderUrl;

    static void main(String[] args) throws FileNotFoundException {
        File file = new File(args[0]);
        Scanner sc = new Scanner(file);

        while (sc.hasNextLine()){
            String line = sc.nextLine();
            String[] parts = line.split(" ");
            String service = parts[0];
            String command = parts[1];

            if(service.equals("USER")){
                //build JSON and send to Order Service
            } else if (service.equals("ORDER")) {
                //
            } else if (service.equals("PRODUCT")) {
                //
            }
        }
    }
}
