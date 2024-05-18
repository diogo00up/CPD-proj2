import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);  //creates a socket to estblish a conection with the server
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true); //Sets up input and output streams to communicate with the server.
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) { //Handles user input from the console t
            String userInput;
            System.out.println(in.readLine()); // Read the prompt for username
            out.println(stdIn.readLine());     // Send username
            System.out.println(in.readLine()); // Read the prompt for password
            out.println(stdIn.readLine());     // Send password

            // Read the authentication result
            System.out.println(in.readLine());
            boolean assister = false;
            // If authenticated, handle further commands
            while ((userInput = stdIn.readLine()) != null) {
      
                out.println(userInput);

                String answer =  in.readLine();
                System.out.println("Resposta do servidor: " + answer);


                /* 
                if ("Two players authenticated! Joining the game!".equals(answer)) {
                    System.out.println("MANDA ALGO AO SERVIDOR:");
                    continue;
                    
                
                    // Add any additional logic here if needed
                }
                */
                
                
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }

                /* 
                if(!assister){
                    System.out.println("Press any KEY to start the game!");
                    assister=true;
                }
                else{
                    System.out.println("Your answer:");
                }
                */
                System.out.println("Your answer:");
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 
 
    



}
