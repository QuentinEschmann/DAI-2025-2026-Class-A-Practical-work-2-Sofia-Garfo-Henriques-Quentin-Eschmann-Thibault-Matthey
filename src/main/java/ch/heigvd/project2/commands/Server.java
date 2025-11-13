package ch.heigvd.project2.commands;

import picocli.CommandLine;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


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


    public enum ServerCommand {
        OK,
        INVALID
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
                        String[] clientRequestParts = clientRequest.split(" ", 2);

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

                                System.out.println("[SERVER] Client used "+ clientRequestParts + " command");
                                response = ServerCommand.OK.name();
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

                                System.out.println("[SERVER] Client used "+ clientRequestParts + " command");
                                response = ServerCommand.OK.name();
                            }

                            case LIST -> {
                                System.out.println("[SERVER] Client used "+ clientRequestParts + " command");

                                response = ServerCommand.OK.name();
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

                                System.out.println("[SERVER] Client used "+ clientRequestParts + " command");
                                response = ServerCommand.OK.name();
                            }

                            case MANAGE -> {
                                if(clientRequestParts.length < 3){
                                        System.out.println(
                                            "[Server] " + command + " command received without <item> or <ammount> parameter. Replying with "
                                            + ServerCommand.INVALID
                                            + ".");
                                    response = ServerCommand.INVALID + " Missing <item> or <ammount> parameter. Please try again.";
                                    break;
                                }

                                System.out.println("[SERVER] Client used "+ clientRequestParts + " command");
                                response = ServerCommand.OK.name();
                            }

                            case RESERVE -> {
                                if(clientRequestParts.length < 3){
                                        System.out.println(
                                            "[Server] " + command + " command received without <item> or <ammount> parameter. Replying with "
                                            + ServerCommand.INVALID
                                            + ".");
                                    response = ServerCommand.INVALID + " Missing <item> or <ammount> parameter. Please try again.";
                                    break;
                                }

                                System.out.println("[SERVER] Client used "+ clientRequestParts + " command");
                                response = ServerCommand.OK.name();
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
}

