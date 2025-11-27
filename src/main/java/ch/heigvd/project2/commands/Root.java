package ch.heigvd.project2.commands;

import picocli.CommandLine;


/**
 * Picocli command line argument configuration
 */
@CommandLine.Command(
        description = "DAI project02",
        version = "1.0.0",
        subcommands = {
            Server.class,
            Client.class
        },
        scope = CommandLine.ScopeType.INHERIT,
        mixinStandardHelpOptions = true)
public class Root implements Runnable{
    @CommandLine.Option(
        names = {"--port", "-p"},
        description = "Port used during the communication.",
        required = false,
        defaultValue = "7580"
    )
    protected  String  port;

    @CommandLine.Option(
        names = {"--host"},
        description = "IP of the server that you want to connect to",
        required = false,
        defaultValue = "localhost"
    )
    protected String host;

    public void run() {
        System.out.println(port);
    }

    protected int getPort(){
        return Integer.parseInt(port);
    }
    protected String getHost(){
        return host;
    }
}