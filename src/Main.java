import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

/*
 I left some comments that I hope you find useful.
 Please note that none of this is intended to critique the code.
 I'm trying to highlight the best practices and give you pointers
 to make the code nicely engineered. Nicely engineered code is sometimes
 a waste of time but sometimes pays off if e.g. you wanted to learn
 some other rendering library and then have this same game run on
 a larger grid.
*/

public class Main {
    public static void main(String[] args) throws Exception {
        Field game = new Field();
         /*

        |-----------------------------------------------------|
        |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |
        |-----------------------------------------------------|
        |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |
        |-----------------------------------------------------|
        |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |
        |-----------------------------------------------------|
        |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |
        |-----------------------------------------------------|
        |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |
        |-----------------------------------------------------|
        |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |
        |-----------------------------------------------------|
        |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |
        |-----------------------------------------------------|
        |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |
        |-----------------------------------------------------|
        |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |  ?  |
        |-----------------------------------------------------|


         */

        Terminal terminal = new DefaultTerminalFactory().createTerminal();

        Screen screen = new TerminalScreen(terminal);
        screen.doResizeIfNecessary();

        TextGraphics textGraphics = screen.newTextGraphics();

        screen.startScreen();

        // Ideally, the game should be written in a way that you parameterize everything
        // using those two numbers, and then the UI changes dynamically.
        int totalHeight = 21;
        int totalWidth = 55;

        System.out.println(screen.getTerminalSize());

        int startingRow = (screen.getTerminalSize().getRows() - totalHeight) / 2;
        int startingCol = (screen.getTerminalSize().getColumns() - totalWidth) / 2;

        TerminalPosition headerPosition = new TerminalPosition(startingCol, startingRow);
        /*
       In software engineering, those random numbers like +1 or +19 below are referred to as 'magic numbers'.
       In general those should be avoided unless it is obvious what they represent.
       For instance, the +19 below could be redefined as `totalHeight - 2` as I assume the footer position depends
       on the height of the screen. Note that even if we do this, I'm not sure where that 2 came from so it is a bit
       tricky to get rid of all magic numbers.

       You can read more about magic numbers here: https://softwareengineering.stackexchange.com/questions/411206/why-are-magic-numbers-bad-practice
       */
        TerminalPosition startPosition = new TerminalPosition(headerPosition.getColumn(), headerPosition.getRow() + 1);
        TerminalPosition footPosition = new TerminalPosition(startPosition.getColumn(), startPosition.getRow() + 19);
        String counter = "                      Flags set: 0                     ";
        // How would you parameterize those strings so that they continue to work if the number of columns changes?
        String divider = "|-----------------------------------------------------|";
        String cellColumn = "|     |     |     |     |     |     |     |     |     |";
        // Instead of padding the text by hand, you could calculate the centering margin width as
        // (total width - text width) / 2
        // You can then create a string with n spaces inside of it using
        // `" ".repeat(n)`
        String won = "                        You won!                       ";
        String lost = "                       Game over!                      ";

        // Same here you can calculate the two centering margins and padding in between the explanation prompts
        String footer = "           f - flag     s - show     q - quit          ";

        textGraphics.putString(headerPosition, counter);
        textGraphics.putString(startPosition, divider);
        screen.setCursorPosition(new TerminalPosition(startPosition.getColumn() + 3, startPosition.getRow() + 1));

        // I would put a note above this to indicate that this actually draws the divider lines and cell columns as
        // at a first glance I didn't know what we were doing here.
        for (int i = 0; i < 9; i++) {
            textGraphics.putString(startPosition.getColumn(), (startPosition.getRow() + 1) + 2 * i, cellColumn);
            textGraphics.putString(startPosition.getColumn(), (startPosition.getRow() + 2) + 2 * i, divider);
        }

        textGraphics.putString(footPosition, footer);

        game.printField(screen, textGraphics, startPosition);
        screen.refresh();
        boolean keepGoing = true;
        ArrayList<TerminalPosition> flags = new ArrayList<>();

        while (keepGoing) {

            KeyStroke keyPressed = terminal.pollInput();
            if (keyPressed != null) {
                if (keyPressed.getKeyType() == KeyType.Character) {
                    char character = keyPressed.getCharacter();
                    TerminalPosition currentPosition = screen.getCursorPosition();
                    int[] cellIndex = getCellIndex(currentPosition, startPosition);
                    if (character == 'q') { // q is for quit
                        keepGoing = false;
                    } else if (character == 's') { // s is for show (uncover)
                        if (game.getCell(cellIndex[1], cellIndex[0]).getValue() == 0) {
                            game.ifEmpty(game.getCell(cellIndex[1], cellIndex[0]));
                        } else if (game.getCell(cellIndex[1], cellIndex[0]).getValue() == -1) {
                            game.getCell(cellIndex[1], cellIndex[0]).uncover();
                            game.printField(screen, textGraphics, startPosition);
                            textGraphics.putString(headerPosition, lost);
                            screen.refresh();
                            TimeUnit.SECONDS.sleep(5);
                            break;
                        }
                        game.getCell(cellIndex[1], cellIndex[0]).uncover();
                    } else if (character == 'f') { // f is for flag
                        if (flags.contains(currentPosition)) {
                            flags.remove(currentPosition);
                            game.getCell(cellIndex[1], cellIndex[0]).flag();
                            textGraphics.putString(headerPosition.getColumn() + 33, headerPosition.getRow(), flags.size() + " ");
                        } else {
                            if (flags.size() <= 9) {
                                flags.add(currentPosition);
                                game.getCell(cellIndex[1], cellIndex[0]).flag();
                                textGraphics.putString(headerPosition.getColumn() + 33, headerPosition.getRow(), flags.size() + " ");
                            }
                        }


                    }

                    game.printField(screen, textGraphics, startPosition);
                    if (game.getWon()) {
                        textGraphics.putString(headerPosition, won);
                        screen.refresh();
                        TimeUnit.SECONDS.sleep(5);
                        keepGoing = false;
                    }
                }

                TerminalPosition position = screen.getCursorPosition();
                //moving only on spots with numbers
                switch (keyPressed.getKeyType()) {
                    case ArrowDown:
                        if (position.getRow() != startPosition.getRow() + 17) {
                            screen.setCursorPosition(new TerminalPosition(position.getColumn(), position.getRow() + 2));
                        }
                        break;
                    case ArrowUp:
                        if (position.getRow() != startPosition.getRow() + 1) {
                            screen.setCursorPosition(new TerminalPosition(position.getColumn(), position.getRow() - 2));
                        }
                        break;
                    case ArrowRight:
                        if (position.getColumn() != startPosition.getColumn() + 51) {
                            screen.setCursorPosition(new TerminalPosition(position.getColumn() + 6, position.getRow()));
                        }
                        break;
                    case ArrowLeft:
                        if (position.getColumn() != startPosition.getColumn() + 3) {
                            screen.setCursorPosition(new TerminalPosition(position.getColumn() - 6, position.getRow()));
                        }
                        break;
                }
                screen.refresh();
            }
        }

        screen.stopScreen();
    }

