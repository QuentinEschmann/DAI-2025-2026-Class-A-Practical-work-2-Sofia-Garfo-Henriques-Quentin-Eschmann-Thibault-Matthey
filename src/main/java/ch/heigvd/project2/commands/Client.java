package ch.heigvd.project2.commands;

import picocli.CommandLine;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

@CommandLine.Command(name = "Client", description = "Starts server client application.")
public class Client implements Runnable {

    @CommandLine.ParentCommand protected Root parent;

    protected String HOST = "localhost";

    private int PORT = 7580;
    public enum ClientCommand {
        ADD,
        REMOVE,
        LIST,
        MODIFY,
        MANAGE,
        RESERVE,
        QUIT,
        HELP
    }
    public static String END_OF_LINE = "\n";


    public enum ServerCommand {
        OK,
        INVALID
    }

    public void run(){
        this.PORT = parent.getPort();
        this.start();
    }

    public void start() {
        System.out.println("[Client] Connecting to " + HOST + ":" + PORT + "...");

        try (Socket socket = new Socket(HOST, PORT);
            Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(reader);
            Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            BufferedWriter out = new BufferedWriter(writer);
            Reader systemInReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            BufferedReader bsir = new BufferedReader(systemInReader)) {
        System.out.println("[Client] Connected to " + HOST + ":" + PORT);
        System.out.println();

        // Display help message
        help();

        // Run REPL until user quits
        while (!socket.isClosed()) {
            // Display prompt
            System.out.print("> ");

            // Read user input
            String userInput = bsir.readLine();

            try {
                // Split user input to parse command (also known as message)
                String[] userInputParts = userInput.split(" ", 4);
                ClientCommand command = ClientCommand.valueOf(userInputParts[0].toUpperCase());

                // Prepare request
                String request = null;

                switch (command) {
                    case ADD -> {
                        String name = userInputParts[1];
                        int ammount = 0;
                        if(userInputParts.length > 2){
                            ammount = Integer.parseInt(userInputParts[2]);
                        }

                        request = ClientCommand.ADD + " " + name + " " + ammount;
                        break;
                    }
                    case REMOVE -> {
                        String name = userInputParts[1];

                        request = ClientCommand.REMOVE + " " + name;
                        break;
                    }
                    case LIST -> {
                        String item = "all";

                        if(userInputParts.length > 1){
                            item = userInputParts[1];
                        }
                        
                        request = ClientCommand.LIST + " " + item;
                        
                        break;
                    }
                    case MODIFY -> {
                        String oldName = userInputParts[1];
                        String newName = userInputParts[2];

                        request = ClientCommand.MODIFY + " " + oldName + " " + newName;
                        break;
                    }
                    case MANAGE -> {
                        String name = userInputParts[1];
                        int quant = Integer.parseInt(userInputParts[2]);

                        request = ClientCommand.MANAGE + " " + name + " " + quant;
                        break;
                    }
                    case RESERVE -> {
                        String name = userInputParts[1];
                        int quant = Integer.parseInt(userInputParts[2]);

                        request = ClientCommand.RESERVE + " " + name + " " + quant;
                        break;
                    }
                    case QUIT -> {
                        socket.close();
                        continue;
                    }
                    case HELP -> help();
                }

                if (request != null) {
                    // Send request to server
                    out.write(request + END_OF_LINE);
                    out.flush();
                }
                } catch (Exception e) {
                    System.out.println("Invalid command. Please try again.");
                    continue;
                }

                // Read response from server and parse it
                String serverResponse = in.readLine();

                // If serverResponse is null, the server has disconnected
                if (serverResponse == null) {
                    socket.close();
                    continue;
                }

                // Split response to parse message (also known as command)
                String[] serverResponseParts = serverResponse.split(" ", 2);

                ServerCommand message = null;
                try {
                    message = ServerCommand.valueOf(serverResponseParts[0]);
                } catch (IllegalArgumentException e) {
                    // Do nothing
                }

                // Handle response from server
                switch (message) {
                    case OK -> {
                        // As we know from the server implementation, the message is always the second part
                        String response = serverResponseParts[0];
                        System.out.println(response);
                        break;
                    }

                    case INVALID -> {
                        if (serverResponseParts.length < 2) {
                        System.out.println("Invalid message. Please try again.");
                        break;
                        }

                        String invalidMessage = serverResponseParts[1];
                        System.out.println(invalidMessage);
                    }
                    case null, default ->
                        System.out.println("Invalid/unknown command sent by server, ignore.");
                    }
                }

                System.out.println("[Client] Closing connection and quitting...");

            } catch (Exception e) {    
                System.out.println("[Client] Exception: " + e);
            }
    }

  private static void help() {
    System.out.println("Usage:");
    System.out.println("  " + ClientCommand.ADD + "<item> - Adds a new item to the inventory.");
    System.out.println("  " + ClientCommand.REMOVE + "<item> - Removes an item from the inventory.");
    System.out.println("  " + ClientCommand.LIST + " - Lists all items in the inventory");
    System.out.println("  " + ClientCommand.MODIFY + "<oldName> <newName> - Changes the name of an item in the inventory.");
    System.out.println("  " + ClientCommand.MANAGE + "<item> <ammount> - changes the ammount of an item in inventory.");
    System.out.println("  " + ClientCommand.RESERVE + "<item> <ammount> - Reserves an item in inventory.");
    System.out.println("  " + ClientCommand.QUIT + " - Close the connection to the server.");
    System.out.println("  " + ClientCommand.HELP + " - Display this help message.");
  }
}
