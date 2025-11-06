package ch.heigvd.project2.commands;

import picocli.CommandLine;

@CommandLine.Command(name = "Client", description = "Starts server client application.")
public class Client implements Runnable {

    @CommandLine.ParentCommand protected Root parent;

    @CommandLine.Option(
        names = {"--host"},
        description = "IP of the server that you want to connect to",
        required = true
    )
    protected String host;

    public void run(){
        System.out.println("Client started on port: " + parent.getPort());
    }
}