    //gives indices of a cell in the field

    /**
     * If you want to be like a pro, you need to write your function docstrings using this /**
     * syntax. This makes them green in your IDE and then if you hit CTRL + K on the name of the
     * function, you get a hover pop-up with the documentation. That way, you can see the explanation
     * at all places in the code where the function is used. If you hover with your mouse over the
     * name of the function, it will show you custom docstring as well.
     */
    public static int[] getCellIndex(TerminalPosition cellPosition, TerminalPosition startPosition) {
        int row = cellPosition.getRow();
        int column = cellPosition.getColumn();

        // Here I would make it clear that the reason we are doing this +3 / 6 is that the text cells are
        // 3 characters wide. Or I would try to make the calculation more generic.

        int x = (column - (startPosition.getColumn() + 3)) / 6;
        int y = (row - (startPosition.getRow() + 1)) / 2;

        return new int[]{x, y};
    }

    public static class Cell {
        boolean uncovered;
        boolean flagged;
        int value;
        int x;
        int y;

        private Cell(int x, int y) {
            this.value = 0;
            this.uncovered = false;
            this.flagged = false;
            this.x = x;
            this.y = y;
        }

        // As above, try playing with proper java docstrings
        //uncover a cell
        public void uncover() {
            this.uncovered = true;
        }

