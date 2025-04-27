import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/

class Hand {
    private ArrayList<Card> hand;

    public Hand() {
        this.hand = new ArrayList<Card>();
    }

    public void addCard(Card card) {
        this.hand.add(card);
    }

    public int size() {
        return this.hand.size();
    }

    public Card getCard(int index) {
        return this.hand.get(index);
    }
}

class Card {
    int cardNumber;
    int instanceId;
    int location;
    int cardType;
    int cost;
    int attack;
    int defense;
    String abilities;
    int myHealthChange;
    int opponentHealthChange;
    int cardDraw;


    public Card(int cardNumber, int instanceId, int cardType, int location, int cost, int attack, int defense, String abilities, int myHealthChange, int opponentHealthChange, int cardDraw) {
        this.cardNumber = cardNumber;
        this.instanceId = instanceId;
        this.location = location;
        this.cardType = cardType;
        this.cost = cost;
        this.attack = attack;
        this.defense = defense;
        this.abilities = abilities;
        this.myHealthChange = myHealthChange;
        this.opponentHealthChange = opponentHealthChange;
        this.cardDraw = cardDraw;

    }
}

class Player {

    static class Board {
        static ArrayList<Card> playerBoard = new ArrayList<Card>();
        static ArrayList<Card>  opponentBoard = new ArrayList<Card>();

        public static void resetBoard() {
            playerBoard = new ArrayList<Card>();
            opponentBoard = new ArrayList<Card>();
        }

        public static void addPlayerCard(Card card) {
            playerBoard.add(card);
        }

        public static void addOpponentCard(Card card) {
            opponentBoard.add(card);
        }

        public static Card getPlayerBoardCard(int index) {
            return playerBoard.get(index);
        }

        public static Card getOpponentBoardCard(int index) {
            return opponentBoard.get(index);
        }

        public static int opponentGuardIndex(){
            int guardIndex = -1;
            for (int i = 0; i < opponentBoard.size(); i++) {
                if (opponentBoard.get(i).abilities.contains("G")) {
                    guardIndex = i;
                }
            }

            return guardIndex;
        }
    }

    public static void main(String args[]) {
        Boolean isDraftTurn = true;
        Scanner in = new Scanner(System.in);
        int turnNum = 0;

        // game loop
        while (true) {
            turnNum++;
            Hand playerHand = new Hand();
            Boolean actionPerformed = false;
            Board.resetBoard();
            if (turnNum == 31) {
                isDraftTurn = false;
            }

            int playerHealth = in.nextInt();
            int playerMana = in.nextInt();
            int playerDeck = in.nextInt();
            int playerRune = in.nextInt();
            int playerDraw = in.nextInt();

            int opponentHealth = in.nextInt();
            int opponentMana = in.nextInt();
            int opponentDeck = in.nextInt();
            int opponentRune = in.nextInt();
            int opponentDraw = in.nextInt();

            int opponentHand = in.nextInt();
            int opponentActions = in.nextInt();
            if (in.hasNextLine()) {
                in.nextLine();
            }
            for (int i = 0; i < opponentActions; i++) {
                String cardNumberAndAction = in.nextLine();
            }
            int cardCount = in.nextInt();

            for (int i = 0; i < cardCount; i++) {
                int cardNumber = in.nextInt();
                int instanceId = in.nextInt();
                int location = in.nextInt();
                int cardType = in.nextInt();
                int cost = in.nextInt();
                int attack = in.nextInt();
                int defense = in.nextInt();
                String abilities = in.next();
                int myHealthChange = in.nextInt();
                int opponentHealthChange = in.nextInt();
                int cardDraw = in.nextInt();

                Card card = new Card(cardNumber, instanceId, cardType, location, cost, attack, defense, abilities, myHealthChange, opponentHealthChange, cardDraw);

                if (!isDraftTurn) {
                    switch (location) {
                        case 0:
                            playerHand.addCard(card);
                        break;

                        case 1:
                            Board.addPlayerCard(card);
                        break;

                        case -1:
                            Board.addOpponentCard(card);
                        break;
                    }
                }
            }

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            String output = "";
            if (isDraftTurn) {
                output += "PASS";
            } else {

                // ATTACKING STRATEGIES
                
                // By default attack face
                String targetId = "-1";

                // Attack guards first strategy
                int opponentGuardIndex = Board.opponentGuardIndex();
                if (opponentGuardIndex != -1) {
                    targetId = Integer.toString(Board.getOpponentBoardCard(opponentGuardIndex).instanceId);
                }

                for (int i = 0; i < Board.playerBoard.size(); i++) {
                    Card card = Board.getPlayerBoardCard(i);
                    actionPerformed = true;
                    output += "ATTACK " + card.instanceId + " " + targetId + ";";

                }

                // SUMMONING STRATEGIES

                if (Board.opponentBoard.size() != 6) {
                    for (int i = 0; i < playerHand.size(); i++) {
                        Card card = playerHand.getCard(i);

                        if (card.cost <= playerMana) {
                            playerMana += -card.cost;
                            actionPerformed = true;
                            output += "SUMMON " + card.instanceId + ";";
                        }
                    }
                }

                if (!actionPerformed) {
                    output += "PASS";
                }
            }

            System.out.println(output);
        }
    }
}