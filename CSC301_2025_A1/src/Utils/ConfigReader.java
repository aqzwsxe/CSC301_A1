package Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigReader {
    public static int getPort(String configFile, String serviceName) throws IOException {
        String content = Files.readString(Paths.get(configFile));
        // Find the start of the service block
        int serviceIndex = content.indexOf("\""+serviceName+"\"");
        if(serviceIndex==-1){
            throw new RuntimeException("The service is not found");
        }
        // Find the port key inside that block
        //1: string; 2: From index
        int portKeyIndex = content.indexOf("\"port\"", serviceIndex);

        int colonIndex = content.indexOf(":", portKeyIndex);
        int commaIndex = content.indexOf(",",colonIndex);
        if(commaIndex==-1){
            commaIndex = content.indexOf("}",colonIndex);
        }

        String portValue = content.substring(colonIndex+1,commaIndex).trim();
        return Integer.parseInt(portValue);
    }

    static void main() throws IOException {
        System.out.println(getPort("config.json", "UserService"));
    }
}

