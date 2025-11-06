package ch.heigvd.project2.commands;

import picocli.CommandLine;


@CommandLine.Command(name = "Server", description = "Starts server side application.")
public class Server implements Runnable {

    @CommandLine.ParentCommand protected Root parent;

    public void run(){
        System.out.println("Server started on port: " + parent.getPort());
    }
}
