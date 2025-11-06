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
    @CommandLine.Parameters(index = "0", description = "Port on wich the Server / Client will be run.")
    protected  int  port;

    public void run() {
        System.out.println(port);
    }

    protected int getPort(){
        return port;
    }
}