        public void flag() {
            this.flagged = !this.flagged;
        }

        private int getValue() {
            return this.value;
        }
    }


    public static class Field {
        private Cell[][] field;
        boolean won;

        //set up the field
        private Field() {
            // I would put this 9 into some static variable like
            // private static final int GRID_SIZE = 9;
            // This could serve as a stepping stone towards making your code
            // fully dynamic with respect to the grid size.
            field = new Cell[9][9];
            for (int x = 0; x < 9; x++) {
                for (int y = 0; y < 9; y++) {
                    field[x][y] = new Cell(x, y);
                }
            }

            //generate mines and set the numbers around them
            for (int i = 0; i < 10; i++) {
                try {
                    int x = random();
                    int y = random();

                    // Does -1 represent a bomb?
                    //generate
                    while (field[x][y].value == -1) {
                        x = random();
                        y = random();
                    }

                    field[x][y].value = -1;
                    //set numbers
                    for (int j = -1; j < 2; j++) {
                        for (int k = -1; k < 2; k++) {
                            if (j == 0 && k == 0) {
                                continue;
                            }

                            if (0 <= x + j && x + j < 9 && 0 <= y + k && y + k < 9 && field[x + j][y + k].value != -1) {
                                field[x + j][y + k].value += 1;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }

            }
        }

        public void printField(Screen screen, TextGraphics textGraphics, TerminalPosition startPosition) throws IOException {
            // For comments in general people prefer to leave a space after the second /
            //variables for counting flags and uncovered cells to tell if the game is won
            int flagged = 0;
            int uncovered = 0;

            for (int y = 0; y < 9; y++) {
                for (int x = 0; x < 9; x++) {
                    TerminalPosition currentPosition = new TerminalPosition((startPosition.getColumn() + 3) + x * 6, (startPosition.getRow() + 1) + y * 2);
                    if (field[y][x].uncovered) {
                        uncovered++;

                        if (field[y][x].value > 0) {
                            switch (field[y][x].value) {
                                //different colour for each number
                                case 1:
                                    textGraphics.setForegroundColor(TextColor.ANSI.GREEN);
                                    break;
                                case 2:
                                    textGraphics.setForegroundColor(TextColor.ANSI.BLUE);
                                    break;
                                case 3:
                                    textGraphics.setForegroundColor(TextColor.ANSI.YELLOW);
                                    break;
                                case 4:
                                    textGraphics.setForegroundColor(TextColor.ANSI.RED);
                                    break;
                            }

                            /*
                            This is a bit of a 10x java engineer tip here so might not
                            feel useful at this point, but when doing this switching that
                            results in the same action being taken but with different params
                            (e.g. only the color above is different in each branch, I tend
                            to extract the switch into something that gives me the color
                            and then call the actual function once with that color.
                            this keeps the code more compact, and you don't need to read
                            all of those repeated function calls. Something like the below:

                            TextColor color = switch (field[y][x].value) {
                                case 1 -> TextColor.ANSI.GREEN;
                                case 2 -> TextColor.ANSI.BLUE;
                                case 3 -> TextColor.ANSI.YELLOW;
                                case 4 -> TextColor.ANSI.RED;
                                default -> TextColor.ANSI.WHITE;
                            };
                            textGraphics.setForegroundColor(color);

                            Also note that the arrows above are absolutely sexy if you
                            enable ligatures in your editor (try doing ctrl+shift+A) and
                            searching for 'ligatures'.

                            Another note is that this syntax above is called 'enhanced switch'
                            or 'switch expression', don't remember. The point is that this
                            should be available in java 17 onwards, so you should be good
                            for your uni using 21.
                             */
                            textGraphics.putString(currentPosition, "" + field[y][x].value);
                            textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
                        } else if (field[y][x].value == -1) {
                            // This is probably not needed but instead of putting here the logic
                            // that the bomb needs to be rendered with *,  you could have an enum
                            // that captures this like so:

//                            private enum CellPosition {
//                                EMPTY(" "),
//                                BOMB("*") ;
//
//                                private final String representation;
//
//                                CellPosition(String representation) {
//                                    this.representation = representation;
//                                }
//                            }

                            // And you would then do something like BOMB.representation;
                            // This removes the need of having those random strings that
                            // contain actual meaning for your game.

                            textGraphics.putString(currentPosition, "*");
                        } else {
                            textGraphics.putString(currentPosition, " ");
                        }
                    } else {
                        if (field[y][x].flagged) {
                            flagged++;
                            textGraphics.setForegroundColor(TextColor.ANSI.MAGENTA);
                            // As above for bomb
                            textGraphics.putString(currentPosition, "F");
                            textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
                        } else {
                            textGraphics.putString(currentPosition, "?");
                        }
                    }
                }
            }

            //check if won
            // You could explain why uncovered needs to be 71, it is fairly simple
            // as it is 9*9 - 10 bombs, but it is good to actually write down this
            // logic in your code instead of having those random numbers everywhere
            if (flagged == 10 && uncovered == 71) {
                won = true;
            }

            screen.refresh();
        }

        public Cell getCell(int x, int y) {
            return field[x][y];
        }

        public boolean getWon() {
            return won;
        }

        //get a true random number for the mines
        private static int random() throws Exception {
            URI uri = new URI("https://www.random.org/integers/?num=1&min=0&max=8&col=1&base=10&format=plain&rnd=new");
            // Wow, this is super cool. I didn't realise it was possible to simply call a URL with a get request as if you were
            // opening a file with an output stream. This is really interesting as you are using a high level API that
            // sends this HTTP get request under the hood but then you are still forced to use this arguably low level
            // API of wrapping an input stream reader with a buffered reader.
            //
            // Btw, do you know why it is called a buffered reader? You might want to read this: https://docs.oracle.com/javase/8/docs/api/java/io/BufferedReader.html#:~:text=Class%20BufferedReader&text=Reads%20text%20from%20a%20character,large%20enough%20for%20most%20purposes.
            URL url = uri.toURL(); // Safe and non-deprecated way
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine = in.readLine();
            int randomInt = Integer.parseInt(inputLine);
            in.close();

            return randomInt;
        }

        //if empty cell is pressed uncover all the cells around it
        private void ifEmpty(Cell cell) {
            cell.uncover();
            int x = cell.x;
            int y = cell.y;

            if (field[x][y].value == 0) {
                // This is the second time you have this exact same for loops in the code to get neighbours of a given cell.
                // It is fine if you are only doing it twice, but if you were to do something like this many times, it is
                // useful to write an abstraction that given a point gives you coordinates of all of its neighbours (without itself
                // which is equivalent to this j,k == 0 clause that you have below). You probably don't need this
                // at this point, but it is really useful for things like advent of code where 70% of
                // problems are some sort of grid-based game-like simulations.
                for (int j = -1; j < 2; j++) {
                    for (int k = -1; k < 2; k++) {
                        if (j == 0 && k == 0) {
                            continue;
                        }

                        if (0 <= x + j && x + j < 9 && 0 <= y + k && y + k < 9 && !field[x + j][y + k].uncovered) {
                            ifEmpty(field[x + j][y + k]);
                        }
                    }
                }
            }
        }
    }

}
