import exceptions.StorageException;
import services.LibraryService;
import storage.FileStorage;
import ui.ConsoleUI;
import utils.LibraryConstants;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Application entry point. It only wires the layers together and starts the console UI;
 * all logic lives in the models, services, storage and ui packages.
 */
public class Main {

    public static void main(String[] args) {
        // UTF-8 output so the rupee sign prints correctly wherever the terminal supports it.
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);

        FileStorage storage = new FileStorage(Paths.get(LibraryConstants.DATA_DIRECTORY));
        LibraryService library = new LibraryService(storage);

        try {
            library.initialise();   // creates missing data files, then loads saved data
        } catch (StorageException e) {
            // Stop here rather than run with empty data and overwrite the user's files on exit.
            out.println("[FATAL] LibraTrack could not load its data: " + e.getMessage());
            out.println("Please fix or remove the file mentioned above and start the program again.");
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            new ConsoleUI(library, scanner, out).run();
        }
    }
}
