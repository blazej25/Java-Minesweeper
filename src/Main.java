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

        int totalHeight = 21;
        int totalWidth = 55;

        System.out.println(screen.getTerminalSize());

        int startingRow = (screen.getTerminalSize().getRows() - totalHeight) / 2;
        int startingCol = (screen.getTerminalSize().getColumns() - totalWidth) / 2;

        TerminalPosition headerPosition = new TerminalPosition(startingCol, startingRow);
        TerminalPosition startPosition = new TerminalPosition(headerPosition.getColumn(), headerPosition.getRow() + 1);
        TerminalPosition footPosition = new TerminalPosition(startPosition.getColumn(), startPosition.getRow() + 19);
        String counter = "                      Flags set: 0                     ";
        String divider = "|-----------------------------------------------------|";
        String cellColumn = "|     |     |     |     |     |     |     |     |     |";
        String won = "                        You won!                       ";
        String lost = "                       Game over!                      ";
        String footer = "           f - flag     s - show     q - quit          ";

        textGraphics.putString(headerPosition, counter);
        textGraphics.putString(startPosition, divider);
        screen.setCursorPosition(new TerminalPosition(startPosition.getColumn() + 3, startPosition.getRow() + 1));

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
    public static int[] getCellIndex(TerminalPosition cellPosition, TerminalPosition startPosition) {
        int row = cellPosition.getRow();
        int column = cellPosition.getColumn();

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
                            textGraphics.putString(currentPosition, "" + field[y][x].value);
                            textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
                        } else if (field[y][x].value == -1) {
                            textGraphics.putString(currentPosition, "*");
                        } else {
                            textGraphics.putString(currentPosition, " ");
                        }
                    } else {
                        if (field[y][x].flagged) {
                            flagged++;
                            textGraphics.setForegroundColor(TextColor.ANSI.MAGENTA);
                            textGraphics.putString(currentPosition, "F");
                            textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
                        } else {
                            textGraphics.putString(currentPosition, "?");
                        }
                    }
                }
            }

            //check if won
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
