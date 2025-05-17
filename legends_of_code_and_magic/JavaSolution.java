import java.util.*;
import java.io.*;
import java.math.*;
import java.util.stream.Collectors;

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

    public Map<Integer, Long> getCardCostDistribution() {
        return hand.stream()
            .collect(Collectors.groupingBy(
                c -> c.cost,
                Collectors.counting()
            ));
    }

    @Override
    public String toString() {
        return hand.stream()
            .map(Object::toString)
            .collect(Collectors.joining(", ", "[", "]"));
    }
}

enum CardType {
    CREATURE,
    GREEN_ITEM,
    RED_ITEM,
    BLUE_ITEM
}

enum CardAbility {
    BREAKTHROUGH('B'),
    CHARGE('C'),
    GUARD('G'),
    DRAIN('D'),
    LETHAL('L'),
    WARD('W');

    private final char code;

    CardAbility(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }
}

class Card {
    int cardNumber;
    int instanceId;
    int location;
    CardType cardType;
    int cost;
    int attack;
    int defense;
    String abilities;
    int myHealthChange;
    int opponentHealthChange;
    int cardDraw;

    public Card(int cardNumber, int instanceId, CardType cardType, int location, int cost, int attack, int defense,
            String abilities, int myHealthChange, int opponentHealthChange, int cardDraw) {
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

    @Override
    public String toString() {
        String myStr = "cardNumber %d, instanceId %d, location %d, cardType %s, cost %d, attack %d, defense %d, abilities %s, myHealthChange %d, opponentHealthChange %d, cardDraw %d";
        String result = String.format(myStr, cardNumber, instanceId, location, cardType.toString(), cost, attack,
                defense,
                abilities, myHealthChange, opponentHealthChange, cardDraw);
        return result;
    }

    public Boolean hasAbility(CardAbility ability) {
        return this.abilities.indexOf(ability.getCode()) != -1;
    }

    public void takeDamage(Card attacker) {
        if (attacker.hasAbility(CardAbility.LETHAL)) {
            this.defense = 0;
        }
        this.defense += -attacker.attack;
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
    private final int targetId;

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

class PickAction implements Action {
    private final int instanceId;

    public PickAction(int instanceId) {
        this.instanceId = instanceId;
    }

    public String toString() {
        return "PICK " + instanceId;
    }
}

interface Strategy {

    public Action chooseDrafts(GameState gameState);

    public ArrayList<SummonAction> chooseSummons(GameState gameState);

    public ArrayList<AttackAction> chooseAttacks(GameState gameState);

    public double getCardScore(Card card);
}

class BasicStrategy implements Strategy {
    ArrayList<AttackAction> attackActions = new ArrayList<>();
    ArrayList<SummonAction> summonActions = new ArrayList<>();

    public Action chooseDrafts(GameState gameState) {
        // Just pick first one
        return new PassAction();
    }

    public ArrayList<SummonAction> chooseSummons(GameState gameState) {
        // Summon greedily
        if (gameState.playerBoard.size() != 6) {
            for (int i = 0; i < gameState.playerHand.size(); i++) {
                Card card = gameState.playerHand.getCard(i);

                if (card.cost <= gameState.getPlayerMana()) {
                    gameState.setPlayerMana(gameState.getPlayerMana() - card.cost);
                    summonActions.add(new SummonAction(card.instanceId));
                }
            }
        }

        return summonActions;
    }

    public ArrayList<AttackAction> chooseAttacks(GameState gameState) {
        // By default attack face
        // Attack guards first strategy
        int targetId = gameState.opponentGuardCard()
                .map(target -> target.instanceId)
                .orElse(-1);

        for (int i = 0; i < gameState.playerBoard.size(); i++) {
            Card card = gameState.getPlayerBoardCard(i);
            attackActions.add(new AttackAction(card.instanceId, targetId));
        }

        return attackActions;
    }

    public double getCardScore(Card card) {
        return card.attack + card.defense;
    }
}

class BronzeStrategy implements Strategy {
    ArrayList<AttackAction> attackActions = new ArrayList<>();
    ArrayList<SummonAction> summonActions = new ArrayList<>();

    public Action chooseDrafts(GameState gameState) {
        Card bestCard = null;
        int bestCardIndex = 0;
        for (int i = 0; i < gameState.playerHand.size(); i++) {
            Card currCard = gameState.playerHand.getCard(i);
            if (currCard.cardType == CardType.CREATURE) {
                if (bestCard == null
                        || getDraftScore(currCard, gameState) > getDraftScore(bestCard, gameState)) {
                    bestCard = currCard;
                    bestCardIndex = i;
                }
            }
        }

        if (bestCard != null) {
            gameState.playerDeck.addCard(bestCard);
            return new PickAction(bestCardIndex);
        }

        return new PassAction();
    }

    public ArrayList<SummonAction> chooseSummons(GameState gameState) {
        // Summon based on best attack power card
        int mana = gameState.getPlayerMana();
        double[][] dp = new double[gameState.playerHand.size() + 1][gameState.getPlayerMana() + 1]; // Hand size to
                                                                                                    // mana, 2D array -
                                                                                                    // stores optimal
                                                                                                    // score
        // Knapsack alg - item either in or out, take max of either case
        if (gameState.playerBoard.size() == 6) {
            return summonActions;
        }
        for (int i = 1; i <= gameState.playerHand.size(); i++) {
            Card card = gameState.playerHand.getCard(i - 1);
            for (int m = 0; m <= gameState.getPlayerMana(); m++) {
                if (card.cost <= m) {
                    dp[i][m] = Math.max(dp[i - 1][m], dp[i - 1][m - card.cost] + getCardScore(card));
                } else {
                    dp[i][m] = dp[i - 1][m];
                }
            }
        }

        // Adding the optimal actions via backtracking (the cards which contributed to
        // the optimal solution)
        for (int i = gameState.playerHand.size(); i > 0 && mana > 0; i--) {
            if (dp[i][mana] != dp[i - 1][mana]) {
                Card card = gameState.playerHand.getCard(i - 1);
                summonActions.add(new SummonAction(card.instanceId));
                mana -= card.cost;
            }
        }
        Collections.reverse(summonActions);

        return summonActions;
    }

    public ArrayList<AttackAction> chooseAttacks(GameState gameState) {
        // By default attack face
        // Attack guards first strategy

        gameState.playerBoard.sort(Comparator.comparingInt(c -> c.attack)); // Sort playerBoard by ascending attack

        for (int i = 0; i < gameState.playerBoard.size(); i++) {

            int targetId = gameState.opponentGuardCard()
                    .map(target -> target.instanceId)
                    .orElse(-1);

            
            Card card = gameState.getPlayerBoardCard(i);

            if (targetId == -1 && card.hasAbility(CardAbility.LETHAL)) { // if LETHAL then attack opponentBoard
                targetId = gameState.opponentHighestScoreCard(this)
                        .map(target -> target.instanceId)
                        .orElse(-1);
            }

            Card targetCard = gameState.getOpponentBoardCardById(targetId);

            if (targetCard != null && targetCard.defense < 0) {
                attackActions.add(new AttackAction(card.instanceId, -1)); // attack face, current card has died
            }
            attackActions.add(new AttackAction(card.instanceId, targetId));

            if (targetCard != null) {
                targetCard.takeDamage(card); // update card after damaging via attack
            }
        }

        return attackActions;
    }

    public double getCardScore(Card card) {
        if (card.cardType != CardType.CREATURE) {
            return 0;
        }

        double score = card.attack + (0.25 * card.defense);

        if (card.hasAbility(CardAbility.BREAKTHROUGH)) {
            score = score * 1;
        }

        if (card.hasAbility(CardAbility.CHARGE)) {
            score = score * 0.9; // My strategy doesnt support charging yet
        }

        if (card.hasAbility(CardAbility.DRAIN)) {
            score = score * 1.6;
        }

        if (card.hasAbility(CardAbility.GUARD)) {
            score = score * 2;
        }

        if (card.hasAbility(CardAbility.LETHAL)) {
            score = score * 4;
        }

        if (card.hasAbility(CardAbility.WARD)) {
            score = score * 1.2;
        }

        if (card.myHealthChange > 0) {
            score = score * (1 + (0.2 * card.myHealthChange));
        }

        if (card.opponentHealthChange < 0) {
            score = score * (1 + (0.2 * -card.opponentHealthChange));
        }

        score = score * (card.cardDraw + 1);
        return score;
    }

    public double getDraftScore(Card card, GameState gameState) {
        double averageDraftCost = gameState.getAverageDraftCost();
        Map<Integer, Long> costDist = gameState.playerDeck.getCardCostDistribution();
        if (card.cost > 6) { // Too big is a waste to lethal
            return 0;
        }

        
        long costFreq = costDist.getOrDefault(card.cost, 0L);
        if (costFreq >= 6) {
            return -costFreq; // Too many of this cost already
        }
        double idealCost = 0;
        
        // Adjust the score: penalize cards that increase the deviation from the ideal
        double balanceFactor = 1.0 / (1.0 + Math.abs(averageDraftCost + card.cost / 2 - idealCost));  // average after picking
        
        return (getCardScore(card) / card.cost) * balanceFactor;
    }

}

class GameState {
    private int playerHealth;
    private int playerMana;
    private int countPlayerDeck;
    private int playerRune;
    private int countPlayerDrawn;

    private int opponentHealth;
    private int opponentMana;
    private int opponentDeck;
    private int opponentRune;
    private int opponentDraw;
    private int opponentHand;

    ArrayList<String> opponentActions;

    Hand playerHand;
    Hand playerDeck; // This does not get parsed via the game input - have to manually update
    ArrayList<Card> playerBoard;
    ArrayList<Card> opponentBoard;

    public GameState(Hand playerDeck) {
        this.playerHand = new Hand();
        this.playerDeck = playerDeck;
        this.playerBoard = new ArrayList<>();
        this.opponentBoard = new ArrayList<>();
        this.opponentActions = new ArrayList<>();
    }

    public static void readPlayerStats(Scanner in, GameState gameState) {
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
    }

    public static void readOpponentActions(Scanner in, GameState gameState) {
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
    }

    public static void initialiseCards(Scanner in, GameState gameState, Boolean isDraftTurn) {
        int cardCount = in.nextInt();

        for (int i = 0; i < cardCount; i++) {
            int cardNumber = in.nextInt();
            int instanceId = in.nextInt();
            int location = in.nextInt();
            CardType cardType = CardType.values()[in.nextInt()];
            int cost = in.nextInt();
            int attack = in.nextInt();
            int defense = in.nextInt();
            String abilities = in.next();
            int myHealthChange = in.nextInt();
            int opponentHealthChange = in.nextInt();
            int cardDraw = in.nextInt();

            Card card = new Card(cardNumber, instanceId, cardType, location, cost, attack, defense, abilities,
                    myHealthChange, opponentHealthChange, cardDraw);

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
            } else {
                gameState.playerHand.addCard(card);
            }
        }
    }

    public int getPlayerMana() {
        return this.playerMana;
    }

    public void setPlayerMana(int mana) {
        this.playerMana = mana;
    }

    public static GameState readGameState(Scanner in, Boolean isDraftTurn) {
        return readGameState(in, isDraftTurn, null); // first time: no previous state

    }
    public static GameState readGameState(Scanner in, Boolean isDraftTurn, GameState prevGameState) {
        Hand deck = (prevGameState != null) ? prevGameState.playerDeck : new Hand();
        GameState gameState = new GameState(deck); // persist the playerDeck
        readPlayerStats(in, gameState);
        readOpponentActions(in, gameState);
        initialiseCards(in, gameState, isDraftTurn);

        return gameState;
    }

    public Card getPlayerBoardCard(int index) {
        return playerBoard.get(index);
    }

    public Card getOpponentBoardCard(int index) {
        return opponentBoard.get(index);
    }

    public Card getOpponentBoardCardById(int id) {
        return opponentBoard.stream()
        .filter(c -> c.instanceId == id)
        .findFirst()
        .orElse(null); // returns null if not found
    }

    public Optional<Card> opponentGuardCard() {
        return opponentBoard.stream().filter(card -> card.abilities.contains("G")).findFirst();
    }

    public Optional<Card> opponentHighestScoreCard(Strategy strategy) {
        return opponentBoard.stream().max(Comparator.comparingDouble(c -> strategy.getCardScore(c)));
    }

    public double getAverageDraftCost() {
        double cost = 0;
        for (int i = 0; i < this.playerHand.size(); i++) {
            Card card = this.playerHand.getCard(i);
            cost += card.cost;
        }
        return cost / this.playerHand.size();
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
            if (turnNum == 31) {
                isDraftTurn = false;
            }
            
            if (gameState == null) {
                gameState = GameState.readGameState(in, isDraftTurn);
            } else {
                gameState = GameState.readGameState(in, isDraftTurn, gameState);
            }

            ArrayList<Action> actions = new ArrayList<Action>();
            Strategy bronzeStrategy = new BronzeStrategy();

            if (isDraftTurn) { // Draft Strategies
                actions.add(bronzeStrategy.chooseDrafts(gameState));
            } else { // Battle Strategies

                actions.addAll(bronzeStrategy.chooseAttacks(gameState));
                actions.addAll(bronzeStrategy.chooseSummons(gameState));

                if (actions.size() == 0) {
                    actions.add(new PassAction());
                }
            }
            String output = actions.stream()
                    .map(Action::toString)
                    .collect(Collectors.joining(";"));
            System.out.println(output);
        }
    }
}
