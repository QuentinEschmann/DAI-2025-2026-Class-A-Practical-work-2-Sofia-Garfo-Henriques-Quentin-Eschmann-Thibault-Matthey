package ch.heigvd.project2.commands;

import picocli.CommandLine;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;

/**
 * Backend implementation of the Warehouse Manager
 */
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

    // Make these static so they're shared across all client threads
    protected static ConcurrentHashMap<String,Integer> db = new ConcurrentHashMap<>();
    protected static ConcurrentHashMap<String,Integer> reserved = new ConcurrentHashMap<>();

    public enum ServerCommand {
        OK,
        INVALID,
        PRINT
    }

    /**
     * function that accepts clients in a loop
     */
    public void run(){
        try (ServerSocket serverSocket = new ServerSocket(parent.getPort());
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println("[SERVER] Listening on port " + parent.getPort() );

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("[SERVER] IO exception: " + e);
        }

        System.out.println("Server started on port: " + parent.getPort());
    }

    /**
     * Class to handle clients concurrently
     */
    class ClientHandler implements Runnable {
        private final Socket socket;

        /**
         * constructor for client handler
         * @param socket Socket, socket used for this connection
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * function that handles the client
         */
        @Override
        public void run() {
            try (socket; // Allow try-with-resources to close socket
                 Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(reader);
                 Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                 BufferedWriter out = new BufferedWriter(writer)) {
                
                System.out.println(
                        "[SERVER] New client connected from "
                                + socket.getInetAddress().getHostAddress()
                                + ":"
                                + socket.getPort());

                // Run REPL until client disconnects
                while (!socket.isClosed()) {
                    // Read response from client
                    String clientRequest = in.readLine();

                    // If clientRequest is null, the client has disconnected
                    if (clientRequest == null) {
                        break;
                    }

                    // Split user input to parse command
                    String[] clientRequestParts = clientRequest.split(" ");

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
                            if(clientRequestParts.length < 3){
                                System.out.println(
                                    "[SERVER] " + command + " command received without parameters. Replying with "
                                    + ServerCommand.INVALID);
                                response = ServerCommand.INVALID + " Missing <item> or <amount> parameter. Please try again.";
                                break;
                            }

                            response = add(clientRequestParts[1], Integer.parseInt(clientRequestParts[2]));
                            System.out.println("[SERVER] " + getUID(socket) + " used "+ command + " command");
                            break;
                        }

                        case REMOVE -> {
                            if(clientRequestParts.length < 2){
                                System.out.println(
                                    "[SERVER] " + command + " command received without <item> parameter. Replying with "
                                    + ServerCommand.INVALID);
                                response = ServerCommand.INVALID + " Missing <item> parameter. Please try again.";
                                break;
                            }

                            String item = clientRequestParts[1];
                            response = remove(item);

                            System.out.println("[SERVER] " + getUID(socket) + " used "+ command + " command");
                            break;
                        }

                        case LIST -> {
                            if(clientRequestParts.length < 2){
                                response = ServerCommand.INVALID + " Missing parameter. Please try again.";
                                break;
                            }
                            response = list(clientRequestParts[1]);
                            System.out.println("[SERVER] " + getUID(socket) + " used "+ command + " command");
                            break;
                        }

                        case MODIFY -> {
                            if(clientRequestParts.length < 3){
                                System.out.println(
                                    "[SERVER] " + command + " command received without <oldName> or <newName> parameters. Replying with "
                                    + ServerCommand.INVALID);
                                response = ServerCommand.INVALID + " Missing <oldname> or <newName> parameter. Please try again.";
                                break;
                            }

                            response = modify(clientRequestParts[1], clientRequestParts[2]);
                            System.out.println("[SERVER] " + getUID(socket) + " used "+ command + " command");
                            break;
                        }

                        case MANAGE -> {
                            if(clientRequestParts.length < 3){
                                System.out.println(
                                    "[SERVER] " + command + " command received without <item> or <amount> parameter. Replying with "
                                    + ServerCommand.INVALID);
                                response = ServerCommand.INVALID + " Missing <item> or <amount> parameter. Please try again.";
                                break;
                            }

                            String item = clientRequestParts[1];
                            int amount;
                            try {
                                amount = Integer.parseInt(clientRequestParts[2]);
                            } catch (NumberFormatException e) {
                                response = ServerCommand.INVALID + " <amount> is not a valid integer.";
                                break;
                            }

                            response = manage(item, amount);
                            System.out.println("[SERVER] " + getUID(socket) + " used "+ command + " command");
                            break;
                        }

                        case RESERVE -> {
                            if(clientRequestParts.length < 3){
                                System.out.println(
                                    "[SERVER] " + command + " command received without <item> or <amount> parameter. Replying with "
                                    + ServerCommand.INVALID);
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

                            System.out.println("[SERVER] " + getUID(socket) + " used "+ command + " command");
                            break;
                        } 

                        case null, default -> {
                            System.out.println(
                                    "[SERVER] Unknown command sent by "+ getUID(socket) +", reply with "
                                            + ServerCommand.INVALID);
                            response = ServerCommand.INVALID + " Unknown command. Please try again.";
                        }
                    }

                    // Send response to client
                    out.write(response + END_OF_LINE);
                    out.flush();
                }

                System.out.println("[SERVER] Closing connection " + getUID(socket));
            } catch (IOException e) {
                System.out.println("[SERVER] IO exception with user: "+ getUID(socket) + " " + e);
            }
        }
    }

    /**
     * add a new item to db
     * @param name String, name of the item to add
     * @param amount int, ammount to add
     * @return String, command status
     */
    private String add(String name, int amount) {
        name = name.toUpperCase();
        if(db.containsKey(name)){
            return (ServerCommand.INVALID + " item " + name + " already exists in inventory ") ;
        }

        if(amount < 0){
            return ServerCommand.INVALID + " <amount> must be a positive or null integer.";
        }

        db.put(name, amount);
        return ServerCommand.OK.name();
    }

    private String remove(String name) {
        name = name.toUpperCase();
        if(!db.containsKey(name)){
            return ServerCommand.INVALID + " item " + name + " does not exist in inventory " ;
        }
        db.remove(name);
        return ServerCommand.OK.name();
    }

    /**
     * lists items contained in db
     * @param name String, item to list or "all" to list everything
     * @return String, command status
     */
    private String list(String name){
        name = name.toUpperCase();
        if(db.isEmpty())
            return  ServerCommand.INVALID.name() + " the inventory is empty";

        if(name.equals("ALL")){
            StringBuilder sb = new StringBuilder(" ,Listing:");
            for(Map.Entry<String, Integer> e : db.entrySet() ){
                sb.append(printItem(e.getKey()));
            }
            return ServerCommand.PRINT.name() + sb;
        } else {
            if(!db.containsKey(name)){
                return ServerCommand.INVALID.name() + " item " + name + " does not exist";
            } else {
                return ServerCommand.PRINT.name() +" "+ printItem(name);
            }
        }
    }

    /**
     * modifies the name of an item
     * @param oldName String, old name for item
     * @param newName String, new name for item
     * @return String, command status
     */
    private String modify(String oldName, String newName){
        newName = newName.toUpperCase();
        oldName = oldName.toUpperCase();
        if(!db.containsKey(oldName)) {
            return ServerCommand.INVALID + " the Item " + oldName + " does not exists.";

        } else if(db.containsKey(newName)){
            return ServerCommand.INVALID + " the Item " + newName + " already exist.";

        }

        int amount = db.remove(oldName);
        db.put(newName, amount);

        return ServerCommand.OK.name();
    }

    /**
     * manages the ammount of item in db
     * @param name String, item to manage
     * @param amount int, new ammount of this item
     * @return String, command status
     */
    private String manage(String name, int amount){
        name = name.toUpperCase();
        //check if item exists
        if(!db.containsKey(name))
            return  ServerCommand.INVALID + " item " + name + " does not exist.";

        if(amount < 0){
            return ServerCommand.INVALID.name() + " <amount> must be a positive or null integer.";
        }
        //replaces old value with new one
        db.put(name, amount);
        return ServerCommand.OK.name();
    }

    /**
     * reserves an ammount of item in internal db
     * @param name String, name of the item
     * @param amount int, ammount to reserve
     * @return String, command status
     */
    private String reserve(String name, int amount){
        name = name.toUpperCase();
        //verify if item exists in inventory
        if(!db.containsKey(name)){
            return (ServerCommand.INVALID + " item " + name + " does not exist.");
        }else if(db.get(name) < amount){
            return (ServerCommand.INVALID + " not enough " + name + " in the warehouse.");
        }

        if(amount < 0){
            return ServerCommand.INVALID.name() + " <amount> must be a positive integer.";
        }

        if(amount > db.get(name)){
            return ServerCommand.INVALID + " cannot reserve more item than available";
        }
        //remove from inventory
        manage(name, db.get(name)-amount);

        //add to list of reserved Items
        int existing = reserved.getOrDefault(name, 0);
        reserved.put(name, existing+amount);

        return ServerCommand.OK.name();
    }

    /**
     * concatenates the informations about an item in db
     * @param name String, name of the item
     * @return String, informations stored about the item
     */
    private String printItem(String name){
        name = name.toUpperCase();
        return " ,Item:" + name + ",Available:" + db.get(name)
            + ",Reserved:" + reserved.getOrDefault(name, 0);
    }

    /**
     * defines an uid for a client based on his ip and used port
     * @param s Socket, socket used with this client
     * @return String, 5 char uid
     */
    private String getUID(Socket s){
        try {
            String input = s.getInetAddress().getHostAddress() + s.getPort();
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 5);
        } catch (Exception e) {
            return "idk";
        }
    }


}


