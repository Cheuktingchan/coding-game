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

interface Action {
    public String toString();
}

class SummonAction implements Action {
    private final int instanceId;

    public SummonAction(int instanceId) {
        this.instanceId = instanceId;
    }

    public String toString() {
        return String.format("SUMMON %d", instanceId);
    }
}

class AttackAction implements Action {
    private final int attackerId;
    int targetId;

    public AttackAction(int attackerId, int targetId) {
        this.attackerId = attackerId;
        this.targetId = targetId;
    }

    public String toString() {
        return String.format("ATTACK %d %d", this.attackerId, this.targetId);
    }
}

class UseAction implements Action {
    private final int itemId;
    private final int creatureId;

    public UseAction(int itemId, int creatureId) {
        this.itemId = itemId;
        this.creatureId = creatureId;
    }

    public String toString() {
        return String.format("USE %d %d", this.itemId, this.creatureId);
    }
}

class PassAction implements Action {
    public String toString() {
        return "PASS";
    }
}

interface Strategy {

    public ArrayList<Action> chooseDrafts(GameState gameState);
    public ArrayList<SummonAction> chooseSummons(GameState gameState);

    public ArrayList<AttackAction> chooseAttacks(GameState gameState);
}

class BasicStrategy implements Strategy {
    ArrayList<Action> draftActions = new ArrayList<>();
    ArrayList<AttackAction> attackActions = new ArrayList<>();
    ArrayList<SummonAction> summonActions = new ArrayList<>();

    public ArrayList<Action> chooseDrafts(GameState gameState) {
        draftActions.add(new PassAction());
        return draftActions;
    }

    public ArrayList<SummonAction> chooseSummons(GameState gameState) {
        if (Board.opponentBoard.size() != 6) {
            for (int i = 0; i < gameState.playerHand.size(); i++) {
                Card card = gameState.playerHand.getCard(i);

                if (card.cost <= gameState.playerMana) {
                    gameState.playerMana += -card.cost;
                    summonActions.add(new SummonAction(card.instanceId));
                }
            }
        }

        return summonActions;
    }

    public ArrayList<AttackAction> chooseAttacks(GameState gameState) {
        // By default attack face
        int targetId = -1;

        // Attack guards first strategy
        int opponentGuardIndex = Board.opponentGuardIndex();
        if (opponentGuardIndex != -1) {
            targetId = Board.getOpponentBoardCard(opponentGuardIndex).instanceId;
        }

        for (int i = 0; i < Board.playerBoard.size(); i++) {
            Card card = Board.getPlayerBoardCard(i);
            attackActions.add(new AttackAction(card.instanceId, targetId));
        }

        return attackActions;
    }
    
}

class GameState {
    private final int playerHealth;
    private final int playerMana;
    private final int countPlayerDeck;
    private final int playerRune;
    private final int countPlayerDrawn;

    private final int opponentHealth;
    private final int opponentMana;
    private final int opponentDeck;
    private final int opponentRune;
    private final int opponentDraw;
    private final int opponentHand;

    ArrayList<String> opponentActions;

    Hand playerHand;
    ArrayList<Card> playerBoard;
    ArrayList<Card> opponentBoard;

    public GameState() {
        this.playerHand = new Hand();
        this.playerBoard = new ArrayList<>();
        this.opponentBoard = new ArrayList<>();
        this.opponentActions = new ArrayList<>();
    }

    public static GameState readGameState(Scanner in, Boolean isDraftTurn) {
        GameState gameState = new GameState();
    
        gameState.playerHealth = in.nextInt();
        gameState.playerMana = in.nextInt();
        gameState.countPlayerDeck = in.nextInt();
        gameState.playerRune = in.nextInt();
        gameState.countPlayerDrawn = in.nextInt();
    
        gameState.opponentHealth = in.nextInt();
        gameState.opponentMana = in.nextInt();
        gameState.opponentDeck = in.nextInt();
        gameState.opponentRune = in.nextInt();
        gameState.opponentDraw = in.nextInt();
    
        gameState.opponentHand = in.nextInt();
        int numOpponentActions = in.nextInt();
    
        if (in.hasNextLine()) {
            in.nextLine();
        }
    
        for (int i = 0; i < numOpponentActions; i++) {
            if (in.hasNextLine()) {
                gameState.opponentActions.add(in.nextLine());
            }
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
                        gameState.playerHand.addCard(card);
                        break;
                    case 1:
                        gameState.playerBoard.add(card);
                        break;
                    case -1:
                        gameState.opponentBoard.add(card);
                        break;
                }
            }
        }
    
        return gameState;
    }
}

public class Board {
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

class Player {
    private static Boolean isDraftTurn = true;
    private static GameState gameState;

    public static void main(String args[]) {
        int turnNum = 0;

        Scanner in = new Scanner(System.in);

        // game loop
        while (true) {
            turnNum++;
            Board.resetBoard();
            if (turnNum == 31) {
                isDraftTurn = false;
            }

            gameState = GameState.readGameState(in, isDraftTurn);
            
            ArrayList<Action> actions = new ArrayList<Action>();
            BasicStrategy basicStrategy = new BasicStrategy();

            if (isDraftTurn) { // Draft Strategies
                actions.addAll(basicStrategy.chooseDrafts(gameState));
            } else { // Battle Strategies                
                
                actions.addAll(basicStrategy.chooseAttacks(gameState));
                actions.addAll(basicStrategy.chooseSummons(gameState));

                if (actions.size() == 0) {
                    actions.add(new PassAction());
                }
            }

            String output = "";
            
            if (actions.size() == 1) {
                output += actions.get(0).toString();
            } else {
                for (int i = 0; i < actions.size(); i++) {
                    output += actions.get(i).toString();
                    output += ";";
                }
            }

            System.out.println(output);
        }
    }
}