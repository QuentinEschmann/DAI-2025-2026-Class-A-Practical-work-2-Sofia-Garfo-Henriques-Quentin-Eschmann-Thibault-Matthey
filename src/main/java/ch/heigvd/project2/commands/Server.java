package ch.heigvd.project2.commands;

import picocli.CommandLine;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;


@CommandLine.Command(name = "Server", description = "Starts server side application.")
public class Server implements Runnable {

    @CommandLine.ParentCommand protected Root parent;

    // Constants for messages
    public enum ClientCommand {
        ADD,
        REMOVE,
        LIST,
        MODIFY,
        MANAGE,
        RESERVE
    }
    public static String END_OF_LINE = "\n";

    protected HashMap<String,Integer> db = new HashMap<>();
    protected HashMap<String,Integer> reserved = new HashMap<>();



    public enum ServerCommand {
        OK,
        INVALID,
        PRINT
    }

    public void run(){
        try (ServerSocket serverSocket = new ServerSocket(parent.getPort());) {
            System.out.println("[Server] Listening on port " + parent.getPort() );

            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept();
                     Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
                     BufferedReader in = new BufferedReader(reader);
                     Writer writer =
                             new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                     BufferedWriter out = new BufferedWriter(writer)) {
                    System.out.println(
                            "[Server] New client connected from "
                                    + socket.getInetAddress().getHostAddress()
                                    + ":"
                                    + socket.getPort());

                    // Run REPL until client disconnects
                    while (!socket.isClosed()) {
                        // Read response from client
                        String clientRequest = in.readLine();

                        // If clientRequest is null, the client has disconnected
                        // The server can close the connection and wait for a new client
                        if (clientRequest == null) {
                            socket.close();
                            continue;
                        }

                        // Split user input to parse command (also known as message)
                        String[] clientRequestParts = clientRequest.split(" ", 4);

                        ClientCommand command = null;
                        try {
                            command = ClientCommand.valueOf(clientRequestParts[0]);
                        } catch (Exception e) {
                            // Do nothing
                        }

                        // Prepare response
                        String response = null;

                        // Handle request from client
                        switch (command) {
                            case ADD -> {
                                if(clientRequestParts.length < 2){
                                        System.out.println(
                                            "[Server] " + command + " command received without <item> parameter. Replying with "
                                            + ServerCommand.INVALID
                                            + ".");
                                    response = ServerCommand.INVALID + " Missing <item> parameter. Please try again.";
                                    break;
                                }

                                response = add(clientRequestParts[1], Integer.parseInt(clientRequestParts[2]));
                                System.out.println("[SERVER] Client used "+ command + " command");
                                break;
                            }

                            case REMOVE -> {
                                if(clientRequestParts.length < 2){
                                        System.out.println(
                                            "[Server] " + command + " command received without <item> parameter. Replying with "
                                            + ServerCommand.INVALID
                                            + ".");
                                    response = ServerCommand.INVALID + " Missing <item> parameter. Please try again.";
                                    break;
                                }

                                String item = clientRequestParts[1];

                                response = remove(item);

                                System.out.println("[SERVER] Client used "+ command + " command");
                                break;
                            }

                            case LIST -> {
                                response =  list(clientRequestParts[1]);

                                System.out.println("[SERVER] Client used "+ command + " command");
                                break;
                            }

                            case MODIFY -> {
                                if(clientRequestParts.length < 3){
                                        System.out.println(
                                            "[Server] " + command + " command received without <oldName> or <newName> parameters. Replying with "
                                            + ServerCommand.INVALID
                                            + ".");
                                    response = ServerCommand.INVALID + " Missing <oldname> or <newName> parameter. Please try again.";
                                    break;
                                }

                                response = modify(clientRequestParts[1], clientRequestParts[2]);
                                System.out.println("[SERVER] Client used "+ command + " command");
                                break;
                            }

                            case MANAGE -> {
                                if(clientRequestParts.length < 3){
                                        System.out.println(
                                            "[Server] " + command + " command received without <item> or <amount> parameter. Replying with "
                                            + ServerCommand.INVALID
                                            + ".");
                                    response = ServerCommand.INVALID + " Missing <item> or <amount> parameter. Please try again.";
                                    break;
                                }

                                String item = clientRequestParts[1];
                                int amount;
                                try {
                                    amount = Integer.parseInt(clientRequestParts[2]);
                                    if (amount < 0) {
                                        response = ServerCommand.INVALID + " <amount> must be a positive integer or zero.";
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    response = ServerCommand.INVALID + " <amount> is not a valid integer.";
                                    break;
                                }

                                response = manage(item, amount);
                                System.out.println("[SERVER] Client used "+ command + " command");
                                break;
                            }

                            case RESERVE -> {
                                if(clientRequestParts.length < 3){
                                        System.out.println(
                                            "[Server] " + command + " command received without <item> or <amount> parameter. Replying with "
                                            + ServerCommand.INVALID
                                            + ".");
                                    response = ServerCommand.INVALID + " Missing <item> or <amount> parameter. Please try again.";
                                    break;
                                }

                                String item = clientRequestParts[1];
                                int amount;
                                try {
                                    amount = Integer.parseInt(clientRequestParts[2]);
                                    if (amount <= 0) {
                                        response = ServerCommand.INVALID + " <amount> must be a positive integer.";
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    response = ServerCommand.INVALID + " <amount> is not a valid integer.";
                                    break;
                                }

                                response = reserve(item, amount);

                                System.out.println("[SERVER] Client used "+ command + " command");
                                break;
                            } 

                            case null, default -> {
                                System.out.println(
                                        "[Server] Unknown command sent by client, reply with "
                                                + ServerCommand.INVALID
                                                + ".");
                                response = ServerCommand.INVALID + " Unknown command. Please try again.";
                            }
                        }

                        // Send response to client
                        out.write(response + END_OF_LINE);
                        out.flush();
                    }

                    System.out.println("[Server] Closing connection");
                } catch (IOException e) {
                    System.out.println("[Server] IO exception: " + e);
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] IO exception: " + e);
        }

        System.out.println("Server started on port: " + parent.getPort());

    }

    private String add(String name, int amount) {
        if(db.containsKey(name)){
            return ServerCommand.INVALID + " item " + name + " already exists in inventory " ;
        }
        db.put(name, amount);
        return ServerCommand.OK.name();
    }

    private String remove(String name) {
        db.remove(name);
        return ServerCommand.OK.name();
    }

    private String list(String name){
        if(db.isEmpty())
            return  ServerCommand.INVALID.name() + " the inventory is empty";

        if(name.equals("all")){
            StringBuilder sb = new StringBuilder(" Listing:  ");
            for(Map.Entry<String, Integer> e : db.entrySet() ){
                sb.append(printItem(e.getKey()));
            }
            return ServerCommand.PRINT.name() + sb;
        } else {
            if(!db.containsKey(name)){
                return ServerCommand.INVALID.name() + " item" + name + " does not exist";
            } else {
                return ServerCommand.PRINT.name() + printItem(name);
            }
        }
    }

    private String modify(String oldName, String newName){
        if(db.containsKey(newName)) {
            return ServerCommand.INVALID.name() + "the Item " + newName + " already exist.";
        } else if(!db.containsKey(oldName)){
            return ServerCommand.INVALID.name() + "the Item " + oldName + " does not exists.";
        }

        int amount = db.remove(oldName);
        db.put(newName, amount);

        return ServerCommand.OK.name();
    }

    private String manage(String name, int amount){
        //check if item exists
        if(!db.containsKey(name))
            return  ServerCommand.INVALID.name() + "item " + name + " does not exist.";

        //replaces old value with new one
        db.put(name, amount);
        return ServerCommand.OK.name();
    }

    private String reserve(String name, int amount){
        //verify if item exists in inventory
        if(!db.containsKey(name)){
            return  ServerCommand.INVALID.name() + "item " + name + " does not exist.";
        }
        //remove from inventory
        manage(name, db.get(name)-amount);

        //add to list of reserved Items
        int existing = reserved.getOrDefault(name, 0);
        reserved.put(name, existing+amount);

        return ServerCommand.OK.name();
    }

    private String printItem(String name){
        return "Item:" + name + " Available:" + db.get(name)
            + " Reserved:" + reserved.getOrDefault(name, 0) + "  ";
    }


}


