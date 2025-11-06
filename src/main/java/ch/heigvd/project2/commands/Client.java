package ch.heigvd.project2.commands;

import picocli.CommandLine;

@CommandLine.Command(name = "Client", description = "Starts server client application.")
public class Client implements Runnable {

    @CommandLine.ParentCommand protected Root parent;

    public void run(){
        System.out.println("Client started on port: " + parent.getPort());
    }
}
