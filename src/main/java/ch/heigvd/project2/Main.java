package ch.heigvd.project2;

import ch.heigvd.project2.commands.Root;
import picocli.CommandLine;

/**
 * Entry point of the application
 */
public class Main {
    public static void main (String[] args){
        Root Sup = new Root();

        int exitCode =
                new CommandLine(Sup)
                        .execute(args);


        System.exit(exitCode);

    }
}